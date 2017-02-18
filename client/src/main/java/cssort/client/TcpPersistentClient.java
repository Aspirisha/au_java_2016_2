package cssort.client;

import cssort.common.Settings;
import cssort.common.Statistics;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

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
            for (int msgNum = 0; msgNum < X; msgNum++) {
                Statistics.ServerRunResult response = performInteractionWithServer(dos, dis);
                ret.set(msgNum, response);
            }
        } catch (IOException e) {
            return null;
        }

        return ret;
    }
}
