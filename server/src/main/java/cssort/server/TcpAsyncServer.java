package cssort.server;

import com.google.protobuf.InvalidProtocolBufferException;
import cssort.common.Settings;
import cssort.protocol.ClientServerProtocol;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import static java.lang.Thread.sleep;

/**
 * Created by andy on 2/19/17.
 */
public class TcpAsyncServer extends AbstractServer {
    private static final long TIMEOUT_CLIENT_MILLIS = 3000;

    @Override
    void run() {
        logger.debug("TcpAsyncServer started");
        System.out.println("TcpAsyncServer is listening on port " + Integer.toString(Settings.SERVER_PORT));
        try (AsynchronousServerSocketChannel server = AsynchronousServerSocketChannel
                .open()) {
            server.bind(new InetSocketAddress(Settings.SERVER_PORT));
            server.accept(null, new ClientConnectionProcessor(server));

            while (!Thread.interrupted()) {
                try {
                    sleep(CHECK_INTERRUPT_PERIOD_MILLIS);
                } catch (InterruptedException e) {
                    logger.debug("Interrupted");
                }
            }
        } catch (IOException e) {
            logger.error("", e);
        }
    }

    class ClientConnectionProcessor implements CompletionHandler<AsynchronousSocketChannel, Void> {
        final AsynchronousServerSocketChannel server;

        public ClientConnectionProcessor(AsynchronousServerSocketChannel server) {
            this.server = server;
        }

        @Override
        public void completed(AsynchronousSocketChannel client, Void ignored) {
            server.accept(null, this);
            RequestAttachment attachment = new RequestAttachment(client);
            client.read(attachment.header, TIMEOUT_CLIENT_MILLIS, TimeUnit.MILLISECONDS,
                    attachment, new HeaderReadingProcessor());
        }

        @Override
        public void failed(Throwable e, Void attachment) {
            logger.error("Error connecting to client", e);
        }
    }

    class RequestAttachment {
        long requestStartProcessTime;
        final AsynchronousSocketChannel client;
        final ByteBuffer header = ByteBuffer.allocate(Integer.BYTES);
        ByteBuffer body = null;
        ByteBuffer response = null;

        RequestAttachment(AsynchronousSocketChannel client) {
            this.client = client;
        }

        void clear() {
            header.clear();
            body = null;
            response = null;
            requestStartProcessTime = -1;
        }
    }

    private void closeClientChannel(AsynchronousSocketChannel client) {
        try {
            client.close();
        } catch (IOException e) {
            logger.error("Error closing client channel", e);
        }
    }

    class HeaderReadingProcessor implements CompletionHandler<Integer, RequestAttachment> {
        @Override
        public void completed(Integer bytesRead, RequestAttachment attachment) {
            if (bytesRead == -1) {
                closeClientChannel(attachment.client);
                return;
            }

            if (attachment.header.hasRemaining()) {
                attachment.client.read(attachment.header, TIMEOUT_CLIENT_MILLIS, TimeUnit.MILLISECONDS,
                        attachment, this);
                return;
            }
            logger.debug("Header fully read");
            attachment.header.flip();
            attachment.body = ByteBuffer.allocate(attachment.header.getInt());
            attachment.client.read(attachment.body, TIMEOUT_CLIENT_MILLIS, TimeUnit.MILLISECONDS,
                    attachment, new RequestBodyReadingProcessor());
        }

        @Override
        public void failed(Throwable e, RequestAttachment attachment) {
            logger.error("Error reading header", e);
            closeClientChannel(attachment.client);
        }
    }

    class RequestBodyReadingProcessor implements CompletionHandler<Integer, RequestAttachment> {
        @Override
        public void completed(Integer bytesRead, RequestAttachment attachment) {
            if (bytesRead == -1) {
                closeClientChannel(attachment.client);
                return;
            }

            if (attachment.body.hasRemaining()) {
                attachment.client.read(attachment.body, TIMEOUT_CLIENT_MILLIS, TimeUnit.MILLISECONDS,
                        attachment, this);
                return;
            }

            attachment.requestStartProcessTime = System.currentTimeMillis();
            logger.debug("Body fully read");
            attachment.body.flip();
            byte[] buf = new byte[attachment.body.limit()];
            attachment.body.get(buf);

            ArrayList<Integer> array;
            try {
                ClientServerProtocol.ClientToServerArray msg  = ClientServerProtocol.ClientToServerArray.parseFrom(buf);
                   array = new ArrayList<>(msg.getDataList());
            } catch (InvalidProtocolBufferException e) {
                logger.error("Error parsing client message", e);
                closeClientChannel(attachment.client);
                return;
            }

            logger.debug("Sorting!");
            long sortTimeStart = System.currentTimeMillis();
            sort(array);
            long sortTime = System.currentTimeMillis() - sortTimeStart;

            ClientServerProtocol.ServerToClientArray response = ClientServerProtocol.ServerToClientArray
                    .newBuilder()
                    .addAllData(array)
                    .setRequestTime(System.currentTimeMillis() - attachment.requestStartProcessTime)
                    .setSortTime(sortTime)
                    .build();

            attachment.response = ByteBuffer.allocate(Integer.BYTES + response.getSerializedSize());
            attachment.response.putInt(response.getSerializedSize());
            attachment.response.put(response.toByteArray());
            attachment.response.flip();

            logger.debug("Response is so long: " + attachment.response.limit());
            attachment.client.write(attachment.response, TIMEOUT_CLIENT_MILLIS, TimeUnit.MILLISECONDS,
                    attachment, new ResponseWriter());
        }

        @Override
        public void failed(Throwable e, RequestAttachment attachment) {
            logger.error("Error reading body", e);
            closeClientChannel(attachment.client);
        }
    }

    class ResponseWriter implements CompletionHandler<Integer, RequestAttachment> {

        @Override
        public void completed(Integer bytesWritten, RequestAttachment attachment) {
            if (bytesWritten == -1) {
                closeClientChannel(attachment.client);
                return;
            }

            if (attachment.response.hasRemaining()) {
                attachment.client.write(attachment.response, TIMEOUT_CLIENT_MILLIS, TimeUnit.MILLISECONDS,
                        attachment, this);
                return;
            }
            // we assume clients don't disconnect when they want to send more
            attachment.clear();
            attachment.client.read(attachment.header, TIMEOUT_CLIENT_MILLIS, TimeUnit.MILLISECONDS,
                    attachment, new HeaderReadingProcessor());
        }

        @Override
        public void failed(Throwable e, RequestAttachment attachment) {
            logger.error("Error writing response", e);
            closeClientChannel(attachment.client);
        }
    }
}
