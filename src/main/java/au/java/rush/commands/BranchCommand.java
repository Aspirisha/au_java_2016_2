package au.java.rush.commands;

import net.sourceforge.argparse4j.inf.Namespace;

/**
 * Created by andy on 9/25/16.
 */
public class BranchCommand extends AbstractCommand {
    @Override
    public void execute(Namespace args) {
        if (args.getBoolean("d")) {
            System.out.print("delete");
        }
    }
}
