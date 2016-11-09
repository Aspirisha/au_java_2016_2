package au.java.tracker.client;

import au.java.tracker.protocol.FileDescriptor;
import com.google.common.base.Strings;

import java.util.List;

/**
 * Created by andy on 11/7/16.
 */
public interface IOHandler {
    String readCommand();

    default void onFileToDownloadNotSpecified() {
        System.out.println("File to download not specified");
    }
    default void onFileIdExpected() {
        System.out.println("Expected file id (integer)");
    }
    default void onInvalidServerIp(String ip) {
        System.out.format("%s is not a valid server ip\n", ip);
    }
    default void onFileNotTracked(int fileId) {
        System.out.format("File with id %d isn't tracked\n", fileId);
    }
    default void serverIpIsRequired() {
        System.out.println("Server ip was not specified and was not found in cache");
    }
    void onHelpRequested();
    default void onUnknownCommand(String command) {
        System.out.format("Unknown command: %s\n\n", command);
    }
    default void onFinishingJobs() {
        System.out.println("Stopping unfinished jobs, this may take some time...");
    }
    default void onCantCloseClientSocket() {
        System.out.println("Couldn't close client socket");
    }
    default void onCantCreateConfigFile() {
        System.out.println("Can't create config file");
    }
    default void onCorruptedConfig() {
        System.out.println("Config file is corrupted. Removing it...");
    }
    default void onCantObtainIp() {
        System.out.println("Couldn't obtain ip address");
    }
    default void onFileToUploadNotSpecified() {
        System.out.println("File to upload not specified");
    }
    default void onUnexistentUpload(String file) {
        System.out.format("Can't upload unexistent file %s\n", file);
    }
    default void onCantConnectToServer() {
        System.out.println("Can't connect to server");
    }
    default void onErrorListeningPort(int port) {
        System.out.format("Error listening port %d\n", port);
    }
    default void onErrorUpdating() {
        System.out.println("Error sending update request to server");
    }
    default void onUploadFailed(String fileName) {
        System.out.println("Failed to upload file " + fileName);
    }

    default void onListFailed() {
        System.out.println("Failed to list files");
    }

    void showFileList(List<FileDescriptor> l);

    void onSuccessfulUpload(DownloadingFileDescriptor id);

    void onFileDownloaded(DownloadingFileDescriptor fd, FileDownloadResult result);

    default void onOutputPathExpected() {
        System.out.println("Output path not provided");
    }

    default void onCantUploadDirectories() {
        System.out.println("Directory uploads are not supported");
    }

    void fileMovingNotSupported(DownloadingFileDescriptor fd);
}
