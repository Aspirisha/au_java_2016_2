package au.java.rush.commands;

import au.java.rush.Rush;
import au.java.rush.utils.BranchManager;
import au.java.rush.utils.IndexManager;
import difflib.DiffUtils;
import difflib.Patch;
import difflib.PatchFailedException;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static difflib.DiffUtils.generateUnifiedDiff;
import static javafx.scene.input.KeyCode.L;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

/**
 * Created by andy on 10/1/16.
 */
public class TestCommands {
    private Path repoRoot = Paths.get("build", "test", "testing_repo");
    private Path rushRoot = repoRoot.resolve(".rush");

    @Before
    public void initializeRepo() {
        System.setProperty("user.dir", String.valueOf(repoRoot.toAbsolutePath()));
        repoRoot.toAbsolutePath().toFile().mkdirs();

        try {
            FileUtils.deleteDirectory(rushRoot.toFile());
            FileUtils.cleanDirectory(repoRoot.toFile());
        } catch (IOException e) {

        }
        Rush.main(new String[]{"init"});
    }

    @Test
    public void emptyRevisisonsBranch() throws IOException {
        Rush.main(new String[]{"branch", "feature"});
        BranchManager bm = new BranchManager(repoRoot.toString());

        assertTrue(bm.branchExists("feature"));
        assertTrue(bm.getCurrentBranch().equals("master"));
        Rush.main(new String[]{"checkout", "feature"});
        assertTrue(bm.getCurrentBranch().equals("feature"));
    }

    @Test
    public void singleCommitBranch() throws IOException {
        File file1 = repoRoot.resolve("file1.txt").toFile();

        final String s = "some text";
        FileUtils.write(file1, s);

        Rush.main(new String[]{"add", "file1.txt"});
        Rush.main(new String[]{"commit", "-m", "message1"});
        Rush.main(new String[]{"branch", "feature"});

        BranchManager bm = new BranchManager(repoRoot.toString());
        assertTrue(bm.branchExists("feature"));
        assertTrue(bm.getCurrentBranch().equals("master"));
        Rush.main(new String[]{"checkout", "feature"});
        assertTrue(bm.getCurrentBranch().equals("feature"));
        assertTrue(FileUtils.readFileToString(file1).equals(s));
    }

    @Test
    public void multiCommitBranch() throws IOException {
        File file1 = repoRoot.resolve("file1.txt").toFile();

        final String s = "some text";
        final String s2 = "some more text!\n";
        FileUtils.write(file1, s);

        Rush.main(new String[]{"add", "file1.txt"});
        Rush.main(new String[]{"commit", "-m", "message1"});
        Rush.main(new String[]{"branch", "feature"});
        Rush.main(new String[]{"checkout", "feature"});
        FileUtils.write(file1, s2, true);
        Rush.main(new String[]{"add", "file1.txt"});
        Rush.main(new String[]{"commit", "-m", "message2"});

        assertTrue(FileUtils.readFileToString(file1).equals(s + s2));

        Rush.main(new String[]{"checkout", "master"});
        assertTrue(FileUtils.readFileToString(file1).equals(s));

        Rush.main(new String[]{"checkout", "feature"});
        assertTrue(FileUtils.readFileToString(file1).equals(s + s2));
    }

    @Test
    public void statusTest() throws IOException, ClassNotFoundException, PatchFailedException {
        File file1 = repoRoot.resolve("file1.txt").toFile();
        final String s = "some text";
        final String s2 = "some more text!\n";
        FileUtils.write(file1, s);

        IndexManager im = new IndexManager(repoRoot.toString());
        assertTrue(im.getCurrentlyIndexedFiles().isEmpty());
        assertTrue(im.getCurrentlyModifiedFiles().isEmpty());
        assertEquals(1, im.getUntrackedFiles().size());
        assertEquals("file1.txt", im.getUntrackedFiles().get(0));

        Rush.main(new String[]{"add", "file1.txt"});
        FileUtils.write(file1, s2, true);

        assertEquals(1, im.getCurrentlyIndexedFiles().size());
        assertEquals(1, im.getCurrentlyModifiedFiles().size());
        assertTrue(im.getUntrackedFiles().isEmpty());


    }
}
