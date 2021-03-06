package cssort.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

/**
 * Created by andy on 2/18/17.
 */
public abstract class AbstractPersistentOldIOTcpServer extends AbstractOldIOTcpServer {
    class ClientProcessor implements Runnable {
        final Socket client;

        ClientProcessor(Socket s) {
            client = s;
        }

        @Override
        public void run() {
            try {
                client.setSoTimeout(5000);
                DataInputStream dis = new DataInputStream(client.getInputStream());
                DataOutputStream dos = new DataOutputStream(client.getOutputStream());
                while (true) {
                    processClient(dis, dos);
                }
            } catch (IOException e) {
                logger.error("Client disconnected");
            }

            try {
                client.close();
            } catch (IOException e) {
                logger.error("Error closing client socket", e);
            }
        }
    }
}
