package au.java.tracker.protocol;

import au.java.tracker.protocol.util.IpValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.oracle.jrockit.jfr.ContentType.Bytes;


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
    private static final byte DESCRIBE_COMMAND_ID = 5;

    //peer requests
    private static final byte STAT_COMMAND_ID = 1;
    private static final byte GET_COMMAND_ID = 2;


    public static ServerProtocol getServerProtocol() {
        return new ServerProtocolImpl();
    }

    public static ClientToServerProtocol getClientToServerProtocol(Socket socket) throws Exception {
        return new ClientToServerProtocolImpl(socket);
    }

    public static PeerClientProtocol getPeerClientProtocol(Socket socket) throws IOException {
        return new PeerClientProtocolImpl(socket);
    }

    public static PeerServerProtocol getPeerServerProtocol() {
        return new PeerServerProtocolImpl();
    }

    public interface ServerProtocol {
        void processCommand(Socket socket,
                            ServerRequestExecutor requestExecutor) throws Exception;
    }

    public interface ClientToServerProtocol extends ServerRequestExecutor { }

    public interface PeerServerProtocol {
        void processCommand(DataInputStream dis, DataOutputStream dos,
                            ClientRequestExecutor requestExecutor) throws IOException;
    }

    public interface PeerClientProtocol extends ClientRequestExecutor { }

    private static class ServerProtocolImpl implements ServerProtocol {
        @Override
        public void processCommand(Socket socket,
                                   ServerRequestExecutor requestExecutor) throws Exception {
            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
            DataInputStream dis = new DataInputStream(socket.getInputStream());

            byte request = dis.readByte();

            switch (request) {
                case LIST_COMMAND_ID: {
                    LOGGER.info("Client " + socket.getInetAddress() + " requested list");
                    List<FileDescriptor> l = requestExecutor.serverRequestList();

                    dos.writeInt(l.size());
                    for (FileDescriptor fd : l) {
                        dos.writeInt(fd.getId());
                        dos.writeUTF(fd.getName());
                        dos.writeLong(fd.getSize());
                    }
                    break;
                }
                case UPLOAD_COMMAND_ID: {
                    LOGGER.info("Client " + socket.getInetAddress() + " requested upload");
                    String name = dis.readUTF();
                    long size = dis.readLong();

                    ClientDescriptor cd = new ClientDescriptor(
                            socket.getInetAddress().getHostAddress(), socket.getPort());

                    int id = requestExecutor.serverRequestUpload(name, size, cd);
                    dos.writeInt(id);
                    break;
                }
                case SOURCES_COMMAND_ID: {
                    LOGGER.info("Client " + socket.getInetAddress() + " requested sources");
                    int id = dis.readInt();

                    Set<ClientDescriptor> src = requestExecutor.serverRequestSources(id);
                    if (src == null) {
                        dos.writeInt(-1);
                        break;
                    }

                    dos.writeInt(src.size());
                    for (ClientDescriptor cd : src) {
                        for (Integer b : cd.getIp()) {
                            dos.writeByte(b);
                        }
                        dos.writeInt(cd.getPort());
                    }
                    break;
                }
                case UPDATE_COMMAND_ID: {
                    LOGGER.info("Client " + socket.getInetAddress() + " requested update");
                    int port = dis.readInt();
                    int filesCount = dis.readInt();
                    Set<Integer> fileIds = new HashSet<>(filesCount);
                    for (int i = 0; i < filesCount; i++) {
                        fileIds.add(dis.readInt());
                    }

                    String clientIp = socket.getInetAddress().getHostAddress();
                    ClientDescriptor cd = new ClientDescriptor(clientIp, port);
                    boolean result = requestExecutor.serverRequestUpdate(cd, fileIds);
                    dos.writeBoolean(result);
                    break;
                }
                case DESCRIBE_COMMAND_ID: {
                    int fileId = dis.readInt();

                    FileDescriptor fd = requestExecutor.describeFile(fileId);

                    if (fd == null) {
                        dos.writeInt(-1);
                        break;
                    }

                    dos.writeInt(fileId);
                    dos.writeUTF(fd.getName());
                    dos.writeLong(fd.size);
                }
                default:
                    break;
            }

            dos.flush();
        }
    }

    private static class PeerServerProtocolImpl implements PeerServerProtocol {

        @Override
        public void processCommand(DataInputStream dis, DataOutputStream dos,
                                   ClientRequestExecutor requestExecutor) throws IOException {
            byte request = dis.readByte();

            switch (request) {
                case STAT_COMMAND_ID: {
                    int fileId = dis.readInt();
                    List<Integer> l = requestExecutor.clientRequestStat(fileId);

                    dos.writeInt(l.size());
                    for (Integer id : l) {
                        dos.writeInt(id);
                    }
                    break;
                }
                case GET_COMMAND_ID: {
                    int fileId = dis.readInt();
                    int filePart = dis.readInt();

                    byte[] data = requestExecutor.clientRequestGet(fileId, filePart);
                    dos.writeInt(data.length);
                    dos.write(data);
                    break;
                }
                default: {
                    LOGGER.error("Unknown command " + Byte.toString(request));
                    break;
                }
            }

            dos.flush();
        }
    }

    private static class ClientToServerProtocolImpl implements ClientToServerProtocol {
        private final DataOutputStream dos;
        private final DataInputStream dis;

        public ClientToServerProtocolImpl(Socket socket) throws Exception {
            dos = new DataOutputStream(socket.getOutputStream());
            dis = new DataInputStream(socket.getInputStream());
        }

        @Override
        public List<FileDescriptor> serverRequestList() throws IOException {
            dos.writeByte(LIST_COMMAND_ID);
            dos.flush();

            int filesNumber = dis.readInt();

            List<FileDescriptor> result = new ArrayList<>(filesNumber);
            for (int i = 0; i < filesNumber; i++) {
                int id = dis.readInt();
                String name = dis.readUTF();
                long size = dis.readLong();

                result.add(new FileDescriptor(id, name, size));
            }

            return result;
        }

        @Override
        public int serverRequestUpload(String fileName, long fileSize, ClientDescriptor cd) throws IOException {
            dos.writeByte(UPLOAD_COMMAND_ID);
            dos.writeUTF(fileName);
            dos.writeLong(fileSize);
            dos.flush();

            return dis.readInt();
        }

        @Override
        public Set<ClientDescriptor> serverRequestSources(int fileId) throws IOException {
            dos.writeByte(SOURCES_COMMAND_ID);
            dos.writeInt(fileId);
            dos.flush();

            int clientsNumber = dis.readInt();
            if (clientsNumber == -1) {
                return null;
            }

            Set<ClientDescriptor> result = new HashSet<>(clientsNumber);
            for (int i = 0; i < clientsNumber; i++) {
                List<Integer> ip = new ArrayList<>(IpValidator.IP_BYTES);
                for (int j = 0; j < IpValidator.IP_BYTES; j++) {
                    ip.add((int) dis.readByte());
                }

                int clientPort = dis.readInt();

                try {
                    result.add(new ClientDescriptor(ip, clientPort));
                } catch (Exception e) {
                    LOGGER.error("", e);
                }
            }

            return result;
        }

        @Override
        public boolean serverRequestUpdate(ClientDescriptor client, Set<Integer> fileIds) throws IOException {
            dos.writeByte(UPDATE_COMMAND_ID);
            dos.writeInt(client.getPort());
            dos.writeInt(fileIds.size());
            for (int id : fileIds) {
                dos.writeInt(id);
            }

            dos.flush();

            return dis.readBoolean();
        }

        @Override
        public FileDescriptor describeFile(int fileId) throws IOException {
            dos.writeByte(DESCRIBE_COMMAND_ID);
            dos.writeInt(fileId);
            dos.flush();

            int id = dis.readInt();
            if (id == -1) {
                return null;
            }

            String name = dis.readUTF();
            long size = dis.readLong();

            return new FileDescriptor(id, name, size);
        }
    }

    private static class PeerClientProtocolImpl implements PeerClientProtocol {
        private final DataOutputStream dos;
        private final DataInputStream dis;

        private PeerClientProtocolImpl(Socket socket) throws IOException {
            dos = new DataOutputStream(socket.getOutputStream());
            dis = new DataInputStream(socket.getInputStream());
        }

        @Override
        public List<Integer> clientRequestStat(int fileId) throws IOException {
            dos.writeByte(STAT_COMMAND_ID);
            dos.writeInt(fileId);
            dos.flush();

            int partsNumber = dis.readInt();
            List<Integer> res = new ArrayList<>();
            for (int i = 0; i < partsNumber; i++) {
                res.add(dis.readInt());
            }
            return res;
        }

        @Override
        public byte[] clientRequestGet(int fileId, int partNum) throws IOException {
            dos.writeByte(GET_COMMAND_ID);
            dos.writeInt(fileId);
            dos.writeInt(partNum);
            dos.flush();

            int size = dis.readInt();
            byte[] res = new byte[size];

            while (size > 0) {
                int readSize = dis.read(res, res.length - size, size);
                size -= readSize;
            }
            return res;
        }
    }

}
