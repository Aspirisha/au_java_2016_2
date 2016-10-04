package au.java.rush.utils;

import au.java.rush.structures.Revision;
import difflib.DiffUtils;
import difflib.Patch;
import difflib.PatchFailedException;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 * Created by andy on 9/25/16.
 */
public class IndexManager extends RepoManager {
    private final String indexRoot;
    private Logger logger = LoggerFactory.getLogger(IndexManager.class);

    public IndexManager(String repoRoot) {
        super(repoRoot);
        indexRoot = Paths.get(repoRoot, ".rush", "index").toString();
    }


    enum PatchCreationResult {
        NO_SUCH_FILE,
        SUCCESS
    }

    public HashSet<String> getDeletedFiles() {
        try {
            return (HashSet<String>) Serializer.deserialize(getDeletedFilesFile());
        } catch (IOException | ClassNotFoundException e) {
            return new HashSet<>();
        }
    }

    public HashSet<String> getDeletedFiles(String hash) {
        Path deletedFilePath = Paths.get(getRevisionDir(hash)).resolve("deleted");
        try {
            return (HashSet<String>) Serializer.deserialize(deletedFilePath.toString());
        } catch (IOException | ClassNotFoundException e) {
            return new HashSet<>();
        }
    }

    public HashMap<String, Boolean> getLineEndings() {
        try {
            return (HashMap<String, Boolean>) Serializer.deserialize(getLineEndingsFile());
        }catch (IOException | ClassNotFoundException e) {
            logger.error("", e);
            return new HashMap<>();
        }
    }

    public HashMap<String, Boolean> getLineEndings(String hash) {
        Path lineEndingsPath = Paths.get(getRevisionDir(hash)).resolve("line-endings");
        try {
            return (HashMap<String, Boolean>) Serializer.deserialize(lineEndingsPath.toString());
        }catch (IOException | ClassNotFoundException e) {
            logger.error("", e);
            return new HashMap<>();
        }
    }

    private class PatchCreator extends SimpleFileVisitor<Path> {
        final IndexManager manager;
        final Revision revision;
        HashSet<String> deletedFiles = null;
        private List<Path> failedToIndexFiles;
        HashMap<String, Boolean> lineEndings;

