package cssort.common;

import java.io.DataInputStream;
import java.io.IOException;

/**
 * Created by andy on 2/17/17.
 */
public final class Util {
    public static byte[] readMessageWithSizePrepended(DataInputStream dis) throws IOException {
        int size = dis.readInt();
        byte[] buf = new byte[size];

        int readBytes = 0;
        do {
            readBytes += dis.read(buf, readBytes, buf.length - readBytes);
        } while (readBytes < buf.length);

        return buf;
    }
}
