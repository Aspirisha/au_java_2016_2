package cssort.server;

import cssort.common.Settings;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

/**
 * Created by andy on 2/16/17.
 */
public class ThreadPerClientServer extends AbstractPersistentTcpServer {
    public void run() {
        logger.debug("ThreadPerClientServer started");
        System.out.println("ThreadPerClientServer is listening on port " + Integer.toString(Settings.SERVER_PORT));
        try (ServerSocket s = new ServerSocket(Settings.SERVER_PORT)) {
            s.setSoTimeout(3000);
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
