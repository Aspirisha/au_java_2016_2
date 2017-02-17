package cssort.server;

import cssort.common.Util;
import cssort.protocol.ClientServerProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import static java.lang.System.in;

/**
 * Created by andy on 2/16/17.
 */
public abstract class AbstractServer {
    protected static final Logger logger = LoggerFactory.getLogger(Main.class);

    void sort(ArrayList<Integer> data) {
        for (int i = 0; i < data.size(); i++) {
            int minIdx = i;
            for (int j = i + 1; j < data.size(); j++) {
                if (data.get(j) < data.get(minIdx)) {
                    minIdx = j;
                }
            }

            Collections.<Integer>swap(data, minIdx, i);
        }
    }

    void processClient(long processTimeStart, DataInputStream dis, DataOutputStream dos) throws IOException {
        byte[] buf = Util.readMessageWithSizePrepended(dis);

        ClientServerProtocol.ClientToServerArray input = ClientServerProtocol.ClientToServerArray.parseFrom(buf);
        ArrayList<Integer> l = new ArrayList<>(input.getDataList());
        long sortStart = System.currentTimeMillis();
        sort(l);
        long sortTime = System.currentTimeMillis() - sortStart;
        long processTime = System.currentTimeMillis() - processTimeStart;
        ClientServerProtocol.ServerToClientArray output =
                ClientServerProtocol.ServerToClientArray.newBuilder()
                .addAllData(l)
                .setProcessTime(processTime)
                .setRequestTime(sortTime)
                .build();
        dos.writeInt(output.getSerializedSize());
        output.writeTo(dos);
    }

    abstract void run();
}
