package au.java.tracker;

import java.io.OutputStream;
import java.util.List;

/**
 * Created by andy on 11/7/16.
 */
public interface ClientRequestExecutor {
    /**
     * @param fileId id of file requested for stat
     * @return parts numbers that this client has
     */
    List<Integer> clientRequestStat(int fileId);

    byte[] clientRequestGet(int fileId, int partNum);
}
