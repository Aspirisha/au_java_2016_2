package au.java.rush.commands;

import au.java.rush.utils.BranchManager;
import difflib.PatchFailedException;
import net.sourceforge.argparse4j.inf.Namespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Created by andy on 9/25/16.
 */
public class MergeCommand extends AbstractCommand {
    private Logger logger = LoggerFactory.getLogger(MergeCommand.class);

    @Override
    public void execute(Namespace args) {
        String branchOrRevision = args.getString("branchOrRevision");
        BranchManager bm = new BranchManager(repo);

        try {
            logger.info(String.format("executing merge of current revision %s with revision %s",
                    bm.getCurrentBranch(), branchOrRevision));
        } catch (IOException e) {
            logger.error("",e);
        }

        try {
            bm.merge(branchOrRevision);
        } catch (IOException e) {
            System.out.println("Failed to read or write some revision data.");
            logger.error("",e);
        } catch (ClassNotFoundException | PatchFailedException e) {
            System.out.println("Internal rush error. Please see logs for details.");
            logger.error("",e);
        }
    }
}
