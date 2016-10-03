package au.java.rush.commands;

import au.java.rush.utils.IndexManager;
import net.sourceforge.argparse4j.inf.Namespace;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Created by andy on 9/25/16.
 */
public class CommitCommand extends AbstractCommand {
    CommitInteractionHandler iohandler = new CommitInteractionHandler();
    Logger logger = LoggerFactory.getLogger(CommitCommand.class);

    @Override
    public void execute(Namespace args) {
        logger.debug("COMMIT command");
        IndexManager im = new IndexManager(repo);
        if (!Files.exists(Paths.get(im.getInternalRoot()))) {
            iohandler.onRepositoryNotInitialized();
            return;
        }
        String message = args.getString("m");

        try {
            String hash = im.commit(message);
            if (!hash.isEmpty()) {
                System.out.format("Committed revision %s \t %s\n", hash, message);
            } else {
                System.out.println("Nothing to commit. First add some files with add command.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }


    }
}
