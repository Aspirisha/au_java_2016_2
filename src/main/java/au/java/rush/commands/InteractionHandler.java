package au.java.rush.commands;

/**
 * Created by andy on 9/25/16.
 */
public class InteractionHandler {
    protected void onRepositoryNotInitialized() {
        System.out.println("Not a rush repository");
    }
    protected void onInternalRushError() {
        System.out.println("Internal rush error. See logs for details.");
    }

}
