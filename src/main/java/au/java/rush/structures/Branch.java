package au.java.rush.structures;

import au.java.rush.utils.RepoManager;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.nio.file.Paths;

/**
 * Created by andy on 10/1/16.
 */
public class Branch {
    private String repoRoot;
    private String name;
    private String headRevision;

    private Branch(String name, String repoRoot) {
        this.name = name;
        this.repoRoot = repoRoot;
    }

    public String getName() {
        return name;
    }

    public String getHeadRevision() throws IOException {
        return FileUtils.readFileToString(Paths.get(repoRoot, "branches", name, "head").toFile());
    }

    public static Branch getExistingBranch(String name, RepoManager rm) {
        return null;
    }
}
