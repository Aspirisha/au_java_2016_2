package au.java.tracker.client;

import com.google.common.base.Strings;

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
    default void onHelpRequested() {
        System.out.format("/%s\\\n", Strings.repeat("-", 78));
        System.out.println("  Available commands:");
        System.out.println("\tupload <file_path> : upload file");
        System.out.println("\tdownload <file id> <output path>: download file with given id");
        System.out.println("\tlist : list files uploaded to tracker");
        System.out.println("\thelp : show this help");
        System.out.println("\texit : exit program");
        System.out.format("\\%s/\n", Strings.repeat("-", 78));
    }
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
}
