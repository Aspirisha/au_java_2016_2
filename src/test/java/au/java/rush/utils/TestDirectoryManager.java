package au.java.rush.utils;

import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.*;

/**
 * Created by andy on 9/25/16.
 */
public class TestDirectoryManager {
    @Test
    public void simpleTest() throws IOException {
        Path repoDir = Paths.get(System.getProperty("user.dir"), "temp");
        Path relativePath = Paths.get("aaa", "bbb");
        Path dirPath = repoDir.resolve(relativePath);

        RepoManager dm = new RepoManager(repoDir.toString());
        String result = dm.getExistingFilePathAbsolute(relativePath.toString());
        assertNull(result);
        Files.createDirectories(dirPath);


        result = dm.getExistingFilePathAbsolute(relativePath.toString());
        System.out.println(dirPath.toString());
        assertNotNull(result);
        assertEquals(dirPath.toString(), result);

        try {
            dm.delete(new File(repoDir.toString()));
        } catch (FileNotFoundException e) {
            fail("Couldn't delete created directories");
        }

    }
}
