package au.java.tracker.protocol;

import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * Created by andy on 11/7/16.
 */
public interface ServerRequestExecutor {
    List<FileDescriptor> serverRequestList() throws IOException;

    /**
     * Uploads file to the tracker
     * @param fileName name of file to upload
     * @param fileSize size of file to upload
     * @return unique id for uploaded file
     */
    int serverRequestUpload(String fileName, long fileSize,
                            ClientDescriptor uploader) throws IOException;
    Set<ClientDescriptor> serverRequestSources(int fileId) throws IOException;
    boolean serverRequestUpdate(ClientDescriptor client, Set<Integer> fileIds) throws IOException;
    FileDescriptor describeFile(int fileId) throws IOException;
}
