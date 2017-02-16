package cssort.client;

import cssort.common.Settings;

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
    public List<ServerRunResult> run() throws IOException {
        List<ServerRunResult> ret = new ArrayList<>(N);
        try (Socket s = new Socket("localhost", Settings.SERVER_PORT);
             DataOutputStream dos = new DataOutputStream(s.getOutputStream());
             DataInputStream dis = new DataInputStream(s.getInputStream())) {
            for (int msgNum = 0; msgNum < X; msgNum++) {
                ServerRunResult response = performInteractionWithServer(dos, dis);
                ret.set(msgNum, response);
            }
        }
        return ret;
    }
}
