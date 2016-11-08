package au.java.tracker.protocol;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
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

    public static ClientToServerProtocol getClientToServerProtocol(String serverIp) throws Exception {
        return new ClientToServerProtocolImpl(serverIp);
    }

    public static PeerClientProtocol getPeerClientProtocol(ClientDescriptor desc) throws IOException {
        return new PeerClientProtocolImpl(desc);
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
        private final Socket socket;

        private Socket tryConnectToServer(String serverIp) {
            final int WAIT_TIMOUT = 2000;

            final Socket[] s = new Socket[1];
            Thread socketThread=new Thread() {
                public void run() {
                    try {
                        s[0] = new Socket(serverIp, SERVER_PORT);
                    }
                    catch (Exception e) {
                        // don't care here
                    }
                }
            };
            socketThread.start();

            try {
                socketThread.join(WAIT_TIMOUT);
            } catch (Exception e) {
                LOGGER.error("", e);
            }

            return s[0];
        }

        public ClientToServerProtocolImpl(String serverIp) throws Exception {
            socket = tryConnectToServer(serverIp);

            if (socket == null) {
                throw new Exception("Couldn't connect to server");
            }
        }

        @Override
        public List<FileDescriptor> serverRequestList() {
            return null;
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
        private final ClientDescriptor clientDescriptor;
        private final Socket socket;

        private PeerClientProtocolImpl(ClientDescriptor clientDescriptor) throws IOException {
            this.clientDescriptor = clientDescriptor;
            socket = new Socket(clientDescriptor.getStringIp(), clientDescriptor.getPort());
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