        PatchCreator(IndexManager manager, Revision revision) {
            this.manager = manager;
            this.revision = revision;
            failedToIndexFiles = new ArrayList<>();

            deletedFiles = manager.getDeletedFiles();
            lineEndings = manager.getLineEndings();
        }
        // Print information about
        // each type of file.
        @Override
        public FileVisitResult visitFile(Path file,
                                         BasicFileAttributes attr) {
            if (attr.isRegularFile() && !attr.isDirectory()) {
                System.out.format("Added file to index file: %s\n", file);
                try {
                    if (PatchCreationResult.SUCCESS != createPatchForFile(file.toString())) {
                        failedToIndexFiles.add(file);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    failedToIndexFiles.add(file);
                } catch (PatchFailedException | ClassNotFoundException e) {
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
        private PatchCreationResult createPatchForFile(String fileName) throws IOException, PatchFailedException, ClassNotFoundException {
            List<String> currentFile = null;
            // TODO ignore .rush
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

            String relPath = manager.getFilePathRelativeToRoot(fileName);

            if (currentFile == null) {
                deletedFiles.add(relPath);
                File maybeIndexed = Paths.get(indexRoot).resolve(relPath).toFile();
                if (maybeIndexed.exists()) {
                    FileUtils.deleteQuietly(maybeIndexed);
                }
                return PatchCreationResult.SUCCESS;
            }

            // set line ending info
            String data = FileUtils.readFileToString(
                    FileUtils.getFile(manager.getFilePathAbsolute(fileName)));

            lineEndings.put(relPath, data.isEmpty() || data.endsWith("\n"));

            String indexPath = getFileIndex(relPath);
            deletedFiles.remove(relPath);
            if (revisionFile == null) {
                Files.createDirectories(Paths.get(indexPath).getParent());
                Files.copy(Paths.get(manager.getFilePathAbsolute(fileName)),
                        Paths.get(indexPath), REPLACE_EXISTING);
                return PatchCreationResult.SUCCESS;
            }

            Patch p = DiffUtils.diff(revision.getFileContents(relPath), currentFile);

            if (p.getDeltas().isEmpty())
                return PatchCreationResult.SUCCESS;
            Files.createDirectories(Paths.get(indexPath).getParent());
            Serializer.serialize(p, indexPath);
            return PatchCreationResult.SUCCESS;
        }
    }

    public String getDeletedFilesFile() {
        return Paths.get(getInternalRoot(), "deleted").toString();
    }

    /**
     *
     * @param fileOrDir relative to repoRoot path
     * @return
     */
    public void createPatch(String fileOrDir) throws IOException, ClassNotFoundException {
        Path startingDir = Paths.get(repoRoot, fileOrDir);

        BranchManager bm = new BranchManager(repoRoot);
        Revision r = bm.hasHeadRevision() ? bm.getHeadRevision() : null;

        logger.debug("Head revision is " +
                (r == null ? "null" : r.getHash()));

        if (startingDir.toFile().exists()) {
            PatchCreator pf = new PatchCreator(this, r);
            Files.walkFileTree(startingDir, pf);
            Serializer.serialize(pf.deletedFiles, getDeletedFilesFile());
            Serializer.serialize(pf.lineEndings, getLineEndingsFile());
        } else {
            Revision indexRev = Revision.getTempIndexRevision(Paths.get(repoRoot));
            Map<String, Revision.ElementStatus> files = indexRev.getNotDeletedFiles();
            HashSet<String> deletedFiles = getDeletedFiles();
            for (Map.Entry<String, Revision.ElementStatus> e : files.entrySet()) {
                Path fileName = Paths.get(e.getKey());
                if (!fileName.startsWith(fileName)) {
                    continue;
                }
                deletedFiles.add(fileName.toString());
                FileUtils.deleteQuietly(Paths.get(getIndexDir())
                        .resolve(fileName).toFile());
            }
            Serializer.serialize(deletedFiles, getDeletedFilesFile());
        }
    }

    // file - path relative to root
    public String getFileIndex(String file) {
        return String.join(File.separator, getIndexDir(), file);
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

    /**
     *
     * @param message commit message
     * @return hash of new commit or empty string, if there was nothing to commit
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public String commit(String message) throws IOException, ClassNotFoundException {
        if (!Files.exists(Paths.get(getIndexDir())))
            return "";

        Revision r = new Revision(Paths.get(repoRoot), message);
        String hash = r.getHash();
        Path commitDir = Paths.get(getCommitPath(hash));
        Path revisionDir = commitDir.getParent();
        Files.createDirectories(revisionDir);

        Serializer.serialize(r, getRevisionFile(hash));

        File index = new File(getIndexDir());
        FileUtils.moveDirectory(index, commitDir.toFile());
        FileUtils.copyFileToDirectory(FileUtils.getFile(getDeletedFilesFile()), revisionDir.toFile());
        FileUtils.copyFileToDirectory(FileUtils.getFile(getLineEndingsFile()), revisionDir.toFile());
        index.mkdirs();

        BranchManager bm = new BranchManager(repoRoot);
        bm.updateHeads(hash);
        return hash;
    }

    class UntrackedFilesGetter extends SimpleFileVisitor<Path> {
        final Path repoRoot;
        final Revision indexRevision;
        private List<String> result;

        UntrackedFilesGetter(Path repoRoot, Revision r) {
            this.repoRoot = repoRoot;
            indexRevision = r;
            result = new ArrayList<>();
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
            if (repoRoot.resolve(".rush").equals(dir))
                return FileVisitResult.SKIP_SUBTREE;
            return CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path file,
                                         BasicFileAttributes attr) {
            if (attr.isRegularFile() && !attr.isDirectory()) {
                String fileName = repoRoot.relativize(file).toString();
                if (indexRevision.getStatusForFile(fileName) == Revision.ElementStatus.NOT_TRACKED) {
                    result.add(fileName);
                }
            }
            return CONTINUE;
        }

        List<String> getResult() {
            return result;
        }
    }

    public Map<String, Revision.ModificationWithRepsectToParentRevisionType> getCurrentlyIndexedFiles() throws IOException, ClassNotFoundException {
        Revision r = Revision.getTempIndexRevision(Paths.get(repoRoot));
        return r.getModifiedFiles();
    }

    public Map<String, Revision.ModificationWithRepsectToIndexType> getCurrentlyModifiedFiles() throws IOException, ClassNotFoundException, PatchFailedException {
        Revision r = Revision.getTempIndexRevision(Paths.get(repoRoot));
        return r.getModifiedRelativeToIndexFiles();
    }

    public List<String> getUntrackedFiles() throws IOException, ClassNotFoundException {
        Revision r = Revision.getTempIndexRevision(Paths.get(repoRoot));
        UntrackedFilesGetter ufg = new UntrackedFilesGetter(Paths.get(repoRoot), r);
        Files.walkFileTree(Paths.get(repoRoot), ufg);

        return ufg.getResult();
    }

    public String getLineEndingsFile() {
        return Paths.get(getInternalRoot(), "line-endings").toString();
    }

    /**
     * resets given file (or all files under this directory, if fileName is directory)
     * @param fileName fil name relative to repo root
     */
    public void resetFile(String fileName) throws IOException, ClassNotFoundException {
        Revision currentRevision = Revision.getTempIndexRevision(Paths.get(repoRoot));
        Map<String, Revision.ModificationWithRepsectToParentRevisionType> modifiedFiles
                = currentRevision.getModifiedFiles();
        HashSet<String> deletedFiles = getDeletedFiles();
        HashSet<String> parentDeletedFiles = getDeletedFiles(currentRevision.getParentHash());
        HashMap<String, Boolean> parentLineEndings = getLineEndings(currentRevision.getParentHash());
        HashMap<String, Boolean> lineEndings = getLineEndings();

        for (String file : modifiedFiles.keySet()) {
            if (!file.startsWith(fileName))
                continue;
            switch (modifiedFiles.get(file)) {
                case NEW:
                case MODIFIED: {
                    FileUtils.deleteQuietly(Paths.get(getIndexDir())
                            .resolve(file).toFile());

                    if (parentDeletedFiles.contains(file)) {
                        deletedFiles.add(file);
                    }
                    break;
                }
                case DELETED:
                    deletedFiles.remove(file);
                    break;
            }

            if (parentLineEndings.containsKey(file)) {
                lineEndings.put(file, parentLineEndings.get(file));
            } else {
                lineEndings.remove(file);
            }
        }

        Serializer.serialize(deletedFiles, getDeletedFilesFile());
        Serializer.serialize(lineEndings, getLineEndingsFile());
    }

    public void reset() throws IOException, ClassNotFoundException {
        resetFile(repoRoot);
    }
}
