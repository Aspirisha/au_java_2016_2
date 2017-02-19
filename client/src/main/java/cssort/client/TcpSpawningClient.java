package cssort.client;

import cssort.common.Settings;
import cssort.common.Statistics;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import static cssort.client.ClientController.logger;

/**
 * Created by andy on 2/19/17.
 */
public class TcpSpawningClient extends AbstractTcpClient {

    TcpSpawningClient(int N, int delta, int X, InetAddress serverAddress) {
        super(N, delta, X, serverAddress);
    }

    @Override
    List<Statistics.ServerRunResult> run() {
        List<Statistics.ServerRunResult> ret = new ArrayList<>(N);
        for (int msgNum = 0; msgNum < X; msgNum++) {
            if (Thread.interrupted()) {
                logger.debug("Client interrupted");
                return null;
            }
            try (Socket s = new Socket(serverAddress, Settings.SERVER_PORT);
                 DataOutputStream dos = new DataOutputStream(s.getOutputStream());
                 DataInputStream dis = new DataInputStream(s.getInputStream())) {
                Statistics.ServerRunResult response = performInteractionWithServer(dos, dis);
                ret.add(response);
            } catch (IOException e) {
                return null;
            }
        }
        return ret;
    }
}
