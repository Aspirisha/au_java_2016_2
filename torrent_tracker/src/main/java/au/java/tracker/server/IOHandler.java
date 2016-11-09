package au.java.tracker.server;

import au.java.tracker.protocol.ClientDescriptor;
import au.java.tracker.protocol.FileDescriptor;

import java.util.Collection;
import java.util.Set;

/**
 * Created by andy on 11/8/16.
 */
public interface IOHandler {
    String readCommand();

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

    default void onCouldntReadState() {
        System.out.println("Couldn't read server state file");
    }

    void listClients(Set<ClientDescriptor> aliveClients);
    void listFiles(Collection<FileDescriptor> files);
}
