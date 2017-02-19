package cssort.client;

import cssort.common.Statistics;
import cssort.common.Util;
import cssort.protocol.ClientServerProtocol;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;

/**
 * Created by andy on 2/19/17.
 */
public abstract class AbstractTcpClient extends AbstractClient {

    AbstractTcpClient(int N, int delta, int X, InetAddress serverAddress) {
        super(N, delta, X, serverAddress);
    }

    Statistics.ServerRunResult performInteractionWithServer(DataOutputStream dos, DataInputStream dis) throws IOException {
        ClientServerProtocol.ClientToServerArray msg =
                ClientServerProtocol.ClientToServerArray.newBuilder()
                        .addAllData(generateInput()).build();
        sleep();
        dos.writeInt(msg.getSerializedSize());
        msg.writeTo(dos);

        byte[] buf = Util.readMessageWithSizePrepended(dis);
        lastResponceTimestamp = System.currentTimeMillis();

        ClientServerProtocol.ServerToClientArray response =
                ClientServerProtocol.ServerToClientArray.parseFrom(buf);
        return new Statistics.ServerRunResult(response.getSortTime(),
                response.getRequestTime());

    }
}
