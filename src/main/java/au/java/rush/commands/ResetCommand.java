package au.java.rush.commands;

import au.java.rush.utils.IndexManager;
import net.sourceforge.argparse4j.inf.Namespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Created by andy on 10/4/16.
 */
public class ResetCommand extends AbstractCommand {
    Logger logger = LoggerFactory.getLogger(ResetCommand.class);
    InteractionHandler iohandler = new InteractionHandler();

    @Override
    public void execute(Namespace args) {
        logger.debug("RESET command");

        // TODO don't forget to restore files which were deleted and then added (i.e. which were rush rmed)
        IndexManager im = new IndexManager(repo);

        String fileToReset = args.getString("fileName");
        try {
            if (fileToReset != null) {
                im.resetFile(fileToReset);
            } else {
                im.reset();
            }
        } catch (ClassNotFoundException e) {
            logger.error("", e);
            iohandler.onInternalRushError();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
