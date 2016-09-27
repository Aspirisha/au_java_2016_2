package au.java.rush.utils;

import difflib.DiffUtils;
import difflib.Patch;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 * Created by andy on 9/25/16.
 */
public class IndexManager extends RepoManager {
    private final String indexRoot;
    public IndexManager(String repoRoot) {
        super(repoRoot);
        indexRoot = Paths.get(repoRoot, ".rush", "index").toString();
    }


    enum PatchCreationResult {
        NO_SUCH_FILE,
        SUCCESS
    }


    class PatchCreator extends SimpleFileVisitor<Path> {
        final RepoManager manager;
        final Revision revision;
        HashSet<String> deletedFiles = null;
        private List<Path> failedToIndexFiles;

        PatchCreator(RepoManager manager, Revision revision) {
            this.manager = manager;
            this.revision = revision;
            failedToIndexFiles = new ArrayList<>();

            try {
                deletedFiles = (HashSet<String>) Serializer.deserialize(getDeletedFilesFile());
            } catch (IOException | ClassNotFoundException e) {
                deletedFiles = new HashSet<>();
            }
        }
        // Print information about
        // each type of file.
        @Override
        public FileVisitResult visitFile(Path file,
                                         BasicFileAttributes attr) {
            if (attr.isRegularFile() && !attr.isDirectory()) {
                System.out.format("Regular file: %s ", file);
                try {
                    if (PatchCreationResult.SUCCESS != createPatchForFile(file.toString())) {
                        failedToIndexFiles.add(file);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    failedToIndexFiles.add(file);
                }
            }
            return CONTINUE;
        }

        List<Path> getFailedToIndexFiles() {
            return failedToIndexFiles;
        }
        /**
         *
         * @param fileName file to create patch for
         * @return patch whichcan be applied to get current fileName fromthe one that lived in repo
         * @throws IOException
         */
        private PatchCreationResult createPatchForFile(String fileName) throws IOException {
            List<String> currentFile = null;
            try {
                currentFile = readRepoFile(fileName);
            } catch (IOException e) {
                currentFile = null;
            }

            List<String> revisionFile = revision != null ?
                    revision.getFileContents(manager.getFilePathRelativeToRoot(fileName)) : null;

            if (revisionFile == null && currentFile == null) {
                return PatchCreationResult.NO_SUCH_FILE;
            }

            PathRelativeToRoot relPath = manager.getFilePathRelativeToRoot(fileName);
            String indexPath = getFileIndex(relPath);
            if (revisionFile == null) {
                Files.copy(Paths.get(manager.getFilePathAbsolute(fileName)),
                        Paths.get(indexPath), REPLACE_EXISTING);
                return PatchCreationResult.SUCCESS;
            }

            if (currentFile == null) {
                deletedFiles.add(relPath.toString());
                return PatchCreationResult.SUCCESS;
            }
            Patch p = DiffUtils.diff(revision.getFileContents(relPath), currentFile);
            deletedFiles.remove(relPath.toString());

            // TODO if p is not empty, else don't write patch
            Serializer.serialize(p, indexPath);
            return PatchCreationResult.SUCCESS;
        }
    }

    String getDeletedFilesFile() {
        return Paths.get(indexRoot, "deleted").toString();
    }
    /**
     *
     * @param fileOrDir relative to repoRoot path
     * @return
     */
    public void createPatch(String fileOrDir) throws IOException {
        Path startingDir = Paths.get(repoRoot, fileOrDir);

        BranchManager bm = new BranchManager(repoRoot);
        Revision r = bm.getHeadRevision();
        PatchCreator pf = new PatchCreator(bm, r);
        Files.walkFileTree(startingDir, pf);
        Serializer.serialize(pf.deletedFiles, getDeletedFilesFile());
    }

    public String getFileIndex(PathRelativeToRoot file) {
        return String.join(File.separator, getIndexDir(), file.path.toString());
    }

    /**
     *
     * @param fileName relative to root name
     * @return
     */
    public List<String> getIndexedFile(String fileName) {
        Path indexPath = Paths.get(indexRoot, fileName);

        Patch p = null;
        try (ObjectInputStream in = new ObjectInputStream(
                new FileInputStream(indexPath.toString()))) {
            p = (Patch) in.readObject();
        } catch (FileNotFoundException e) {
            return null;
        } catch (ClassNotFoundException e) {
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        return null;
    }

    public void commit(String message) throws IOException {
        BranchManager bm = new BranchManager(repoRoot);
        Revision r = new Revision(Paths.get(repoRoot), message);
        String hash = r.getHash();
        Serializer.serialize(r, String.join(File.separator, bm.getRevisionsDir(), hash));
    }
}
