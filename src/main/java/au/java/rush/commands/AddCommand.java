package au.java.rush.commands;

import au.java.rush.utils.IndexManager;
import difflib.Patch;
import net.sourceforge.argparse4j.inf.Namespace;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by andy on 9/25/16.
 */
public class AddCommand implements Subcommand {
    private AddInteractionHandler iohandler = new AddInteractionHandler();

    @Override
    public void execute(Namespace args) {
        String repo = System.getProperty("user.dir");
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

        try {
            im.createPatch(fileOrDirToAdd);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
