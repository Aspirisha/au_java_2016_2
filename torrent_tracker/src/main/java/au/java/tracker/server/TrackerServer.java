package au.java.tracker.server;

import au.java.tracker.protocol.ServerRequestExecutor;
import au.java.tracker.protocol.TrackerProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.*;

/**
 * Created by andy on 11/7/16.
 */
public class TrackerServer {
    private static final Logger LOGGER = LoggerFactory.getLogger(TrackerServer.class);

    private final Thread listeningThread;
    private final ExecutorService executorService;
    private final ServerRequestExecutor requestExecutor = new ServerRequestExecutorImpl();
    private final IOHandler iohandler = new IOHandlerImpl();

    private class ClientServerInstance implements Runnable {
        private final Socket clientSocket;

        ClientServerInstance(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {
            TrackerProtocol.ServerProtocol p = TrackerProtocol.getServerProtocol();

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
        listeningThread.interrupt();
        executorService.shutdownNow();
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
        listeningThread.start();
    }

    public void run() {
        final String exitCommand = "exit";
        final String listClientsCommand = "list-client";
        final String listFilesCommand = "list-files";

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

                    break;
                }
                case listFilesCommand: {

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
