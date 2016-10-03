package au.java.rush.utils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by andy on 9/25/16.
 */
public class RepoManager {
    protected final String repoRoot;
    public RepoManager(String repoRoot) {
        this.repoRoot = repoRoot;
    }

    public class PathRelativeToRoot {
        private PathRelativeToRoot(Path p) {
            path = p;
        }

        private PathRelativeToRoot(String s) {
            path = Paths.get(s);
        }

        public final Path path;

        @Override
        public String toString() {
            return path.toString();
        }
    }

    public String getFilePathAbsolute(String fileName) {
        Path p = Paths.get(fileName);

        if (p.isAbsolute()) {
            return fileName.indexOf(repoRoot) == 0 ? p.toString() : null;
        }

        return getAbsolutePathFromRelativeToRepo(fileName);
    }

    public String getExistingFilePathAbsolute(String fileName) {
        String p = getFilePathAbsolute(fileName);

        return Files.exists(Paths.get(p)) ? p : null;
    }

    public String getFilePathRelativeToRoot(String fileName) {
        String absolutePath = getFilePathAbsolute(fileName);
        int prefixIndex = absolutePath.indexOf(repoRoot);

        if (prefixIndex != 0) {
            return null;
        }
        Path relative = Paths.get(repoRoot).relativize(Paths.get(absolutePath));
        return relative.toString();
    }

    private String findFileAbsolutePath(String toolName) {
        try {
            return Paths.get(toolName).toRealPath().toString();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private String getAbsolutePathFromRelativeToRepo(String toolName) {
        return Paths.get(repoRoot, toolName).toString();
    }

    protected List<String> readRepoFile(String fileName) throws IOException {
        final List<String> lines = new ArrayList<String>();
        String line;
        final BufferedReader in = new BufferedReader(new FileReader(fileName));
        while ((line = in.readLine()) != null) {
            lines.add(line);
        }

        return lines;
    }

    public void delete(File f) throws FileNotFoundException {
        if (f.isDirectory()) {
            for (File c : f.listFiles())
                delete(c);
        }
        if (!f.delete())
            throw new FileNotFoundException("Failed to delete file: " + f);
    }

    public final String getRepoRoot() {
        return repoRoot;
    }
    public String getInternalRoot() {
        return String.join(File.separator, repoRoot, ".rush");
    }
    public String getHeadFile() {
        return String.join(File.separator, getInternalRoot(), "head");
    }
    public String getRevisionsDir() {
        return String.join(File.separator, getInternalRoot(), "revisions");
    }
    public String getBranchesDir() {
        return String.join(File.separator, getInternalRoot(), "branches");
    }
    public String getIndexDir() {
        return String.join(File.separator, getInternalRoot(), "index");
    }
    public String getCommitPath(String revision) {
        return String.join(File.separator, getRevisionsDir(), revision, "commit_data");
    }

    public String getRevisionDir(String revision) {
        return String.join(File.separator, getRevisionsDir(), revision);
    }
    public String getRevisionFile(String revision) {
        return String.join(File.separator, getRevisionsDir(), revision, "revision");
    }

    public String getCurrentBranchFile() {
        return String.join(File.separator, getInternalRoot(), "current-branch");
    }
}
