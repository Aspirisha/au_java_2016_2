package au.java.tracker.protocol;

import java.util.List;
import java.util.Set;

/**
 * Created by andy on 11/7/16.
 */
public interface ServerRequestExecutor {
    List<FileDescriptor> serverRequestList();

    /**
     * Uploads file to the tracker
     * @param fileName name of file to upload
     * @param fileSize size of file to upload
     * @return unique id for uploaded file
     */
    int serverRequestUpload(String fileName, long fileSize);
    List<ClientDescriptor> serverRequestSources(int fileId);
    boolean serverRequestUpdate(ClientDescriptor client, Set<Integer> fileIds);
    FileDescriptor describeFile(int fileId);
}
