package cssort.server;

import cssort.common.Settings;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

/**
 * Created by andy on 2/16/17.
 */
public class ThreadPerClientServerOldIO extends AbstractPersistentOldIOTcpServer {
    public void run() {
        logger.debug("ThreadPerClientServerOldIO started");
        System.out.println("ThreadPerClientServerOldIO is listening on port " + Integer.toString(Settings.SERVER_PORT));
        try (ServerSocket s = new ServerSocket(Settings.SERVER_PORT)) {
            s.setSoTimeout(CHECK_INTERRUPT_PERIOD_MILLIS);
            while (!Thread.interrupted()) {
                try {
                    Socket client = s.accept();
                    logger.debug("Client connected");
                    new Thread(new ClientProcessor(client)).start();
                } catch (SocketTimeoutException e) {}
            }
        } catch (IOException e) {
            logger.error("", e);
            e.printStackTrace();
        }
    }
}
