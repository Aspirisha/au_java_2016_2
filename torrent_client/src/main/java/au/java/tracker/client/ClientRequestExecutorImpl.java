package au.java.tracker.client;

import au.java.tracker.protocol.ClientRequestExecutor;

import java.util.List;

/**
 * Created by andy on 11/7/16.
 */
public class ClientRequestExecutorImpl implements ClientRequestExecutor {
    @Override
    public List<Integer> clientRequestStat(int fileId) {
        return null;
    }

    @Override
    public byte[] clientRequestGet(int fileId, int partNum) {
        return new byte[0];
    }
}
