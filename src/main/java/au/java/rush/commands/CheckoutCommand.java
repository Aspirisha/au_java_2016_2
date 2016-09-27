package au.java.rush.commands;

import au.java.rush.utils.BranchManager;
import difflib.PatchFailedException;
import net.sourceforge.argparse4j.inf.Namespace;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Created by andy on 9/25/16.
 */
public class CheckoutCommand extends AbstractCommand {
    @Override
    public void execute(Namespace args) {
        String revision = args.getString("branchOrRevision");
        BranchManager bm = new BranchManager(repo);
        if (!Files.exists(Paths.get(bm.getInternalRoot()))) {
            System.out.println("Not a rush repository"); // TODO io should be separate
            return;
        }


        try {
            bm.checkout(revision);
            // TODO tell the user if he can lose something!!!!!
        } catch (FileNotFoundException e) {
            System.out.println("Neither revision or branch found with given name: " + revision);
        } catch (IOException e) {
            System.out.println("Couldn't read revision file");
        } catch (ClassNotFoundException e) {
            System.out.println("Internal rush error");
            e.printStackTrace();
        } catch (PatchFailedException e) {
            System.out.println("Error restoring revision");
        }
    }
}
