package au.java.rush.commands;

import au.java.rush.utils.IndexManager;
import net.sourceforge.argparse4j.inf.Namespace;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;


/**
 * Created by andy on 9/25/16.
 */
public class AddCommand extends AbstractCommand {
    private AddInteractionHandler iohandler = new AddInteractionHandler();

    @Override
    public void execute(Namespace args) {
        IndexManager im = new IndexManager(repo);
        if (!Files.exists(Paths.get(im.getInternalRoot()))) {
            iohandler.onRepositoryNotInitialized();
            return;
        }
        String fileOrDirToAdd = args.getString("fileOrDirectory");

        if (fileOrDirToAdd == null) {
            iohandler.onNoFileSpecified();
            return;
        }

        if (Paths.get(fileOrDirToAdd).startsWith(".rush")) {
            System.out.println("Can't add .rush directory to index");
            return;
        }

        try {
            im.createPatch(fileOrDirToAdd);
        } catch (IOException e) {
            System.out.println("Failed to add file " + fileOrDirToAdd + " to index");
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

    }
}
