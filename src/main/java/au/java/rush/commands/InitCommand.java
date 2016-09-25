package au.java.rush.commands;

import net.sourceforge.argparse4j.inf.Namespace;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by andy on 9/25/16.
 */
public class InitCommand implements Subcommand {
    @Override
    public void execute(Namespace args) {
        String currentDir = System.getProperty("user.dir");
        Path root = Paths.get(currentDir, ".rush");
        Path branches = Paths.get(root.toString(), "branches");
        Path head = Paths.get(root.toString(), "head");

        File headFile = new File(head.toString());
        try {
            Files.createDirectories(branches);
            headFile.createNewFile();
        } catch (IOException e) {
            System.out.println("Failed to initialize repository: unable to create .rush directory.");
            return;
        }


    }
}
