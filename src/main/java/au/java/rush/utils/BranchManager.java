package au.java.rush.utils;

import com.google.common.base.Strings;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by andy on 9/25/16.
 */
public class BranchManager extends RepoManager {
    private final String branchPath;
    BranchManager(String repoPath) {
        super(repoPath);
        branchPath = Paths.get(repoPath, ".rush", "branches").toString();
    }

    public String getCurrentBranch() {
        String headBranch = null;
        try (BufferedReader br = new BufferedReader(
                new FileReader(getHeadFile().toString())))
        {
            headBranch = br.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return headBranch;
    }

    public String getCurrentBranchPath() {
        String curBranch = getCurrentBranch();
        return String.join(File.separator, branchPath, curBranch);
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

        String revPath = String.join(File.separator, repoRoot, "revisions", rev);
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
}
