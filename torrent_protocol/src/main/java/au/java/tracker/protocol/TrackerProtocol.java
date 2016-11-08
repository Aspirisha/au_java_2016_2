package au.java.tracker.protocol;

import au.java.tracker.protocol.util.TimeoutSocketConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;


/**
 * Created by andy on 11/7/16.
 */
public class TrackerProtocol {
    private static final Logger LOGGER = LoggerFactory.getLogger(TrackerProtocol.class);

    public static final int CHUNK_SIZE = 1024 * 1024 * 4; // 4 Mb
    public static final int SERVER_PORT = 8081;
    // tracker requests
    private static final byte LIST_COMMAND_ID = 1;
    private static final byte UPLOAD_COMMAND_ID = 2;
    private static final byte SOURCES_COMMAND_ID = 3;
    private static final byte UPDATE_COMMAND_ID = 4;


    public static ServerProtocol getServerProtocol() {
        return new ServerProtocolImpl();
    }

    public static ClientToServerProtocol getClientToServerProtocol(Socket socket) throws Exception {
        return new ClientToServerProtocolImpl(socket);
    }

    public static PeerClientProtocol getPeerClientProtocol(Socket socket) {
        return new PeerClientProtocolImpl(socket);
    }

    public static PeerServerProtocol getPeerServerProtocol() {
        return new PeerServerProtocolImpl();
    }

    public interface ServerProtocol {
        void processCommand(DataInputStream dis, DataOutputStream dos,
                            ServerRequestExecutor requestExecutor) throws IOException;
    }

    public interface ClientToServerProtocol extends ServerRequestExecutor { }

    public interface PeerServerProtocol {
        void processCommand(DataInputStream dis, DataOutputStream dos,
                            ClientRequestExecutor requestExecutor) throws IOException;
    }

    public interface PeerClientProtocol extends ClientRequestExecutor { }

    private static class ServerProtocolImpl implements ServerProtocol {
        @Override
        public void processCommand(DataInputStream dis, DataOutputStream dos,
                                   ServerRequestExecutor requestExecutor) throws IOException {
            byte request = dis.readByte();

            switch (request) {
                case LIST_COMMAND_ID: {

                    break;
                }
                case UPLOAD_COMMAND_ID: {

                    break;
                }
                case SOURCES_COMMAND_ID: {

                    break;
                }
                case UPDATE_COMMAND_ID: {

                    break;
                }
                default:
                    break;
            }
        }
    }

    private static class PeerServerProtocolImpl implements PeerServerProtocol {

        @Override
        public void processCommand(DataInputStream dis, DataOutputStream dos, ClientRequestExecutor requestExecutor) throws IOException {

        }
    }

    private static class ClientToServerProtocolImpl implements ClientToServerProtocol {
        private final java.net.Socket socket;
        private final DataOutputStream dos;
        private final DataInputStream dis;

        public ClientToServerProtocolImpl(Socket socket) throws Exception {
            this.socket = socket;
            dos = new DataOutputStream(socket.getOutputStream());
            dis = new DataInputStream(socket.getInputStream());
        }

        @Override
        public List<FileDescriptor> serverRequestList() throws IOException, ClassNotFoundException {
            dos.writeInt(LIST_COMMAND_ID);
            int filesNumber = dis.readInt();

            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
            List<FileDescriptor> result = new ArrayList<>(filesNumber);
            for (int i = 0; i < filesNumber; i++) {
                FileDescriptor fd = (FileDescriptor) ois.readObject();
                result.add(fd);
            }

            return result;
        }

        @Override
        public int serverRequestUpload(String fileName, long fileSize) {
            return 0;
        }

        @Override
        public List<ClientDescriptor> serverRequestSources(int fileId) {
            return null;
        }

        @Override
        public boolean serverRequestUpdate(ClientDescriptor client, Set<Integer> fileIds) {
            return false;
        }

        @Override
        public FileDescriptor describeFile(int fileId) {
            return null;
        }
    }

    private static class PeerClientProtocolImpl implements PeerClientProtocol {
        private final Socket socket;

        private PeerClientProtocolImpl(Socket socket) {
            this.socket = socket;
        }

        @Override
        public List<Integer> clientRequestStat(int fileId) {
            return null;
        }

        @Override
        public byte[] clientRequestGet(int fileId, int partNum) {
            return new byte[0];
        }
    }

}
