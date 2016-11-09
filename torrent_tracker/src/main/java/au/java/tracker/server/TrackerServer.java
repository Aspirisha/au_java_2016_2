package au.java.tracker.server;

import au.java.tracker.protocol.ClientDescriptor;
import au.java.tracker.protocol.FileDescriptor;
import au.java.tracker.protocol.ServerRequestExecutor;
import au.java.tracker.protocol.TrackerProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by andy on 11/7/16.
 */
public class TrackerServer implements ServerRequestExecutor {
    private static final Logger LOGGER = LoggerFactory.getLogger(TrackerServer.class);
    private static final String STATE_FILE = ".server_state";

    private final Thread listeningThread;
    private final Thread aliveClientsFilter;
    private final ExecutorService executorService;
    private final IOHandler iohandler = new IOHandlerImpl();

    private ServerState state;

    @Override
    public List<FileDescriptor> serverRequestList() throws IOException {
        List<FileDescriptor> data = new ArrayList<>();
        data.addAll(state.uploadedFiles.values());

        return data;
    }

    @Override
    public int serverRequestUpload(String fileName, long fileSize,
                                   ClientDescriptor uploader) throws IOException {
        int id = state.uploadedFilesNumber.incrementAndGet();

        FileDescriptor fd = new FileDescriptor(id, fileName, fileSize);
        state.uploadedFiles.put(id, fd);
        ConcurrentSkipListSet<ClientDescriptor> holders = new ConcurrentSkipListSet<>();
        holders.add(uploader);

        state.holders.put(id, holders);
        return id;
    }

    @Override
    public Set<ClientDescriptor> serverRequestSources(int fileId) throws IOException {
        return state.holders.get(fileId);
    }

    @Override
    public boolean serverRequestUpdate(ClientDescriptor client, Set<Integer> fileIds) throws IOException {
        state.timestampOfLastClientActivity.put(client, System.currentTimeMillis());
        state.aliveClients.add(client);
        for (Integer id : fileIds) {
            if (!state.holders.containsKey(id))
                continue;
            state.holders.get(id).add(client);
        }

        state.clientsFiles.put(client, fileIds);
        return true;
    }

    @Override
    public FileDescriptor describeFile(int fileId) throws IOException {
        return state.uploadedFiles.get(fileId);
    }

    private static class ServerState implements Serializable {
        AtomicInteger uploadedFilesNumber;
        ConcurrentHashMap<Integer, FileDescriptor> uploadedFiles;

        // mapping from file id to clients that own this file
        transient ConcurrentHashMap<Integer, ConcurrentSkipListSet<ClientDescriptor>> holders;
        transient ConcurrentHashMap<ClientDescriptor, Long> timestampOfLastClientActivity;
        transient ConcurrentSkipListSet<ClientDescriptor> aliveClients;
        // mapping from client to his files
        transient ConcurrentHashMap<ClientDescriptor, Set<Integer>> clientsFiles;

        ServerState() {
            uploadedFilesNumber = new AtomicInteger(0);
            uploadedFiles = new ConcurrentHashMap<>();
            holders = new ConcurrentHashMap<>();
            timestampOfLastClientActivity = new ConcurrentHashMap<>();
            aliveClients = new ConcurrentSkipListSet<>();
            clientsFiles = new ConcurrentHashMap<>();
        }

        private void readObject(ObjectInputStream ois)
                throws ClassNotFoundException, IOException {
            ois.defaultReadObject();

            holders = new ConcurrentHashMap<>();
            timestampOfLastClientActivity = new ConcurrentHashMap<>();
            aliveClients = new ConcurrentSkipListSet<>();
            clientsFiles = new ConcurrentHashMap<>();
        }

        private void makeInactive(ClientDescriptor cd) {
            aliveClients.remove(cd);
            Set<Integer> clientFiles = clientsFiles.get(cd);

            for (Integer fileId : clientFiles) {
                holders.get(fileId).remove(cd);
            }
        }
    }

    private class ClientServerInstance implements Runnable {
        private final Socket clientSocket;

        ClientServerInstance(Socket clientSocket) {
            this.clientSocket = clientSocket;
            LOGGER.info("Client connected: " + clientSocket.getInetAddress());
        }

