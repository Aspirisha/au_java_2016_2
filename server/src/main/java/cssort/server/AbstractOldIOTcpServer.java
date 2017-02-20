package cssort.server;

import cssort.common.Util;
import cssort.protocol.ClientServerProtocol;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by andy on 2/20/17.
 */
public abstract class AbstractOldIOTcpServer extends AbstractServer {

    void processClient(DataInputStream dis, DataOutputStream dos) throws IOException {
        byte[] buf = Util.readMessageWithSizePrepended(dis);

        long requestTimeStart = System.nanoTime();
        ClientServerProtocol.ClientToServerArray input = ClientServerProtocol.ClientToServerArray.parseFrom(buf);
        ArrayList<Integer> l = new ArrayList<>(input.getDataList());
        long sortStart = System.nanoTime();
        sort(l);
        long sortTime = System.nanoTime() - sortStart;
        long requestTime = System.nanoTime() - requestTimeStart;
        ClientServerProtocol.ServerToClientArray output =
                ClientServerProtocol.ServerToClientArray.newBuilder()
                        .addAllData(l)
                        .setSortTime(sortTime)
                        .setRequestTime(requestTime)
                        .build();
        dos.writeInt(output.getSerializedSize());
        output.writeTo(dos);
    }

}
