package au.java.rush.commands;

import au.java.rush.utils.IndexManager;
import net.sourceforge.argparse4j.inf.Namespace;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Created by andy on 9/25/16.
 */
public class CommitCommand extends AbstractCommand {
    CommitInteractionHandler iohandler = new CommitInteractionHandler();
    @Override
    public void execute(Namespace args) {
        IndexManager im = new IndexManager(repo);
        if (!Files.exists(Paths.get(im.getInternalRoot()))) {
            iohandler.onRepositoryNotInitialized();
            return;
        }
        String message = args.getString("m");

        try {
            String hash = im.commit(message);


        } catch (IOException e) {
            e.printStackTrace();
        }


    }
}
