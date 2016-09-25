package au.java.rush.commands;

import net.sourceforge.argparse4j.inf.Namespace;

/**
 * Created by andy on 9/25/16.
 */
public interface Subcommand {
    void execute(Namespace args);
}
