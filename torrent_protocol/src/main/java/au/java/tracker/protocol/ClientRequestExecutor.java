package au.java.tracker.protocol;

import java.io.IOException;
import java.util.List;

/**
 * Created by andy on 11/7/16.
 */
public interface ClientRequestExecutor {
    /**
     * @param fileId id of file requested for stat
     * @return parts numbers that this client has
     */
    List<Integer> clientRequestStat(int fileId) throws IOException;

    byte[] clientRequestGet(int fileId, int partNum) throws IOException;
}
