package au.java.rush.commands;

/**
 * Created by andy on 9/27/16.
 */
public abstract class AbstractCommand implements Subcommand {
    protected String repo = System.getProperty("user.dir");
}
