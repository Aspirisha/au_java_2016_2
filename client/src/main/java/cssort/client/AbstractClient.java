package cssort.client;

import cssort.common.Statistics;
import cssort.common.Util;
import cssort.protocol.ClientServerProtocol;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by andy on 2/15/17.
 */
public abstract class AbstractClient {
    final int N;
    final int delta;
    final int X;
    long lastResponceTimestamp = -1;

    AbstractClient(int N, int delta, int X) {
        this.N = N;
        this.delta = delta;
        this.X = X;
    }

    private void sleep() {
        long sleptTime = System.currentTimeMillis() - lastResponceTimestamp;
        while (sleptTime < delta) {
            try {
                Thread.sleep(delta - sleptTime);
            } catch (InterruptedException e) {
                sleptTime = System.currentTimeMillis() - lastResponceTimestamp;
            }
        }
    }

    List<Integer> generateInput() {
        List<Integer> a = new ArrayList<>(N);
        Random r = new Random(System.currentTimeMillis());
        for (int i = 0; i < N; i++) {
            a.set(i, r.nextInt());
        }
        return a;
    }

    Statistics.ServerRunResult performInteractionWithServer(DataOutputStream dos, DataInputStream dis) throws IOException {
        ClientServerProtocol.ClientToServerArray msg =
                ClientServerProtocol.ClientToServerArray.newBuilder()
                .addAllData(generateInput()).build();
        sleep();
        dos.writeInt(msg.getSerializedSize());
        msg.writeTo(dos);

        byte[] buf = Util.readMessageWithSizePrepended(dis);

        ClientServerProtocol.ServerToClientArray response =
                ClientServerProtocol.ServerToClientArray.parseFrom(buf);
        return new Statistics.ServerRunResult(response.getRequestTime(),
                response.getProcessTime());

    }

    abstract List<Statistics.ServerRunResult> run();
}
