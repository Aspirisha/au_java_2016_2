package cssort.client;

import cssort.common.Settings;
import cssort.common.Statistics;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import static cssort.client.ClientController.logger;

/**
 * Created by andy on 2/15/17.
 */
public class TcpPersistentClient extends AbstractClient {
    TcpPersistentClient(int N, int delta, int X) {
        super(N, delta, X);
    }

    @Override
    public List<Statistics.ServerRunResult> run() {
        List<Statistics.ServerRunResult> ret = new ArrayList<>(N);
        try (Socket s = new Socket("localhost", Settings.SERVER_PORT);
             DataOutputStream dos = new DataOutputStream(s.getOutputStream());
             DataInputStream dis = new DataInputStream(s.getInputStream())) {
            logger.debug("Connected to server");
            for (int msgNum = 0; msgNum < X; msgNum++) {
                Statistics.ServerRunResult response = performInteractionWithServer(dos, dis);
                ret.add(response);
            }
        } catch (IOException e) {
            return null;
        }

        return ret;
    }
}
