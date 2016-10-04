package au.java.rush.commands;

/**
 * Created by andy on 9/25/16.
 */
public class AddInteractionHandler extends InteractionHandler {
    protected void onNoFileSpecified() {
        System.out.println("Nothing specified, nothing added.");
    }

    protected void onFileDoesntExist(String fileName) {
        System.out.println(String.format("pathspec '%s' did not match any files\n", fileName));
    }

    protected void onTryingToAddRushDirectory() {
        System.out.println("Can't add .rush directory to index");
    }
}