        @Override
        public void run() {
            TrackerProtocol.ServerProtocol p = TrackerProtocol.getServerProtocol();
            try {
                p.processCommand(clientSocket, TrackerServer.this);
            } catch (Exception e) {
                LOGGER.error("", e);
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

    private void dumpState() {
        try (FileOutputStream fos = new FileOutputStream(STATE_FILE);
                ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(state);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void readState() {
        try (FileInputStream fis = new FileInputStream(STATE_FILE);
             ObjectInputStream ois = new ObjectInputStream(fis)) {
            state = (ServerState) ois.readObject();
            if (state == null) {
                LOGGER.error("Read null state!");
                state = new ServerState();
            }
        } catch (FileNotFoundException e) {
            state = new ServerState();
        } catch (IOException | ClassNotFoundException e) {
            LOGGER.error("", e);
            iohandler.onCouldntReadState();
            state = new ServerState();
        }
    }

    public void stop() {
        listeningThread.interrupt();
        aliveClientsFilter.interrupt();
        executorService.shutdownNow();
        dumpState();
    }

    private TrackerServer() {
        final int MAX_THREADS = 100;
        final int DEFAULT_THREADS = 10;
        final int SOCKET_TIMEOUT_MILLIS = 5000;

        LOGGER.info("Starting server on port " + TrackerProtocol.SERVER_PORT);

        executorService = new ThreadPoolExecutor(
                DEFAULT_THREADS, MAX_THREADS, 1, TimeUnit.MINUTES, new SynchronousQueue<>());

        listeningThread = new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(TrackerProtocol.SERVER_PORT)) {
                serverSocket.setSoTimeout(SOCKET_TIMEOUT_MILLIS);
                while (!Thread.interrupted()) {
                    try {
                        executorService.execute(new ClientServerInstance(serverSocket.accept()));
                    } catch (RejectedExecutionException rej) {
                        LOGGER.info("Rejected connection with client");
                    } catch (SocketTimeoutException ignored) {
                        // This is expected exception, nothing interesting
                    }
                    catch (IOException e) {
                        LOGGER.error("", e);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        aliveClientsFilter = new Thread(() -> {
            final int SLEEP_TIME_SECONDS = 300;
            final int MAX_TIME_TO_CONSIDER_ALIVE_SECONDS = 300;

            while (!Thread.interrupted()) {
                long currentTime = System.currentTimeMillis();
                state.timestampOfLastClientActivity.entrySet().forEach(
                        e -> {
                            if (currentTime - e.getValue() > MAX_TIME_TO_CONSIDER_ALIVE_SECONDS) {
                                state.makeInactive(e.getKey());
                            }
                        });
                try {
                    Thread.sleep(SLEEP_TIME_SECONDS * 1000);
                } catch (InterruptedException e) {
                    return;
                }
            }
        });

        readState();

        LOGGER.info("STate is " + (state == null));
        // this thread just dumps server state once in a while
        executorService.submit(() -> {
            final int SLEEP_SECONDS = 60;

            while (!Thread.interrupted()) {
                dumpState();
                try {
                    Thread.sleep(SLEEP_SECONDS * 1000);
                } catch (InterruptedException e) {
                    return;
                }
            }
        });
    }

    public void run() {
        final String exitCommand = "exit";
        final String listClientsCommand = "clients";
        final String listFilesCommand = "files";

        listeningThread.start();
        aliveClientsFilter.start();
        iohandler.onHelpRequested();
        for (String command = iohandler.readCommand(); command != null;
             command = iohandler.readCommand()) {
            LOGGER.info("New command: " + command);
            String[] s = command.split(" ");

            switch (s[0]) {
                case exitCommand: {
                    iohandler.onFinishingJobs();
                    stop();
                    return;
                }
                case listClientsCommand: {
                    iohandler.listClients(state.aliveClients);
                    break;
                }
                case listFilesCommand: {
                    iohandler.listFiles(state.uploadedFiles.values());
                    break;
                }
                case "": {
                    continue;
                }
                default: {
                    iohandler.onUnknownCommand(s[0]);
                }
            }
        }
    }

    public static void main(String[] args) {
        new TrackerServer().run();
    }
}
