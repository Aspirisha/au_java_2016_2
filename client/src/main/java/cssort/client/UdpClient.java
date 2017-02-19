package cssort.client;

import cssort.common.Settings;
import cssort.common.Statistics;
import cssort.common.UdpUtils;
import cssort.common.Util;
import cssort.protocol.ClientServerProtocol;
import lombok.extern.slf4j.Slf4j;
import sun.misc.RequestProcessor;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

import static cssort.client.ClientController.logger;

/**
 * Created by andy on 2/19/17.
 */
@Slf4j
public class UdpClient extends AbstractClient {

    UdpClient(int N, int delta, int X, InetAddress serverAddress) {
        super(N, delta, X, serverAddress);
    }

    Statistics.ServerRunResult performInteractionWithServer(DatagramSocket socket) throws IOException {
        ClientServerProtocol.ClientToServerArray msg =
                ClientServerProtocol.ClientToServerArray.newBuilder()
                        .addAllData(generateInput()).build();
        sleep();

        UdpUtils.sendProtobufMessage(msg, serverAddress, Settings.SERVER_PORT, socket);

        UdpUtils.ProtobufMessageReceiver<ClientServerProtocol.ServerToClientArray> messageReceiver =
                new UdpUtils.ProtobufMessageReceiver<>();
        while (true) {
            byte[] buf = new byte[Settings.UDP_CHUNK_SIZE];
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            socket.receive(packet);
            // unfortunately, localhost can't be always deduced to 127.0.0.1,
            // so this is a workaround for local testing
            // IRL this should be uncommented
            if (/*packet.getAddress() != serverAddress || */packet.getPort() != Settings.SERVER_PORT) {
                logger.debug("Received random message");
                continue;
            }

            if (messageReceiver.onChunkReceived(packet) == UdpUtils.ReceiveResult.COMPLETED) {
                lastResponceTimestamp = System.currentTimeMillis();
                ClientServerProtocol.ServerToClientArray response =
                    messageReceiver.getMessage(ClientServerProtocol.ServerToClientArray.PARSER);
                return new Statistics.ServerRunResult(response.getSortTime(),
                        response.getRequestTime());
            }
        }
    }

    @Override
    List<Statistics.ServerRunResult> run() {
        List<Statistics.ServerRunResult> ret = new ArrayList<>(N);
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setReceiveBufferSize(1000000);
            socket.setSendBufferSize(1000000);
            socket.setSoTimeout(1000);
            logger.debug("Connected to server");
            for (int msgNum = 0; msgNum < X; msgNum++) {
                if (Thread.interrupted()) {
                    logger.debug("Client interrupted");
                    return null;
                }
                Statistics.ServerRunResult response = performInteractionWithServer(socket);
                ret.add(response);
            }
        } catch (IOException e) {
            log.error("Error occurred", e);
            return null;
        }
        return ret;
    }
}
