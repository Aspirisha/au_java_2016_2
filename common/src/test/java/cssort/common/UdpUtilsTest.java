package cssort.common;

import cssort.protocol.ClientServerProtocol;
import org.junit.Test;

import java.io.IOException;
import java.net.DatagramPacket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by andy on 2/19/17.
 */
public class UdpUtilsTest {

    @Test
    public void testGetMessage() throws IOException {
        final List<Integer> smallData = Arrays.asList(3, 2, 1);
        testMessageSentEqualMessageGot(smallData);

        final List<Integer> bigData = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            bigData.add(1);
        }
        testMessageSentEqualMessageGot(bigData);
    }

    private void testMessageSentEqualMessageGot(List<Integer> input) throws IOException {
        UdpUtils.ProtobufMessageReceiver<ClientServerProtocol.ClientToServerArray> receiver =
                new UdpUtils.ProtobufMessageReceiver<>();

        ClientServerProtocol.ClientToServerArray clientMsg =
                ClientServerProtocol.ClientToServerArray
                        .newBuilder()
                        .addAllData(input)
                        .build();


        List<UdpUtils.ReceiveResult> results = new ArrayList<>();
        UdpUtils.sendProtobufMessage(clientMsg, data -> {
            byte[] c = data.clone();
            DatagramPacket d = new DatagramPacket(c, c.length);
            results.add(receiver.onChunkReceived(d));
        });

        assertEquals(results.get(results.size() - 1), UdpUtils.ReceiveResult.COMPLETED);
        ClientServerProtocol.ClientToServerArray receivedMsg =
                receiver.getMessage(ClientServerProtocol.ClientToServerArray.PARSER);
        assertEquals(receivedMsg.getDataList(), clientMsg.getDataList());
    }
}
