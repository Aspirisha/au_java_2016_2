package au.java.tracker.client;

import au.java.tracker.protocol.ClientDescriptor;
import au.java.tracker.protocol.ClientRequestExecutor;
import au.java.tracker.protocol.FileDescriptor;
import au.java.tracker.protocol.TrackerProtocol;
import au.java.tracker.protocol.util.IpValidator;
import au.java.tracker.protocol.util.TimeoutSocketConnector;
import com.google.common.collect.Range;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.softhouse.jargo.Argument;
import se.softhouse.jargo.CommandLineParser;
import se.softhouse.jargo.ParsedArguments;

import java.io.*;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static se.softhouse.jargo.Arguments.integerArgument;
import static se.softhouse.jargo.Arguments.stringArgument;

/**
 * Created by andy on 11/7/16.
 */
public class TrackerClient implements FileListProvider, ClientRequestExecutor {
    private static final Logger LOGGER = LoggerFactory.getLogger(TrackerClient.class);
    private static final Path CONFIG_FILE = Paths.get(".config");
    private static final Path CACHED_IP_FILE = Paths.get(".ips");

    private final Thread listeningThread;
    private final Thread updateThread;

    private final ExecutorService taskService;
    private final ClientDescriptor myDescriptor;
    private final IOHandler iohandler = new IOHandlerImpl();
    private String serverIp;
    private String myIp;
    private final ConcurrentHashMap<Integer, DownloadingFileDescriptor> myFiles;
    private final onDownloadFinishedListener downloadListener = iohandler::onFileDownloaded;

    public interface onDownloadFinishedListener {
        void onDownloadFinished(DownloadingFileDescriptor fd,
                                FileDownloadResult result);
    }

    public void stop() {
        iohandler.onFinishingJobs();

        listeningThread.interrupt();
        taskService.shutdownNow();
        updateThread.interrupt();
        dumpMyFiles();
        writeIps();
    }

    private void writeIps() {
        try {
            Files.write(CACHED_IP_FILE, Collections.singletonList(serverIp), Charset.defaultCharset(),
                    StandardOpenOption.WRITE, StandardOpenOption.CREATE);
        } catch (IOException e) {
            LOGGER.error("", e);
        }
    }

    private ConcurrentHashMap<Integer, DownloadingFileDescriptor> readConfig() throws IOException {
        if (!CONFIG_FILE.toFile().exists()) {
            return new ConcurrentHashMap<>();
        }

        try (FileInputStream is =
                     new FileInputStream(CONFIG_FILE.toString());
             ObjectInputStream out = new ObjectInputStream(is)) {
            return (ConcurrentHashMap<Integer, DownloadingFileDescriptor>) out.readObject();
        } catch(IOException e) {
            LOGGER.error("", e);
            iohandler.onCantCreateConfigFile();
            throw e;
        } catch (ClassNotFoundException e) {
            LOGGER.error("", e);
            iohandler.onCorruptedConfig();
            CONFIG_FILE.toFile().delete();
        }
        iohandler.onCantConnectToServer();
        return new ConcurrentHashMap<>();
    }

    private void readCachedServerIp() {
        if (!CACHED_IP_FILE.toFile().exists()) {
            return;
        }

        try {
            List<String> ips = Files.readAllLines(CACHED_IP_FILE);
            if (ips.isEmpty())
                return;
            serverIp = ips.get(0);
        } catch (IOException e) {
            LOGGER.error("", e);
        }
    }

    private void dumpMyFiles() {
        try (FileOutputStream os =
                     new FileOutputStream(CONFIG_FILE.toString());
             ObjectOutputStream out = new ObjectOutputStream(os)) {
             out.writeObject(myFiles);
        } catch(IOException e) {
            LOGGER.error("", e);
            iohandler.onCantCreateConfigFile();
        }
    }

    private void getMyIp() throws IOException {
        Socket s = new Socket("192.168.1.1", 80);
        myIp = s.getLocalAddress().getHostAddress();
    }

