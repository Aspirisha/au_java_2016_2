package au.java.rush.commands;

import au.java.rush.utils.IndexManager;
import net.sourceforge.argparse4j.inf.Namespace;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

/**
 * Created by andy on 10/4/16.
 */
public class CleanCommand extends AbstractCommand {
    private Logger logger = LoggerFactory.getLogger(CleanCommand.class);

    @Override
    public void execute(Namespace args) {
        IndexManager im = new IndexManager(repo);
        try {
            List<String> untrackedFiles = im.getUntrackedFiles();
            untrackedFiles.stream()
                    .forEach(fileName -> FileUtils.deleteQuietly(
                    Paths.get(repo).resolve(fileName).toFile()));
        } catch (IOException e) {
            System.out.println("Error reading rush files.");
            logger.error("", e);
        } catch (ClassNotFoundException e) {
            System.out.println("Internal rush error. See logs for details.");
            logger.error("", e);
        }
        System.out.println("rush: cleaned working directory.");
    }
}
