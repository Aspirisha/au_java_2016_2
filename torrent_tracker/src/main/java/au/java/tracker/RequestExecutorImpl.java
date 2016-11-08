package au.java.tracker;

import java.util.List;

/**
 * Created by andy on 11/7/16.
 */
public class RequestExecutorImpl implements ServerRequestExecutor {

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
