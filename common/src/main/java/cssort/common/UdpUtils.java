package cssort.common;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Parser;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;

/**
 * Created by andy on 2/19/17.
 */
public class UdpUtils {
    static int evaluateChunksNumber(int messageSize) {
        int denominator = Settings.UDP_CHUNK_SIZE - 4;
        int numerator = messageSize + Integer.BYTES + denominator - 1;
        return numerator / denominator;
    }

    static int evaluateTotalSize(int messageSize, int chunksNumber) {
        return messageSize + Integer.BYTES * chunksNumber + Integer.BYTES;
    }


    public static void sendProtobufMessage(com.google.protobuf.GeneratedMessage msg, SocketAddress address,
                                           DatagramSocket socket) throws IOException {
        sendProtobufMessage(msg, data -> {
            DatagramPacket d = new DatagramPacket(data, data.length, address);
            socket.send(d);
        });
    }

    public static void sendProtobufMessage(com.google.protobuf.GeneratedMessage msg, InetAddress address,
                                           int port, DatagramSocket socket) throws IOException {
        sendProtobufMessage(msg, data -> {
            DatagramPacket d = new DatagramPacket(data, data.length, address, port);
            socket.send(d);
        });
    }

    static void sendProtobufMessage(com.google.protobuf.GeneratedMessage msg, DatagramSender sender) throws IOException {
        int outputDataSize = msg.getSerializedSize();
        int outputChunksNumber = evaluateChunksNumber(outputDataSize);
        byte[] messageBytes = msg.toByteArray();

        byte[] data = new byte[Settings.UDP_CHUNK_SIZE];
        for (int i = 0, offset = 0; i < outputChunksNumber; i++) {
            ByteBuffer wrapper = ByteBuffer.wrap(data);
            wrapper.putInt(i);
            if (i == 0) {
                wrapper.putInt(outputDataSize);
            }

            int length = Math.min(wrapper.remaining(), messageBytes.length - offset);
            wrapper.put(messageBytes, offset, length);
            offset += length;
           // for (int j = 0; j < 3; j++)
                sender.sendDatagram(data);
        }
    }

    interface DatagramSender {
        void sendDatagram(byte[] data) throws IOException;
    }

    public enum ReceiveResult {
        UNCOMPLETED,
        COMPLETED,
        DUPLICATE_CHUNK
    }

    public static class ProtobufMessageReceiver<T extends com.google.protobuf.GeneratedMessage> {
        final ArrayList<byte[]> chunks = new ArrayList<>();
        int receivedChunks = 0;
        int messageSize = -1;
        int chunksNumber = -1;

        private boolean addChunk(int chunkId, byte[] chunk) {
            for (int i = chunks.size(); i < chunkId; i++) {
                chunks.add(null);
            }
            if (chunks.size() > chunkId && chunks.get(chunkId) != null) {
                // chunk already added
                return false;
            }
            chunks.add(chunk);
            receivedChunks++;
            return true;
        }

        public ReceiveResult onChunkReceived(DatagramPacket chunk) {
            assert(chunk.getLength() == Settings.UDP_CHUNK_SIZE);
            ByteBuffer wrapped = ByteBuffer.wrap(chunk.getData());
            int chunkId = wrapped.getInt();
            if (!addChunk(chunkId, chunk.getData())) return ReceiveResult.DUPLICATE_CHUNK;

            if (chunkId == 0) {
                messageSize = wrapped.getInt();
                chunksNumber = evaluateChunksNumber(messageSize);
            }

            return chunksNumber == receivedChunks ? ReceiveResult.COMPLETED : ReceiveResult.UNCOMPLETED;
        }

        public T getMessage(Parser<T> messageParser) throws InvalidProtocolBufferException {
            byte[] buf = new byte[messageSize];
            for (int i = 0, destOffset = 0; i < chunksNumber;
                 i++) {
                int srcOffset = i == 0 ? 2 * Integer.BYTES : Integer.BYTES;

                int length = Math.min(buf.length - destOffset, Settings.UDP_CHUNK_SIZE - srcOffset);
                System.arraycopy(chunks.get(i), srcOffset, buf, destOffset, length);
                destOffset += length;
            }

            return messageParser.parseFrom(buf);
        }
    }
}
