package au.java.sftp;

import javafx.util.Pair;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by andy on 10/12/16.
 */
public class SftpProtocol {
    public static final int REQUEST_LIST = 1;
    public static final int REQUEST_GET = 2;
    public static final int REQUEST_GREET = 3;
    public static final int REQUEST_BYE = 4;

    private SftpProtocol() {}

    public static SftpServerProtocol getServerProtocol() {
        return new SftpServerProtocolImpl();
    }

    public static SftpClientProtocol getClientProtocol() {
        return new SftpClientProtocolImpl();
    }

    public interface SftpServerProtocol {
        int process(DataInputStream dis, DataOutputStream dos) throws IOException;
        boolean isFinished();
    }

    public interface SftpClientProtocol {
        List<Pair<String, Boolean>> requestList(DataInputStream dis, DataOutputStream dos, String path) throws IOException;
        byte[] requestFile(DataInputStream dis, DataOutputStream dos, String path) throws IOException;

        void greet(DataOutputStream dos) throws IOException;
        void farewell(DataOutputStream dos) throws IOException;
    }

    private static class SftpClientProtocolImpl implements SftpClientProtocol {
        @Override
        public List<Pair<String, Boolean>> requestList(DataInputStream dis, DataOutputStream dos,
                                                       String path) throws IOException {
            dos.writeInt(REQUEST_LIST);
            dos.writeUTF(path);

            int size = dis.readInt();

            List<Pair<String, Boolean>> result = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                result.add(new Pair<>(dis.readUTF(), dis.readBoolean()));

            }
            return result;
        }

        @Override
        public byte[] requestFile(DataInputStream dis, DataOutputStream dos, String path) throws IOException {
            dos.writeInt(REQUEST_GET);
            dos.writeUTF(path);

            long size = dis.readLong();
            byte[] data = new byte[(int) size];
            int totalRead = 0;

            while (totalRead < size) {
                int bytesRead = dis.read(data, totalRead, (int) (size - totalRead));
                if (bytesRead < 0) {
                    throw new IOException("Data stream ended prematurely");
                }
                totalRead += bytesRead;
            }

            return data;
        }

        @Override
        public void greet(DataOutputStream dos) throws IOException {
            dos.writeInt(SftpProtocol.REQUEST_GREET);
        }

        @Override
        public void farewell(DataOutputStream dos) throws IOException {
            dos.writeInt(SftpProtocol.REQUEST_BYE);
        }
    }


    private static class SftpServerProtocolImpl implements SftpServerProtocol {
        private Logger logger = LoggerFactory.getLogger(SftpProtocol.class);
        private boolean finished = false;

        @Override
        public int process(DataInputStream dis, DataOutputStream dos) throws IOException {
            int request = dis.readInt();

            switch (request) {
                case REQUEST_LIST: {
                    String data = dis.readUTF();
                    logger.info("Requested listing in " + data);
                    Path p = Paths.get(data);
                    if (!p.toFile().isDirectory()) {
                        dos.writeInt(0);
                        break;
                    }

                    Collection<File> res = FileUtils.listFilesAndDirs(p.toFile(),
                           TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE);

                    logger.info("Listing size is " + res.size());
                    dos.writeInt(res.size() - 1);
                    res.stream().forEach(f -> {
                        try {
                            if (f.equals(p.toFile())) {
                               return;
                            }
                            dos.writeUTF(f.toString());
                            dos.writeBoolean(f.isDirectory());
                        } catch (IOException e) {
                            logger.error("", e);
                            e.printStackTrace();
                        }
                    });

                    break;
                }
                case REQUEST_GET: {
                    String file = dis.readUTF();
                    logger.info("Requested get file " + file);
                    File f = FileUtils.getFile(file);

                    if (!f.exists()) {
                        dos.writeLong(0);
                        break;
                    }
                    dos.writeLong(f.length());
                    dos.write(FileUtils.readFileToByteArray(f));
                    break;
                }
                case REQUEST_GREET: {
                    dos.writeUTF("Hello!");
                    break;
                }

                case REQUEST_BYE: {
                    dos.writeUTF("Bye!");
                    finished = true;
                    break;
                }
            }

            return request;
        }

        @Override
        public boolean isFinished() {
            return finished;
        }
    }


}
