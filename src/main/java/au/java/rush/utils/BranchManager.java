package au.java.rush.utils;

import difflib.PatchFailedException;
import javafx.util.Pair;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by andy on 9/25/16.
 */
public class BranchManager extends RepoManager {
    private final String branchPath;
    public BranchManager(String repoPath) {
        super(repoPath);
        branchPath = Paths.get(repoPath, ".rush", "branches").toString();
    }

    public String getCurrentBranch() throws IOException {
        return FileUtils.readFileToString(FileUtils.getFile(getCurrentBranchFile()));
    }

    public String getBranchPath(String branch) {
        return String.join(File.separator, branchPath, branch);
    }

    public String getCurrentBranchPath() throws IOException {
        String curBranch = getCurrentBranch();
        return getBranchPath(curBranch);
    }

    public Revision getHeadRevision() throws IOException, ClassNotFoundException {
        return getRevisionByHash(getHeadRevisionHash());
    }

    public Revision getCurrentBranchHeadRevision() throws IOException, ClassNotFoundException {
        return getBranchHeadRevision(getCurrentBranch());
    }

    public boolean hasHeadRevision() throws IOException {
        return !getHeadRevisionHash().isEmpty();
    }

    /**
     *
     * @return hash of current head revision
     */
    public String getHeadRevisionHash() throws IOException {
        String branchConfig = String.join(File.separator, getInternalRoot(), "head");
        return FileUtils.readFileToString(FileUtils.getFile(branchConfig));
    }

    public String getBranchHeadRevisionHash(String branchName) throws IOException {
        String branchConfig = String.join(File.separator, branchPath, branchName, "head");
        return FileUtils.readFileToString(FileUtils.getFile(branchConfig));
    }

    public Revision getBranchHeadRevision(String branchName) throws IOException, ClassNotFoundException {
        String rev = getBranchHeadRevisionHash(branchName);
        String revPath = getRevisionFile(rev);
        return (Revision) Serializer.deserialize(revPath);
    }

    public Path getFile(Path filePath) throws IOException {
        Path branchPath = Paths.get(getCurrentBranchPath());
        Path fileInBranch = branchPath.resolve(filePath);

        if (!Files.exists(fileInBranch))
            return null;
        return fileInBranch;
    }

    private void setCurrentBranch(String branch) {
        try {
            FileUtils.write(new File(getCurrentBranchFile()), branch);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean branchExists(String branchName) {
        return Paths.get(getBranchPath(branchName)).toFile().exists();
    }

    /**
     * Get revision path in file system by either revision hash or branch name
     * @param branchOrRevision name of either branch or revision to get path for
     * @return pair, where first element is path to revision, and second is true if the passed
     *         name refers to branch, not a revision hash
     * @throws IOException in case
     * @throws ClassNotFoundException
     */
    private Pair<Path, Boolean> getRevisionPathByBranchOrRevisionName(String branchOrRevision)
            throws IOException {
        if (branchExists(branchOrRevision)) {
            return new Pair<>(Paths.get(getRevisionsDir(),
                    String.valueOf(getBranchHeadRevisionHash(branchOrRevision))), true);
        }

        Path rev = Paths.get(getRevisionFile(branchOrRevision));
        if (!Files.exists(rev)) {
            throw new FileNotFoundException();
        }
        return new Pair<>(rev, false);
    }

    public void checkout(String branchOrRevision) throws IOException, ClassNotFoundException, PatchFailedException {
        Pair<Path, Boolean> revisionPath =
                getRevisionPathByBranchOrRevisionName(branchOrRevision);

        Revision r = (Revision) Serializer.deserialize(revisionPath.getKey().toString());
        r.checkout();

        if (revisionPath.getValue()) {
            setCurrentBranch(branchOrRevision);
        }
    }

    public Revision getRevisionByHash(String hash) throws IOException, ClassNotFoundException {
        Path revisionPath = Paths.get(getRevisionFile(hash));
        return (Revision) Serializer.deserialize(revisionPath.toString());
    }

    public Revision getRevisionByHashOrBranch(String branchOrRevision) throws IOException, ClassNotFoundException {
        Pair<Path, Boolean> revisionPath =
                getRevisionPathByBranchOrRevisionName(branchOrRevision);
        return (Revision) Serializer.deserialize(revisionPath.getKey().toString());
    }

    void updateHeads(String hash) throws IOException {
        FileUtils.write(Paths.get(getCurrentBranchPath(), "head").toFile(), hash);
        FileUtils.write(Paths.get(getHeadFile()).toFile(), hash);
    }
}
