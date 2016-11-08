package au.java.tracker;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * Created by andy on 11/7/16.
 */
public class TrackerProtocol {
    // tracker requests
    private static final byte LIST_COMMAND_ID = 1;
    private static final byte UPLOAD_COMMAND_ID = 2;
    private static final byte SOURCES_COMMAND_ID = 3;
    private static final byte UPDATE_COMMAND_ID = 4;


    static ServerProtocol getServerProtocol() {
        return new ServerProtocolImpl();
    }

    public interface ServerProtocol {
        void processCommand(DataInputStream dis, DataOutputStream dos,
                            ServerRequestExecutor requestExecutor) throws IOException;
    }

    public interface ClientProtocol extends ServerRequestExecutor, ClientRequestExecutor {
        void processCommand(DataInputStream dis, DataOutputStream dos,
                            ClientRequestExecutor requestExecutor) throws IOException;
    }

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


    public class ClientProtocolImpl implements ClientProtocol {

        @Override
        public void processCommand(DataInputStream dis, DataOutputStream dos,
                                   ClientRequestExecutor requestExecutor) throws IOException {

        }

        @Override
        public List<Integer> clientRequestStat(int fileId) {
            return null;
        }

        @Override
        public byte[] clientRequestGet(int fileId, int partNum) {
            return new byte[0];
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
        public boolean serverRequestUpdate(ClientDescriptor client, List<Integer> fileIds) {
            return false;
        }
    }

}
