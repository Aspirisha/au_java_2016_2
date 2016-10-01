package au.java.rush.commands;

import au.java.rush.utils.BranchManager;
import net.sourceforge.argparse4j.inf.Namespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Created by andy on 9/25/16.
 */
public class BranchCommand extends AbstractCommand {
    private Logger logger = LoggerFactory.getLogger(BranchCommand.class);
    @Override
    public void execute(Namespace args) {
        BranchManager bm = new BranchManager(repo);
        String branchName = args.getString("branchName");
        if (!args.getBoolean("d")) {
            try {
                BranchManager.BranchCreationResult result =
                        bm.createBranch(branchName);

                switch (result) {
                    case SUCCESS:
                        System.out.format("Created new branch %s\n", branchName);
                        break;
                    case ERROR_CREATING_BRANCH:
                        System.out.println("Couldn't create new branch.");
                        break;
                    case BRANCH_ALREADY_EXISTS:
                        System.out.format("Branch with name %s already exists.\n", branchName);
                        break;
                }
            } catch (IOException e) {
                System.out.println("Couldn't create new branch.");
                logger.error("", e);
            }
        } else {
            try {
                BranchManager.BranchDeletionResult result = bm.deleteBranch(branchName);
                switch (result) {
                    case SUCCESS:
                        System.out.format("Deleted branch %s\n", branchName);
                        break;
                    case BRANCH_DOESNT_EXIST:
                        System.out.format("Branch with name %s doesn't exist.\n", branchName);
                        break;
                }
                if (bm.isDetachedHead()) {
                    System.out.println("You are now in a detached head state (no current branch)." +
                            "You are still able to commit and do all the shit you want (it's not git");
                }
            } catch (IOException e) {
                logger.error("exception deleting branch", e);
                System.out.println("Couldn't delete branch.");
            }
        }
    }
}
