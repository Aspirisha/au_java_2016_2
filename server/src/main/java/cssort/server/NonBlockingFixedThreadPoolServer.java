package cssort.server;

import com.google.protobuf.InvalidProtocolBufferException;
import cssort.common.Settings;
import cssort.protocol.ClientServerProtocol;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by andy on 2/18/17.
 */
public class NonBlockingFixedThreadPoolServer extends AbstractServer {
    private final ExecutorService requestThreadPool = Executors.newFixedThreadPool(20);
    private final ArrayList<SocketChannel> clientChannels = new ArrayList<>();

    @Override
    void run() {
        logger.debug("NonBlockingFixedThreadPoolServer started");
        System.out.println("NonBlockingFixedThreadPoolServer is listening on port " + Integer.toString(Settings.SERVER_PORT));

        try (ServerSocketChannel socket = ServerSocketChannel.open();
             Selector selector = Selector.open()) {
            socket.configureBlocking(false);
            socket.bind(new InetSocketAddress(Settings.SERVER_PORT));

            socket.register(selector, SelectionKey.OP_ACCEPT);

            while (!Thread.interrupted()) {
                selector.select(CHECK_INTERRUPT_PERIOD_MILLIS);
                Iterator<SelectionKey> keyIterator = selector.selectedKeys().iterator();

                while (keyIterator.hasNext()) {
                    SelectionKey key = keyIterator.next();
                    if (key.isAcceptable()) {
                        onAccept(key);
                    } else if (key.isReadable()) {
                        onRead(key);
                    } else if (key.isWritable()) {
                        onWrite(key);
                    }

                    keyIterator.remove();
                }
            }

            for (SocketChannel channel : clientChannels) {
                channel.close();
            }
        } catch (IOException e) {
            logger.error("", e);
        }

    }

    private void onWrite(SelectionKey key) {
        SocketChannel channel = (SocketChannel) key.channel();
        RequestData data = (RequestData) key.attachment();

        if (!data.isSorted) return;

        if (data.outputMessage == null) {
            ClientServerProtocol.ServerToClientArray response = ClientServerProtocol.ServerToClientArray
                    .newBuilder()
                    .addAllData(data.array)
                    .setRequestTime(System.currentTimeMillis() - data.requestStartTime)
                    .setSortTime(data.sortTimeEnd - data.sortTimeStart)
                    .build();
            data.outputMessage = ByteBuffer.allocate(Integer.BYTES + response.getSerializedSize());
            data.outputMessage.putInt(response.getSerializedSize());
            data.outputMessage.put(response.toByteArray());
            data.outputMessage.flip();
        }

        try {
            channel.write(data.outputMessage);
            logger.debug("output message buffer has remaining: " + data.outputMessage.remaining());
            if (!data.outputMessage.hasRemaining()) {
                data.clear();
                logger.debug("written fully");
            } else {
                logger.debug("written partially");
            }
        } catch (IOException e) {
            closeConnection(channel, key);
            logger.error("Error writing to client", e);
        }
    }

    private void closeConnection(SocketChannel channel, SelectionKey key) {
        try {
            channel.close();
        } catch (IOException e) {
            logger.error("Error closing channel", e);
        }
        key.cancel();
    }

    private void readOrDisconnect(SocketChannel channel, SelectionKey key, ByteBuffer buffer) {
        try {
            if (channel.read(buffer) < 0) {
                closeConnection(channel, key);
            }
        } catch (IOException e) {
            logger.error("Client disconnected", e);
            closeConnection(channel, key);
        }
    }

    private void onRead(SelectionKey key) {

        SocketChannel channel = (SocketChannel) key.channel();
        RequestData data = (RequestData) key.attachment();
        if (data.header.hasRemaining()) {
            readOrDisconnect(channel, key, data.header);
            return;
        }
        if (data.body == null) {
            data.header.flip();
            int size = data.header.getInt();
            data.body = ByteBuffer.allocate(size);
        }

        if (data.body.hasRemaining()) {
            readOrDisconnect(channel, key, data.body);
            if (data.body.hasRemaining()) return;
        }

        data.requestStartTime = System.currentTimeMillis();
        data.body.flip();
        byte[] buf = new byte[data.body.limit()];
        data.body.get(buf);

        // this all works cool since we know that client doesn't send new array
        // before he or she receives current result
        // so after this line we can safely think that onRead is not called until we send results
        try {
            ClientServerProtocol.ClientToServerArray msg  = ClientServerProtocol.ClientToServerArray.parseFrom(buf);
            requestThreadPool.submit(() -> {
                data.array = new ArrayList<>(msg.getDataList());
                data.sortTimeStart = System.currentTimeMillis();
                sort(data.array);
                data.sortTimeEnd = System.currentTimeMillis();

                data.isSorted = true;
            });
        } catch (InvalidProtocolBufferException e) {
            logger.error("Error parsing client message", e);
            closeConnection(channel, key);
            return;
        }


    }

    class RequestData {
        long requestStartTime;
        long sortTimeStart;
        long sortTimeEnd;
        ArrayList<Integer> array = null;
        ByteBuffer header = ByteBuffer.allocate(Integer.BYTES);
        ByteBuffer body = null;
        ByteBuffer outputMessage = null;
        volatile boolean isSorted = false;

        void clear() {
            array = null;
            header.clear();
            body = null;
            isSorted = false;
            outputMessage = null;
        }
    }

    private void onAccept(SelectionKey key) {
        try {
            SocketChannel client = ((ServerSocketChannel) key.channel()).accept();
            client.configureBlocking(false);
            client.register(key.selector(), SelectionKey.OP_READ | SelectionKey.OP_WRITE,
                    new RequestData());
            clientChannels.add(client);
        } catch (IOException e) {
            logger.error("Error working with client", e);
        }
    }
}
