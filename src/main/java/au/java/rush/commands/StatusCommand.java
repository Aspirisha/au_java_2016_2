package au.java.rush.commands;

import au.java.rush.structures.Revision;
import au.java.rush.utils.IndexManager;
import difflib.PatchFailedException;
import net.sourceforge.argparse4j.inf.Namespace;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Created by andy on 10/2/16.
 */
public class StatusCommand extends AbstractCommand {
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BLACK = "\u001B[30m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_WHITE = "\u001B[37m";

    @Override
    public void execute(Namespace args) {
        IndexManager im = new IndexManager(repo);
        Map<String, Revision.ModificationWithRepsectToParentRevisionType> addedFiles;
        Map<String, Revision.ModificationWithRepsectToIndexType> modifiedFiles;
        List<String> untrackedFiles;
        try {
            addedFiles = im.getCurrentlyIndexedFiles();
            modifiedFiles = im.getCurrentlyModifiedFiles();
            untrackedFiles = im.getUntrackedFiles();
        } catch (IOException e) {
            e.printStackTrace();

            return;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return;
        } catch (PatchFailedException e) {
            e.printStackTrace();
            return;
        }

        for (Map.Entry<String, Revision.ModificationWithRepsectToParentRevisionType> e : addedFiles.entrySet()) {
            System.out.format("%s%s \t %s%s\n", ANSI_GREEN, e.getValue().toString(),
                    e.getKey(), ANSI_RESET);
        }

        for (Map.Entry<String, Revision.ModificationWithRepsectToIndexType> e
                : modifiedFiles.entrySet()) {
            System.out.format("%s%s \t %s%s\n", ANSI_RED, e.getValue().toString(),
                    e.getKey(), ANSI_RESET);
        }

        for (String s : untrackedFiles) {
            System.out.format("%sUNTRACKED \t %s%s\n", ANSI_RED,
                    s, ANSI_RESET);
        }

    }
}
