package au.java.rush.commands;

import au.java.rush.utils.BranchManager;
import au.java.rush.utils.Revision;
import net.sourceforge.argparse4j.inf.Namespace;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Stack;

/**
 * Created by andy on 9/25/16.
 */
public class LogCommand extends AbstractCommand {
    @Override
    public void execute(Namespace args) {
        BranchManager bm = new BranchManager(repo);
        if (!Files.exists(Paths.get(bm.getInternalRoot()))) {
            System.out.println("Not a rush repository"); // TODO io should be separate
            return;
        }

        Stack<Revision> revisions = new Stack<>();
        Revision r = bm.getBranchHeadRevision(bm.getCurrentBranch());
        if (r == null) {
            return;
        }
        revisions.add(r);
        while (r.getParentHash() != null) {
            try {
                r = bm.getRevision(r.getParentHash());
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            revisions.add(r);
        }

        revisions.stream().forEach(rev -> {
            System.out.format("%s\t %s\n", rev.getHash(), rev.getMessage());
        });

    }
}
