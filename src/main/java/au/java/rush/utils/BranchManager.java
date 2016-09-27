package au.java.rush.utils;

import com.google.common.base.Strings;
import difflib.PatchFailedException;
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

    public String getCurrentBranch() {
        try {
            return FileUtils.readFileToString(new File(getCurrentBranchFile()));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    public String getBranchPath(String branch) {
        return String.join(File.separator, branchPath, branch);
    }

    public String getCurrentBranchPath() {
        String curBranch = getCurrentBranch();
        return getBranchPath(curBranch);
    }

    public Revision getHeadRevision() {
        String rev = getHeadRevisionHash();
        String revPath = String.join(File.separator, repoRoot, "revisions", rev);

        try {
            return (Revision) Serializer.deserialize(revPath);
        } catch (IOException e) {
           // e.printStackTrace();
        } catch (ClassNotFoundException e) {
           // e.printStackTrace();
        }

        return null;
    }

    public Revision getCurrentBranchHeadRevision() {
        return getBranchHeadRevision(getCurrentBranch());
    }

    /**
     *
     * @return hash of current head revision
     */
    public String getHeadRevisionHash() {
        String branchConfig = String.join(File.separator, getInternalRoot(), "head");
        String rev = null;
        try (BufferedReader in = new BufferedReader(new FileReader(branchConfig))) {
            return in.readLine();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public Revision getBranchHeadRevision(String branchName) {
        if (branchName == null)
            return null;

        String branchConfig = String.join(File.separator, branchPath, branchName, "head");
        String rev = null;
        try (BufferedReader in = new BufferedReader(new FileReader(branchConfig))) {
            rev = in.readLine();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        String revPath = getRevisionFile(rev);
        try (FileInputStream fis = new FileInputStream(revPath);
              ObjectInputStream ois = new ObjectInputStream(fis)) {
            return (Revision) ois.readObject();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }

    }
    public Path getFile(Path filePath) {
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

    public void checkout(String branchOrRevision) throws IOException, ClassNotFoundException, PatchFailedException {
        Path revisionPath = null;

        boolean isBranchCo = false;
        if (branchExists(branchOrRevision)) {
            revisionPath = Paths.get(getRevisionsDir(),
                    String.valueOf(getBranchHeadRevision(branchOrRevision)));
            isBranchCo = true;
        } else {
            revisionPath = Paths.get(getRevisionFile(branchOrRevision));
            if (!Files.exists(revisionPath)) {
                throw new FileNotFoundException();
            }
        }

        Revision r = (Revision) Serializer.deserialize(revisionPath.toString());
        r.checkout();

        if (isBranchCo) {
            setCurrentBranch(branchOrRevision);
        }
    }

    public Revision getRevision(String hash) throws IOException, ClassNotFoundException {
        Path revisionPath = Paths.get(getRevisionFile(hash));
        return (Revision) Serializer.deserialize(revisionPath.toString());
    }

    public void updateHeads(String hash) throws IOException {
        String currentBranch = FileUtils.readFileToString(new File(getCurrentBranchFile()));
        FileUtils.write(Paths.get(getCurrentBranchPath(), "head").toFile(), hash);
        FileUtils.write(Paths.get(getHeadFile()).toFile(), hash);
    }
}
