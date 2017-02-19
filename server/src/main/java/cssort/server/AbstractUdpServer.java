package cssort.server;

import com.google.protobuf.InvalidProtocolBufferException;
import cssort.common.Settings;
import cssort.common.UdpUtils;
import cssort.protocol.ClientServerProtocol;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by andy on 2/19/17.
 */
public abstract class AbstractUdpServer extends AbstractServer {
    final HashMap<SocketAddress, RequestDescriptor> currentRequests = new HashMap<>();

    RequestDescriptor getRequestDescriptor(SocketAddress client, DatagramSocket socket) {
        RequestDescriptor d = currentRequests.get(client);
        if (d == null) {
            d = new RequestDescriptor(client, socket);
            currentRequests.put(client, d);
        }
        return d;
    }

    class RequestDescriptor {
        final SocketAddress clientAddress;
        final DatagramSocket socket;
        final UdpUtils.ProtobufMessageReceiver<ClientServerProtocol.ClientToServerArray>
                messageReceiver = new UdpUtils.ProtobufMessageReceiver<>();

        RequestDescriptor(SocketAddress clientAddress, DatagramSocket socket) {
            this.clientAddress = clientAddress;
            this.socket = socket;
        }
    }


    class RequestProcessor implements Runnable {
        RequestDescriptor requestDescriptor;
        public RequestProcessor(RequestDescriptor d) {
            requestDescriptor = d;
        }

        @Override
        public void run() {
            long requestTimeStart = System.nanoTime();
            ClientServerProtocol.ClientToServerArray input;
            try {
                input = requestDescriptor.messageReceiver.getMessage(ClientServerProtocol.ClientToServerArray.PARSER);
            } catch (InvalidProtocolBufferException e) {
                logger.error("Error parsing client message", e);
                return;
            }

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
            try {
                UdpUtils.sendProtobufMessage(output, requestDescriptor.clientAddress, requestDescriptor.socket);
            } catch (IOException e) {
                logger.error("Error sending chunk", e);
            }
        }
    }

    @Override
    void run() {
        try(DatagramSocket server = new DatagramSocket(Settings.SERVER_PORT)) {
            server.setReceiveBufferSize(1000000);
            server.setSendBufferSize(1000000);
            server.setSoTimeout(CHECK_INTERRUPT_PERIOD_MILLIS);
            while (!Thread.interrupted()) {
                try {
                    byte[] buf = new byte[Settings.UDP_CHUNK_SIZE];
                    DatagramPacket packet = new DatagramPacket(buf, buf.length);
                    server.receive(packet);
                    SocketAddress clientAddress = packet.getSocketAddress();

                    RequestDescriptor d = getRequestDescriptor(clientAddress, server);
                    if (d.messageReceiver.onChunkReceived(packet) == UdpUtils.ReceiveResult.COMPLETED) {
                        currentRequests.remove(clientAddress);
                        onReceivedRequest(new RequestProcessor(d));
                    }
                } catch (IOException e) {
                    logger.error("Error receiving packet", e);
                }
            }
        } catch (SocketException e) {
            logger.error("Couldn't start server", e);
        }
    }

    abstract void onReceivedRequest(RequestProcessor p);
}
