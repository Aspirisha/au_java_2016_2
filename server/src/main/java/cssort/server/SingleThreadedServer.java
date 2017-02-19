package cssort.server;

import cssort.common.Settings;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

/**
 * Created by andy on 2/19/17.
 */
public class SingleThreadedServer extends AbstractServer {
    @Override
    void run() {
        logger.debug("SingleThreadedServer started");
        System.out.println("SingleThreadedServer is listening on port " + Integer.toString(Settings.SERVER_PORT));
        try (ServerSocket s = new ServerSocket(Settings.SERVER_PORT)) {
            s.setSoTimeout(3000);
            while (!Thread.interrupted()) {
                try (Socket client = s.accept();
                     DataInputStream dis = new DataInputStream(client.getInputStream());
                DataOutputStream dos = new DataOutputStream(client.getOutputStream())) {
                    processClient(System.currentTimeMillis(), dis, dos);
                } catch (SocketTimeoutException e) {}
                catch (IOException e) {
                    logger.info("Client probably disconnected", e);
                }
            }
        } catch (IOException e) {
            logger.error("", e);
            e.printStackTrace();
        }
    }
}
