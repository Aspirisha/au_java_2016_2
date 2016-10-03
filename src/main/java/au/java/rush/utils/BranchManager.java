package au.java.rush.utils;

import au.java.rush.structures.Revision;
import difflib.PatchFailedException;
import javafx.util.Pair;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;

/**
 * Created by andy on 9/25/16.
 */
public class BranchManager extends RepoManager {
    private Logger logger = LoggerFactory.getLogger(BranchManager.class);

    private final String branchPath;
    public BranchManager(String repoPath) {
        super(repoPath);
        branchPath = Paths.get(repoPath, ".rush", "branches").toString();
    }

    public String getCurrentBranch() throws IOException {
        return FileUtils.readFileToString(FileUtils.getFile(getCurrentBranchFile()));
    }

    /**
     *
     * @param branch name of branch
     * @return path to directory containing branch specific data
     */
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

    public boolean hasHeadRevision(String branchName) throws IOException {
        return !getBranchHeadRevisionHash(branchName).isEmpty();
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
                    String.valueOf(getBranchHeadRevisionHash(branchOrRevision)), "revision"), true);
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

        logger.info("revision path = " + revisionPath.getKey());
        FileUtils.cleanDirectory(FileUtils.getFile(getIndexDir()));
        try {
            FileUtils.copyFileToDirectory(revisionPath.getKey()
                            .resolve("deleted").toFile(),
                    FileUtils.getFile(getInternalRoot()));
        } catch (IOException e) {
            logger.error("", e);
        }

        try {
            FileUtils.copyFileToDirectory(revisionPath.getKey()
                            .resolve("line-endings").toFile(),
                    FileUtils.getFile(getInternalRoot()));
        } catch (IOException e) {
            logger.error("", e);
        }


        if (revisionPath.getValue() && !hasHeadRevision(branchOrRevision)) {
            setCurrentBranch(branchOrRevision);
            updateHeads("");
            return;
        }

        Revision r = (Revision) Serializer.deserialize(revisionPath.getKey().toString());
        r.checkout();

        if (revisionPath.getValue()) {
            setCurrentBranch(branchOrRevision);
        }
        updateHeads(r.getHash());
    }

    public Revision getRevisionByHash(String hash) throws IOException, ClassNotFoundException {
        Path revisionPath = Paths.get(getRevisionFile(hash));
        return (Revision) Serializer.deserialize(revisionPath.toString());
    }

    public Revision getRevisionByHashOrBranch(String branchOrRevision) throws IOException, ClassNotFoundException {
        Pair<Path, Boolean> revisionPath =
                getRevisionPathByBranchOrRevisionName(branchOrRevision);
        logger.debug("revision path = " + revisionPath.getKey());
        return (Revision) Serializer.deserialize(revisionPath.getKey().toString());
    }

    public String getBranchHeadFile(String branchPath) {
        return String.join(File.separator, branchPath, "head");
    }

    public enum BranchCreationResult {
        SUCCESS,
        ERROR_CREATING_BRANCH,
        BRANCH_ALREADY_EXISTS
    }

    public BranchCreationResult createBranch(String branchName) throws IOException {
        if (branchExists(branchName)) {
            return BranchCreationResult.BRANCH_ALREADY_EXISTS;
        }

        String branchPath = getBranchPath(branchName);
        if (!Paths.get(branchPath).toFile().mkdirs())
            return BranchCreationResult.ERROR_CREATING_BRANCH;

        FileUtils.copyFile(FileUtils.getFile(getHeadFile()),
                FileUtils.getFile(getBranchHeadFile(branchPath)));

        return BranchCreationResult.SUCCESS;
    }


    public enum BranchDeletionResult {
        SUCCESS,
        BRANCH_DOESNT_EXIST
    }

    public BranchDeletionResult deleteBranch(String branchName) throws IOException {
        if (!branchExists(branchName)) {
            return BranchDeletionResult.BRANCH_DOESNT_EXIST;
        }

        String branchPath = getBranchPath(branchName);

        FileUtils.deleteDirectory(FileUtils.getFile(branchPath));

        if (getCurrentBranch().equals(branchName)) {
            String s = getCurrentBranchFile();
            FileUtils.write(FileUtils.getFile(s), ""); // detached head state
        }

        return BranchDeletionResult.SUCCESS;
    }

    void updateHeads(String hash) throws IOException {
        FileUtils.write(Paths.get(getCurrentBranchPath(), "head").toFile(), hash);
        FileUtils.write(Paths.get(getHeadFile()).toFile(), hash);
    }

    public boolean isDetachedHead() throws IOException {
        return getCurrentBranch().isEmpty();
    }

    public void merge(String branchOrRevision) throws IOException, ClassNotFoundException, PatchFailedException {
        Pair<Path, Boolean> revisionPath =
                getRevisionPathByBranchOrRevisionName(branchOrRevision);

        Revision other = (Revision) Serializer.deserialize(revisionPath.getKey().toString());

        Revision currentHead = getHeadRevision();

        currentHead.merge(other);
    }

    public HashSet<String> getRevisionsDeletedFiles(String hash) throws IOException, ClassNotFoundException {
        Pair<Path, Boolean> revisionPath =
                getRevisionPathByBranchOrRevisionName(hash);
        try {
            return (HashSet<String>) Serializer.deserialize(revisionPath
                    .getKey().resolve("deleted").toString());
        } catch (IOException e) {
            logger.error("", e);
            return new HashSet<>();
        }
    }
}
