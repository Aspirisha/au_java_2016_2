package au.java.tracker.client;

import au.java.tracker.protocol.*;
import au.java.tracker.protocol.util.IpValidator;
import com.google.common.collect.Range;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.softhouse.jargo.Argument;
import se.softhouse.jargo.CommandLineParser;
import se.softhouse.jargo.ParsedArguments;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.*;

import static se.softhouse.jargo.Arguments.integerArgument;
import static se.softhouse.jargo.Arguments.stringArgument;

/**
 * Created by andy on 11/7/16.
 */
public class TrackerClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(TrackerClient.class);
    private static final Path CONFIG_FILE = Paths.get(".config");
    private static final Path CACHED_IP_FILE = Paths.get(".ips");
    private static final int UPDATE_INTERVAL_SECONDS = 5 * 60;

    private final Thread listeningThread;
    private final Thread updateThread;
    private final ExecutorService uploadExecutorService;
    private final ExecutorService downloadExecutorService;
    private final ClientRequestExecutor requestExecutor = new ClientRequestExecutorImpl();
    private final ClientDescriptor myDescriptor;
    private final IOHandler iohandler = new IOHandlerImpl();
    private String serverIp;
    private String myIp;
    private final Map<Integer, DownloadingFileDescriptor> myFiles;

    private class ClientServerInstance implements Runnable {
        private final Socket clientSocket;

        ClientServerInstance(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {
            TrackerProtocol.PeerServerProtocol p = TrackerProtocol.getPeerServerProtocol();

            try(DataOutputStream dos = new DataOutputStream(clientSocket.getOutputStream());
                DataInputStream dis = new DataInputStream(clientSocket.getInputStream())) {
                p.processCommand(dis, dos, requestExecutor);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    LOGGER.error("", e);
                    iohandler.onCantCloseClientSocket();
                }
            }
        }
    }

    public void stop() {
        iohandler.onFinishingJobs();

        listeningThread.interrupt();
        uploadExecutorService.shutdownNow();
        downloadExecutorService.shutdownNow();
        updateThread.interrupt();
        writeConfig();
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

    private Map<Integer, DownloadingFileDescriptor> readConfig() throws IOException {
        if (!CONFIG_FILE.toFile().exists()) {
            return new HashMap<>();
        }

        try (FileInputStream is =
                     new FileInputStream(CONFIG_FILE.toString());
             ObjectInputStream out = new ObjectInputStream(is)) {
            return (Map<Integer, DownloadingFileDescriptor>) out.readObject();
        } catch(IOException e) {
            LOGGER.error("", e);
            iohandler.onCantCreateConfigFile();
            throw e;
        } catch (ClassNotFoundException e) {
            LOGGER.error("", e);
            iohandler.onCorruptedConfig();
            CONFIG_FILE.toFile().delete();
        }

        return new HashMap<>();
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

    private void writeConfig() {
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

        uploadExecutorService = new ThreadPoolExecutor(DEFAULT_THREADS,
                MAX_THREADS, 1, TimeUnit.MINUTES, new SynchronousQueue<>());
        downloadExecutorService = new ThreadPoolExecutor(DEFAULT_THREADS,
                MAX_THREADS, 1, TimeUnit.MINUTES, new SynchronousQueue<>());

        listeningThread = initListeningThread(port);
        updateThread = initUpdateThread();
    }

    private Thread initListeningThread(int port) {
        return new Thread(() -> {
            LOGGER.info("Starting listening on port " + port);
            try (ServerSocket serverSocket = new ServerSocket(port)) {
                serverSocket.setSoTimeout(10000);
                while (!Thread.interrupted()) {
                    try {
                        uploadExecutorService.execute(new ClientServerInstance(serverSocket.accept()));
                    } catch (RejectedExecutionException rej) {
                        LOGGER.info("Rejected connection with client");
                    } catch (IOException e) {
                        LOGGER.error("", e);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private Thread initUpdateThread() {
        return new Thread(() -> {
            TrackerProtocol.ClientToServerProtocol p = null;
            try {
                p = TrackerProtocol.getClientToServerProtocol(serverIp);
            } catch (Exception e) {
                LOGGER.error("", e);
                return;
            }

            while (true) {
                Set<Integer> fileIdSet = new HashSet<>();
                synchronized (myFiles) {
                    fileIdSet.addAll(myFiles.keySet());
                }

                p.serverRequestUpdate(myDescriptor, fileIdSet);
                try {
                    Thread.sleep(UPDATE_INTERVAL_SECONDS * 1000);
                } catch (InterruptedException e) {
                    return;
                }
            }
        });
    }



    private void run() throws Exception {
        final String UPLOAD_COMMAND = "upload";
        final String GET_COMMAND = "get";
        final String LIST_COMMAND = "list";
        final String EXIT_COMMAND = "exit";
        final String HELP_COMMAND = "help";

        listeningThread.start();
        updateThread.start();

        TrackerProtocol.ClientToServerProtocol p = null;
        try {
            p = TrackerProtocol.getClientToServerProtocol(serverIp);
        } catch (Exception e) {
            iohandler.onCantConnectToServer();
            throw e;
        }

        iohandler.onHelpRequested();
        for (String command = iohandler.readCommand(); command != null;
             command = iohandler.readCommand()) {
            LOGGER.info("New command: " + command);
            String[] s = command.split(" ", 1);

            switch (s[0].trim()) {
                case UPLOAD_COMMAND: {
                    if (s.length == 1) {
                        iohandler.onFileToUploadNotSpecified();
                        break;
                    }
                    Path file = Paths.get(s[1]);

                    if (!file.toFile().exists()) {
                        iohandler.onUnexistentUpload(file.toString());
                        break;
                    }

                    p.serverRequestUpload(s[1], file.toFile().length());
                    break;
                }
                case GET_COMMAND: {
                    if (s.length == 1) {
                        iohandler.onFileToDownloadNotSpecified();
                        break;
                    }

                    Integer fileId;
                    s = s[1].split(" ", 1);
                    try {
                        fileId = Integer.valueOf(s[0]);
                    } catch (NumberFormatException e) {
                        iohandler.onFileIdExpected();
                        break;
                    }

                    au.java.tracker.protocol.FileDescriptor desc = p.describeFile(fileId);
                    if (desc == null) {// file wasn't uploaded
                        iohandler.onFileNotTracked(fileId);
                        break;
                    }

                    synchronized (myFiles) {
                        myFiles.putIfAbsent(fileId, new DownloadingFileDescriptor(desc,
                                new HashSet<>(), s[1]));
                    }
                    DownloadingFileDescriptor fd = myFiles.get(fileId);

                    downloadExecutorService.submit(new FileDownloader(fd, serverIp));

                    break;
                }
                case LIST_COMMAND: {

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
            client =  new TrackerClient(port, ip);
            client.run();
        } catch (Exception ignored) {
            if (client != null) {
                client.stop();
            }
        }
    }

}