    private TrackerClient(int port, String _serverIp) throws Exception {
        final int MAX_THREADS = 30;
        final int DEFAULT_THREADS = 1;
        serverIp = _serverIp;

        getMyIp();
        if (serverIp == null) {
            readCachedServerIp();
        }

        if (serverIp == null) {
            iohandler.serverIpIsRequired();
            throw new Exception("Null server ip");
        }


        try {
            myDescriptor = new ClientDescriptor(myIp, port);
        } catch (Exception e) {
            LOGGER.error("", e);
            iohandler.onCantObtainIp();
            throw e;
        }

        myFiles = readConfig();
        if (!IpValidator.validateIp(serverIp)) {
            iohandler.onInvalidServerIp(serverIp);
            throw new Exception("Invalid ip");
        }

        taskService = new ThreadPoolExecutor(DEFAULT_THREADS,
                MAX_THREADS, 1, TimeUnit.MINUTES, new SynchronousQueue<>());

        listeningThread = new Thread(new ConnectionListener(port, this));
        updateThread = new Thread(new Updater(serverIp, myDescriptor, this));

        // this thread dumps files once in a while
        taskService.submit(() -> {
            final int SLEEP_SECONDS = 60;

            while (!Thread.interrupted()) {
                dumpMyFiles();
                try {
                    Thread.sleep(SLEEP_SECONDS * 1000);
                } catch (InterruptedException e) {
                    return;
                }
            }
        });
    }

    private void run() throws Exception {
        final String UPLOAD_COMMAND = "upload";
        final String DOWNLOAD_COMMAND = "download";
        final String LIST_COMMAND = "list";
        final String EXIT_COMMAND = "exit";
        final String HELP_COMMAND = "help";

        listeningThread.start();
        updateThread.start();

        iohandler.onHelpRequested();
        for (String command = iohandler.readCommand(); command != null;
             command = iohandler.readCommand()) {
            LOGGER.info("New command: " + command);
            String[] s = command.split(" ", 2);

            switch (s[0].trim()) {
                case UPLOAD_COMMAND: {
                    uploadCommand(s);
                    break;
                }
                case DOWNLOAD_COMMAND: {
                    getCommand(s);
                    break;
                }
                case LIST_COMMAND: {
                    listCommand();
                    break;
                }
                case HELP_COMMAND: {
                    iohandler.onHelpRequested();
                    break;
                }
                case EXIT_COMMAND: {
                    stop();
                    return;
                }
                case "" : {
                    continue;
                }
                default: {
                    iohandler.onUnknownCommand(s[0]);
                    iohandler.onHelpRequested();
                }
            }
        }
    }

    private void getCommand(String[] commandParts)
            throws IOException {
        if (commandParts.length == 1) {
            iohandler.onFileToDownloadNotSpecified();
            return;
        }

        Integer fileId;
        commandParts = commandParts[1].split(" ", 2);


        try {
            fileId = Integer.valueOf(commandParts[0]);
        } catch (NumberFormatException e) {
            iohandler.onFileIdExpected();
            return;
        }

        if (commandParts.length < 2 && !myFiles.containsKey(fileId)) {
            iohandler.onOutputPathExpected();
            return;
        }

        if (commandParts.length >= 2 && myFiles.containsKey(fileId)) {
            iohandler.fileMovingNotSupported(myFiles.get(fileId));
        }

        if (myFiles.containsKey(fileId)) {
            DownloadingFileDescriptor fd = myFiles.get(fileId);
            if (fd.isCompletelyDownloaded()) {
                iohandler.onFileDownloaded(fd, FileDownloadResult.FILE_IS_DOWNLOADED);
                return;
            }
        }

        au.java.tracker.protocol.FileDescriptor desc = null;
        try (Socket s = TimeoutSocketConnector.tryConnectToServer(
                serverIp, TrackerProtocol.SERVER_PORT)) {
            TrackerProtocol.ClientToServerProtocol p = TrackerProtocol.getClientToServerProtocol(s);

            desc = p.describeFile(fileId);
        } catch (Exception e) {
            LOGGER.error("", e);
            iohandler.onCantConnectToServer();
            return;
        }

        if (desc == null) {// file wasn't uploaded
            iohandler.onFileNotTracked(fileId);
            return;
        }

        myFiles.putIfAbsent(fileId, new DownloadingFileDescriptor(desc,
                commandParts[1]));
        DownloadingFileDescriptor fd = myFiles.get(fileId);

        taskService.submit(new FileDownloader(fd, serverIp, downloadListener));
    }

