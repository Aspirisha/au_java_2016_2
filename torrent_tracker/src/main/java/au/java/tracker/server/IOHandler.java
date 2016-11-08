package au.java.tracker.server;

import com.google.common.base.Strings;

/**
 * Created by andy on 11/8/16.
 */
public interface IOHandler {
    String readCommand();

    default void onHelpRequested() {
        System.out.format("/%s\\\n", Strings.repeat("-", 78));
        System.out.println("  Commands:");
        System.out.println("\t list-clients : list connected clients");
        System.out.println("\t list-files : list uploaded files");
        System.out.println("\t exit : stop server and exit");

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
}
