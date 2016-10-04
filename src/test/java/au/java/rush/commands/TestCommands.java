package au.java.rush.commands;

import au.java.rush.Rush;
import au.java.rush.utils.BranchManager;
import au.java.rush.utils.IndexManager;
import difflib.PatchFailedException;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;

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

    @Test
    public void deleteTest() throws IOException, ClassNotFoundException, PatchFailedException {
        File file1 = repoRoot.resolve("file1.txt").toFile();
        File file2 = repoRoot.resolve("subdir").resolve("file2.txt").toFile();
        File file3 = repoRoot.resolve("subdir").resolve("file3.txt").toFile();
        final String s = "some text";
        FileUtils.write(file1, s);

        IndexManager im = new IndexManager(repoRoot.toString());
        Rush.main(new String[]{"add", "file1.txt"});
        FileUtils.deleteQuietly(file1);
        assertEquals(1, im.getCurrentlyIndexedFiles().size());
        assertEquals(1, im.getCurrentlyModifiedFiles().size());
        assertTrue(im.getUntrackedFiles().isEmpty());

        Rush.main(new String[]{"add", "file1.txt"});
        // file was not in repo before so no info about it should now be saved
        assertTrue(im.getCurrentlyIndexedFiles().isEmpty());
        assertTrue(im.getCurrentlyModifiedFiles().isEmpty());
        assertTrue(im.getUntrackedFiles().isEmpty());

        FileUtils.write(file1, s);
        FileUtils.write(file2, s);
        FileUtils.write(file3, s);
        Rush.main(new String[]{"add", "file1.txt"});
        Rush.main(new String[]{"add", "subdir"});
        Rush.main(new String[]{"commit", "-m", "message1"});

        im.getUntrackedFiles().stream().forEach(System.out::println);
        assertTrue(im.getUntrackedFiles().isEmpty());
        FileUtils.deleteQuietly(repoRoot.resolve("subdir").toFile());
        assertEquals(2, im.getCurrentlyModifiedFiles().size());
    }

    @Test
    public void cleanTest() throws IOException, ClassNotFoundException {
        File file1 = repoRoot.resolve("file1.txt").toFile();

        Path file2path = repoRoot.resolve("subdir").resolve("file2.txt");
        Path file3path = repoRoot.resolve("subdir").resolve("file3.txt");

        File file2 = file2path.toFile();
        File file3 = file3path.toFile();
        final String s = "some text";
        FileUtils.write(file1, s);
        FileUtils.write(file2, s);
        FileUtils.write(file3, s);
        Rush.main(new String[]{"add", repoRoot.relativize(file2path).toString()});

        IndexManager im = new IndexManager(repoRoot.toString());
        assertEquals(2, im.getUntrackedFiles().size());
        assertEquals(1, im.getCurrentlyIndexedFiles().size());
        assertTrue(im.getCurrentlyIndexedFiles()
                .containsKey(repoRoot.relativize(file2path).toString()));

        Rush.main(new String[]{"clean"});
        assertTrue(im.getUntrackedFiles().isEmpty());
    }

    @Test
    public void resetTest() throws IOException, ClassNotFoundException, PatchFailedException {
        File file1 = repoRoot.resolve("file1.txt").toFile();

        Path file2path = repoRoot.resolve("subdir").resolve("file2.txt");
        Path file3path = repoRoot.resolve("subdir").resolve("file3.txt");

        File file2 = file2path.toFile();
        File file3 = file3path.toFile();
        final String s = "some text";
        FileUtils.write(file1, s);
        FileUtils.write(file2, s);
        FileUtils.write(file3, s);

        IndexManager im = new IndexManager(repoRoot.toString());
        Rush.main(new String[]{"add", "subdir"});
        assertEquals(1, im.getUntrackedFiles().size());
        Rush.main(new String[]{"reset", "subdir"});
        assertEquals(3, im.getUntrackedFiles().size());

        Rush.main(new String[]{"add", "subdir"});
        Rush.main(new String[]{"add", "file1.txt"});
        Rush.main(new String[]{"commit", "-m", "message"});

        FileUtils.deleteQuietly(file1);
        FileUtils.deleteQuietly(file2path.toFile());
        Rush.main(new String[]{"add", "file1.txt"});
        Rush.main(new String[]{"add", "subdir"});
        Rush.main(new String[]{"reset", "subdir"});
        String hash = im.commit("message");
        BranchManager bm = new BranchManager(repoRoot.toString());
        bm.checkout(hash);

        assertTrue(im.getDeletedFiles().contains("file1.txt"));
        assertFalse(file1.exists());
        assertTrue(file2.exists());
        assertFalse(im.getDeletedFiles().contains(repoRoot.relativize(file2path).toString()));
    }
}
