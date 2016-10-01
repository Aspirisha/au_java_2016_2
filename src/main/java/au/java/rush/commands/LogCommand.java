package au.java.rush.commands;

import au.java.rush.utils.BranchManager;
import au.java.rush.structures.Revision;
import net.sourceforge.argparse4j.inf.Namespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Stack;

/**
 * Created by andy on 9/25/16.
 */
public class LogCommand extends AbstractCommand {
    private Logger logger = LoggerFactory.getLogger(LogCommand.class);

    @Override
    public void execute(Namespace args) {
        BranchManager bm = new BranchManager(repo);
        if (!Files.exists(Paths.get(bm.getInternalRoot()))) {
            System.out.println("Not a rush repository"); // TODO io should be separate
            return;
        }

        Stack<Revision> revisions = new Stack<>();
        Revision r = null;
        String branchOrRevision = args.getString("branchOrRevision");

        if (branchOrRevision == null) {
            try {
                r = bm.getHeadRevision();
            } catch (FileNotFoundException e) {
                System.out.println("Commit history is empty for current head");
            } catch (IOException e) {
                System.out.println("Failed ");
            } catch (ClassNotFoundException e) {
                System.out.println("Internal rush error. Error information is stored in...");
            }
        } else {
            try {
                r = bm.getRevisionByHashOrBranch(branchOrRevision);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

        if (r == null) {
            return;
        }
        revisions.add(r);
        while (!r.getParentHash().isEmpty()) {
            try {
                r = bm.getRevisionByHash(r.getParentHash());
            } catch (IOException e) {
                System.out.println("Error reading some parent revisions. " +
                        "History is limited to last uncorrupted revision.");
                break;
            } catch (ClassNotFoundException e) {
                System.out.println("Internal rush error. Error information is stored in...");
            }
            revisions.add(r);
        }

        try {
            if (!bm.isDetachedHead()) {
                System.out.format("On branch %s\n", bm.getCurrentBranch());
            } else {
                System.out.format("In a detached head mode. Head revision is %s\n",
                        bm.getHeadRevisionHash());
            }
        } catch (IOException e) {
            System.out.println("Couldn't read current branch information.");
            logger.error("", e);
        }
        revisions.stream().forEach(rev -> {
            System.out.format("%s\t %s\n", rev.getHash(), rev.getMessage());
        });

    }
}
