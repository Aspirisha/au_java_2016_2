package au.java.tracker.client;

import au.java.tracker.protocol.ClientRequestExecutor;
import au.java.tracker.protocol.TrackerProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.*;

/**
 * Created by andy on 11/8/16.
 */
public class ConnectionListener implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionListener.class);
    private static final int MAX_THREADS = 30;
    private static final int DEFAULT_THREADS = 1;

    private final int port;
    private final ExecutorService uploadExecutorService;

    ConnectionListener(int port) {
        this.port = port;
        uploadExecutorService = new ThreadPoolExecutor(DEFAULT_THREADS,
                MAX_THREADS, 1, TimeUnit.MINUTES, new SynchronousQueue<>());
    }

    @Override
    public void run() {
        LOGGER.info("Starting listening on port " + port);
        try (ServerSocket listeningSocket = new ServerSocket(port)) {
            listeningSocket.setSoTimeout(5000);
            while (!Thread.interrupted()) {
                try {
                    uploadExecutorService.execute(new ClientServerInstance(listeningSocket.accept()));
                } catch (RejectedExecutionException rej) {
                    LOGGER.info("Rejected connection with client");
                } catch (IOException e) {
                    LOGGER.error("", e);
                }
            }

            uploadExecutorService.shutdownNow();
        } catch (IOException e) {
            LOGGER.error("", e);
        }
    }

    private class ClientServerInstance implements Runnable {
        private final Socket clientSocket;
        private final ClientRequestExecutor requestExecutor = new ClientRequestExecutorImpl();
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
                }
            }
        }
    }
}
