package au.java.rush.commands;

import au.java.rush.utils.RepoManager;
import net.sourceforge.argparse4j.inf.Namespace;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by andy on 9/25/16.
 */
public class InitCommand extends AbstractCommand {
    @Override
    public void execute(Namespace args) {
        Path root = Paths.get(repo, ".rush");

        if (Files.exists(root)) {
            System.out.println("Rush repository already initialized in " + root);
            return;
        }

        RepoManager rm = new RepoManager(repo);

        try {
            Files.createDirectories(Paths.get(rm.getBranchesDir(), "master"));
            Files.createDirectories(Paths.get(rm.getIndexDir()));
            Files.createDirectories(Paths.get(rm.getRevisionsDir()));
            Files.createFile(Paths.get(rm.getHeadFile()));
            FileUtils.write(new File(rm.getCurrentBranchFile()), "master");
        } catch (IOException e) {
            System.out.println("Failed to initialize repository: unable to create .rush directory.");
            return;
        }
        System.out.println("Initialized empty rush repository in " + root);

    }
}