    private void listCommand() {
        taskService.submit(() -> {
            try (Socket s = TimeoutSocketConnector.tryConnectToServer(
                    serverIp, TrackerProtocol.SERVER_PORT)) {
                TrackerProtocol.ClientToServerProtocol p = TrackerProtocol.getClientToServerProtocol(s);
                List<FileDescriptor> l = p.serverRequestList();
                iohandler.showFileList(l);
            } catch (Exception e) {
                LOGGER.error("", e);
                iohandler.onListFailed();
            }
        });
    }

    private void uploadCommand(String[] commandParts) throws IOException {
        if (commandParts.length == 1) {
            iohandler.onFileToUploadNotSpecified();
            return;
        }
        Path file = Paths.get(commandParts[1]);

        if (!file.toFile().exists()) {
            iohandler.onUnexistentUpload(file.toString());
            return;
        }

        if (!file.toFile().isFile()) {
            iohandler.onCantUploadDirectories();
            return;
        }

        taskService.submit(() -> {
            try (Socket s = TimeoutSocketConnector.tryConnectToServer(
                    serverIp, TrackerProtocol.SERVER_PORT)) {
                TrackerProtocol.ClientToServerProtocol p = TrackerProtocol.getClientToServerProtocol(s);
                int id = p.serverRequestUpload(file.getFileName().toString(), file.toFile().length(), myDescriptor);

                DownloadingFileDescriptor fd = DownloadingFileDescriptor.getForMyFile(id, file.toString());
                myFiles.put(id, fd);
                assert (myFiles.containsKey(id));
                iohandler.onSuccessfulUpload(fd);
            } catch (Exception e) {
                LOGGER.error("", e);
                iohandler.onUploadFailed(file.toString());
            }
        });
    }

    public static void main(String[] args) {
        final Integer MIN_PORT = 1024;

        Argument<Integer> portNumber = integerArgument("-p", "--port")
                .description("Port number to use")
                .metaDescription("<n>")
                .defaultValue(MIN_PORT)
                .limitTo(Range.closed(MIN_PORT, 2 * Short.MAX_VALUE - 1))
                .build();

        Argument<String> serverIpAddress = stringArgument("-s", "--server")
                .description("Server ip")
                .metaDescription("<ip>")
                .defaultValue(null)
                .build();

        ParsedArguments arguments = CommandLineParser.withArguments(
                portNumber, serverIpAddress)
                .programName("tracker-client").parse(args);

        Integer port = Optional.ofNullable(arguments.get(portNumber)).orElse(MIN_PORT);
        String ip = arguments.get(serverIpAddress);

        TrackerClient client = null;
        try {
            client = new TrackerClient(port, ip);
            client.run();
        } catch (Exception ignored) {
            ignored.printStackTrace();
            if (client != null) {
                client.stop();
            }
        }
    }

    @Override
    public Set<Integer> listFiles() {
        Set<Integer> fileIdSet = new HashSet<>();
        fileIdSet.addAll(myFiles.keySet());

        return fileIdSet;
    }

    @Override
    public List<Integer> clientRequestStat(int fileId) {
        if (!myFiles.containsKey(fileId)) {
            return new LinkedList<>();
        }

        return Arrays.stream(myFiles.get(fileId).partsMap).filter(
                e -> e.get() == DownloadingFileDescriptor.PART_TYPE.IS_DOWNLOADED)
                .map(AtomicInteger::get)
                .collect(Collectors.toList());
    }

    @Override
    public byte[] clientRequestGet(int fileId, int partNum) {
        if (myFiles.get(fileId) == null) {
            return new byte[0];
        }

        DownloadingFileDescriptor desc = myFiles.get(fileId);
        if (partNum >= desc.partsNumber || partNum < 0) {
            return new byte[0];
        }

        if (desc.partsMap[partNum].get() != DownloadingFileDescriptor.PART_TYPE.IS_DOWNLOADED) {
            return new byte[0];
        }

        byte[] buf = new byte[desc.getPartSize(partNum)];
        try (RandomAccessFile raf = new RandomAccessFile(desc.outputPath, "r")) {
            raf.readFully(buf, TrackerProtocol.CHUNK_SIZE * partNum, buf.length);
            return buf;
        } catch (IOException e) {
            LOGGER.error("", e);
            return new byte[0];
        }
    }
}
