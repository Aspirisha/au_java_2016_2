package cssort.client;

import cssort.protocol.ClientServerProtocol;
import cssort.protocol.ProfilerClientProtocol;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    ServerRunResult performInteractionWithServer(DataOutputStream dos, DataInputStream dis) throws IOException {
        ClientServerProtocol.ClientToServerArray.Builder b = ClientServerProtocol.ClientToServerArray.newBuilder();
        List<Integer> a = new ArrayList<>(N);
        Random r = new Random(System.currentTimeMillis());
        for (int i = 0; i < N; i++) {
            a.set(i, r.nextInt());
        }
        b.addAllData(a);

        ClientServerProtocol.ClientToServerArray msg = b.build();

        sleep();
        dos.writeInt(msg.getSerializedSize());
        msg.writeTo(dos);

        int size = dis.readInt();
        byte[] buf = new byte[size];

        int readBytes = 0;
        do {
            readBytes += dis.read(buf, readBytes, buf.length - readBytes);
        } while (readBytes < buf.length);

        ClientServerProtocol.ServerToClientArray response =
                ClientServerProtocol.ServerToClientArray.parseFrom(buf);
        return new ServerRunResult(response.getRequestTime(),
                response.getProcessTime());

    }

    abstract List<ServerRunResult> run() throws IOException;
}

@Data
@AllArgsConstructor
class ServerRunResult {
    long requestTime;
    long processTime;
}