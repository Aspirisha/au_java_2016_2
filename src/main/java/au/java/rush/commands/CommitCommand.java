package au.java.rush.commands;

import au.java.rush.utils.IndexManager;
import net.sourceforge.argparse4j.inf.Namespace;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Created by andy on 9/25/16.
 */
public class CommitCommand implements Subcommand {
    CommitInteractionHandler iohandler = new CommitInteractionHandler();
    @Override
    public void execute(Namespace args) {
        String repo = System.getProperty("user.dir");
        IndexManager im = new IndexManager(repo);
        if (!Files.exists(Paths.get(im.getInternalRoot()))) {
            iohandler.onRepositoryNotInitialized();
            return;
        }
        String message = args.getString("m");

        try {
            im.commit(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
