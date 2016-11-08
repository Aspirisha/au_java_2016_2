package au.java.tracker.server;

import au.java.tracker.protocol.ClientDescriptor;
import au.java.tracker.protocol.FileDescriptor;
import au.java.tracker.protocol.ServerRequestExecutor;

import java.util.List;
import java.util.Set;

/**
 * Created by andy on 11/7/16.
 */
public class ServerRequestExecutorImpl implements ServerRequestExecutor {

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
