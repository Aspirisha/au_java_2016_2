import au.java.sftp.SftpProtocol;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * Created by andy on 10/16/16.
 */
public class ServerTest {
    @Test
    public void listTest() throws IOException {
        Path path = Paths.get(System.getProperty("user.dir"), "src", "test", "resources");

        ByteArrayOutputStream bos1 = new ByteArrayOutputStream();
        DataOutputStream dos1 = new DataOutputStream(bos1);
        dos1.writeInt(SftpProtocol.REQUEST_LIST);
        dos1.writeUTF(path.toString());

        ByteArrayInputStream bis1 = new ByteArrayInputStream(bos1.toByteArray());
        DataInputStream dis1 = new DataInputStream(bis1);

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        DataOutputStream dos2 = new DataOutputStream(os);

        SftpProtocol.SftpServerProtocol p = SftpProtocol.getServerProtocol();

        assertEquals(SftpProtocol.REQUEST_LIST, p.process(dis1, dos2));

        DataInputStream dis2 = new DataInputStream(
                new ByteArrayInputStream(os.toByteArray()));

        int size = dis2.readInt();
        Set<String> data = new HashSet<>();
        Set<String> expected = new HashSet<>(Arrays.asList("1.txt", "A",
                Paths.get("A", "1.txt").toString(),
                Paths.get("A", "2.txt").toString()));
        for (int i = 0; i < size; i++) {
            data.add(path.relativize(Paths.get(dis2.readUTF())).toString());
            dis2.readBoolean();
        }

        assertEquals(4, size);
        assertEquals(expected, data);
    }

    @Test
    public void getTest() throws IOException {
        Path path = Paths.get(System.getProperty("user.dir"), "src",
                "test", "resources", "A", "1.txt");
        SftpProtocol.SftpServerProtocol p = SftpProtocol.getServerProtocol();

        ByteArrayOutputStream bos1 = new ByteArrayOutputStream();
        DataOutputStream dos1 = new DataOutputStream(bos1);
        dos1.writeInt(SftpProtocol.REQUEST_GET);
        dos1.writeUTF(path.toString());
        ByteArrayInputStream bis1 = new ByteArrayInputStream(bos1.toByteArray());
        DataInputStream dis1 = new DataInputStream(bis1);

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        DataOutputStream dos2 = new DataOutputStream(os);

        assertEquals(SftpProtocol.REQUEST_GET, p.process(dis1, dos2));
        DataInputStream dis2 = new DataInputStream(
                new ByteArrayInputStream(os.toByteArray()));
        long size = dis2.readLong();

        assertEquals(size, path.toFile().length());
        byte[] data = new byte[(int) size];
        assertEquals(size, dis2.read(data));
        assertArrayEquals(data, FileUtils.readFileToByteArray(path.toFile()));
    }
}
