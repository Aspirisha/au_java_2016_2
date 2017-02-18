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
public class ThreadPerClientServer extends AbstractServer {
    public void run() {
        logger.debug("Server started");
        System.out.println("Listening on port " + Integer.toString(Settings.SERVER_PORT));
        try (ServerSocket s = new ServerSocket(Settings.SERVER_PORT)) {
            while (!Thread.interrupted()) {
                logger.debug("Client connected");
                Socket client = s.accept();

                new Thread(() -> {
                    try {
                        DataInputStream dis = new DataInputStream(client.getInputStream());
                        DataOutputStream dos = new DataOutputStream(client.getOutputStream());
                        while (true) {
                            client.setSoTimeout(5000);
                            processClient(System.currentTimeMillis(), dis, dos);
                        }
                    } catch (IOException e) {
                        logger.error("Client disconnected", e);
                    }
                }).start();
            }
        } catch (IOException e) {
            logger.error("", e);
            e.printStackTrace();
        }
    }
}
