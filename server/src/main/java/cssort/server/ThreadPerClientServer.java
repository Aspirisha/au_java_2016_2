package cssort.server;

import cssort.common.Settings;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

/**
 * Created by andy on 2/16/17.
 */
public class ThreadPerClientServer extends AbstractPersistentTcpServer {
    public void run() {
        logger.debug("ThreadPerClientServer started");
        System.out.println("Listening on port " + Integer.toString(Settings.SERVER_PORT));
        try (ServerSocket s = new ServerSocket(Settings.SERVER_PORT)) {
            while (!Thread.interrupted()) {
                Socket client = s.accept();
                logger.debug("Client connected");
                new Thread(new ClientProcessor(client)).start();
            }
        } catch (IOException e) {
            logger.error("", e);
            e.printStackTrace();
        }
    }
}
