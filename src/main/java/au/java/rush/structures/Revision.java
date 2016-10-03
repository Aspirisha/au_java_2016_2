package au.java.rush.structures;

import au.java.rush.utils.BranchManager;
import au.java.rush.utils.FileHasher;
import au.java.rush.utils.IndexManager;
import au.java.rush.utils.Serializer;
import com.google.common.hash.Hashing;
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
import java.util.stream.Collectors;

import static au.java.rush.structures.Revision.ElementStatus.*;
import static difflib.DiffUtils.diff;
import static java.nio.file.FileVisitResult.CONTINUE;

/**
 * Revision represents stores whole revision information. In particular,
 * revision is
 *   1. set of patches for all modified during the revision files
 *   2. Directory structure snapshot, which represents all tracked files,
 *      whose deletion wasn't tracked
 *   3. parent hash, depth and other useful stuff
 *
 *   There is some info that is in fact part of revision, but stored separatly now in files
 *   because otherwise it won't be convenient to get this info:
 *   4. Set of deleted files. These are files which were tracked, but some day
 *      user deleted them and added this to index and committed.
 *   5. line endings info for each tracked file (whether or not file has last line empty)
 *
 * Created by andy on 9/25/16.
 */
public class Revision implements Serializable {
    public enum ElementStatus implements Serializable {
        MODIFIED,
        UNMODIFIED,
        NOT_TRACKED
    }

    private static class DirElement implements Serializable {
        public DirElement() {}
        public DirElement(ElementStatus es, ArrayList<String> prevModifyingRevisions, boolean hasLastLineEmpty) {
            status = es;
            this.prevModifyingRevisions = prevModifyingRevisions;
            this.hasLastLineEmpty = hasLastLineEmpty;
        }

        ElementStatus status;
        ArrayList<String> prevModifyingRevisions;
        boolean hasLastLineEmpty;
    }

    private String ownHash;
    private String parentHash; // it is empty for the first revision
    private String message;
    private HashMap<String, DirElement> dirStructure; // key is file name relative to repo root
    private int depth; // number of revisions from this to first revision (0 for first revision)
    private transient Path repoRoot;
    private transient BranchManager bm;
    private transient IndexManager im;
    private transient Logger logger;

    @SuppressWarnings("unused") // This constructor is used in deserialization
    public Revision() {    }

    public String getHash() {
        return ownHash;
    }

    public String getParentHash() {
        return parentHash;
    }

    public String getMessage() {
        return message;
    }

    public static Revision getTempIndexRevision(Path repoRoot) throws IOException, ClassNotFoundException {
        return new Revision(repoRoot, null);
    }

    // creates revision from index
    public Revision(Path repoRoot, String message) throws IOException, ClassNotFoundException {
        this.repoRoot = repoRoot;
        this.message = message;

        initTransientFields();
        this.parentHash = bm.getHeadRevisionHash();


        if (!parentHash.isEmpty()) {
            Revision parent = bm.getRevisionByHash(parentHash);
            depth = parent.depth + 1;
        } else {
            depth = 0;
        }
        String filesHash = FileHasher.getDirectoryOrFileHash(bm.getIndexDir(),
                Paths.get(bm.getIndexDir())).toString();

        // add dependency on parent revision
        ownHash = String.valueOf(Hashing.md5().hashString(filesHash + parentHash));
        dirStructure = new HashMap<>();

        // create directory structure
        if (parentHash == null) {
            createNewDirectoryStructure();
        } else {
            createNewDirectoryStructure();
            createDirectoryStructureFromParent();
        }
    }

    public enum ModificationWithRepsectToParentRevisionType {
        NEW,
        DELETED,
        MODIFIED
    }

    /**
     * @return files whose modifications are added to current index,
     * i.e. index folder contains their patches/contents
     */
    public Map<String, ModificationWithRepsectToParentRevisionType> getModifiedFiles() throws IOException, ClassNotFoundException {

        String parentRevisionFile = bm.getRevisionFile(parentHash);
        final Revision parent = FileUtils.getFile(parentRevisionFile).exists() ?
                (Revision) Serializer.deserialize(parentRevisionFile) : null;

        List<String> deleted = getDeletedFilesSinceLastRevision(parent);
        Map<String, ModificationWithRepsectToParentRevisionType> modified = deleted
                .stream()
                .collect(Collectors.toMap(s -> s, x -> ModificationWithRepsectToParentRevisionType.DELETED));

        modified.putAll(dirStructure.entrySet().stream()
                .filter(e -> e.getValue().status == MODIFIED)
                .collect(Collectors.toMap(Map.Entry::getKey,
                e -> {
                    if (null != parent && parent.dirStructure.containsKey(e.getKey())) {
                        return ModificationWithRepsectToParentRevisionType.MODIFIED;
                    }
                    return ModificationWithRepsectToParentRevisionType.NEW;
                })));
        return modified;
    }

    /**
     * Retrieves files which were tracked till previous revision and were deleted in current
     * @param parent parent of current revision
     * @return list of deleted files
     */
    private List<String> getDeletedFilesSinceLastRevision(Revision parent) {
        if (parent == null)
            return new ArrayList<>();

        return parent.dirStructure.entrySet()
                .stream()
                .filter(e -> !dirStructure.containsKey(e.getKey()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    public Map<String, ElementStatus> getNotDeletedFiles() {
        return dirStructure.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        s -> s.getValue().status));
    }

    public enum ModificationWithRepsectToIndexType {
        DELETED,
        MODIFIED
    }

    public Map<String, ModificationWithRepsectToIndexType> getModifiedRelativeToIndexFiles() throws PatchFailedException, IOException, ClassNotFoundException {
        Map<String, ModificationWithRepsectToIndexType> result = new HashMap<>();
        for (Map.Entry<String, DirElement> k : dirStructure.entrySet()) {
            final String fileName = k.getKey();
            List<String> myContent = getFileContents(fileName);

            File indexPath = Paths.get(bm.getIndexDir()).resolve(fileName).toFile();

            if (indexPath.exists()) {
                if (myContent == null) // this is not patch but rather text
                    myContent = FileUtils.readLines(indexPath);
                else {
                    Patch<String> p = (Patch<String>) Serializer.deserialize(indexPath.toString());
                    myContent = DiffUtils.patch(myContent, p);
                }
            }

            File absoluteFile = repoRoot.resolve(fileName).toFile();
            if (!absoluteFile.exists()) {
                if (myContent != null) {
                    result.put(fileName, ModificationWithRepsectToIndexType.DELETED);
                }
                continue;
            }

            if (myContent == null) {
                logger.error("Contents of tracked files shouldn't be null");
                throw new RuntimeException("Contents of tracked files shouldn't be null");
            }

            List<String> currentContent = FileUtils.readLines(absoluteFile);
            Patch<String> p = DiffUtils.diff(myContent, currentContent);
            if (!p.getDeltas().isEmpty()) {
                myContent.stream().forEach(s -> logger.debug("[myContent] " + s));
                currentContent.stream().forEach(s -> logger.debug("[currentContent] " + s));
                result.put(fileName, ModificationWithRepsectToIndexType.MODIFIED);
            }
        }

        return result;
    }

    private void initTransientFields() {
        bm = new BranchManager(repoRoot.toString());
        im = new IndexManager(repoRoot.toString());
        logger = LoggerFactory.getLogger(Revision.class);
    }

    /**
     * adds to the directory structure of the newly created revision those
     * files which were already tracked by parent, if they are not deleted by current index
     * all parent files, which were added by current index are considered modified
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private void createDirectoryStructureFromParent() throws IOException, ClassNotFoundException {
        Revision parent;
        String parentRevisionFile = bm.getRevisionFile(parentHash);
        if (!FileUtils.getFile(parentRevisionFile).exists())
            return;

        parent = (Revision) Serializer.deserialize(parentRevisionFile);

        IndexManager im = new IndexManager(repoRoot.toString());
        HashSet<String> deletedFiles = im.getDeletedFiles();

        for (Map.Entry<String, DirElement> k : parent.dirStructure.entrySet()) {
            final String key = k.getKey();
            DirElement updatedElement = k.getValue();
            if (dirStructure.containsKey(key)) {
                updatedElement.status = MODIFIED;
                updatedElement.hasLastLineEmpty = dirStructure.get(key).hasLastLineEmpty;
                dirStructure.put(key, updatedElement);
            } else {
                if (!deletedFiles.contains(key)) {
                    dirStructure.put(key, updatedElement);
                }
            }
        }
    }

    /**
     * adds to the directory structure of the newly created revision only those
     * file which were indexed since last commit
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private void createNewDirectoryStructure() throws IOException, ClassNotFoundException {
        Path indexRoot = Paths.get(bm.getIndexDir());

        IndexManager im = new IndexManager(repoRoot.toString());
        final HashMap<String, Boolean> lineEndings = new HashMap<>();

        try {
            lineEndings.putAll((HashMap<String, Boolean>) Serializer.deserialize(
                    im.getLineEndingsFile()));
        } catch (IOException e) {
            logger.debug("", e);
        }
        class DirectoryStructureCreator extends SimpleFileVisitor<Path> {
            @Override
            public FileVisitResult visitFile(Path file,
                                             BasicFileAttributes attr) throws IOException {

                if (attr.isRegularFile() && !attr.isDirectory()) {
                    String data = FileUtils.readFileToString(file.toFile());
                    logger.debug(indexRoot.relativize(file).toString());
                    boolean hasLastLineEmpty = lineEndings.get(indexRoot.relativize(file).toString());
                    logger.debug(String.format("file %s ends with newline: %b", file.toString(), hasLastLineEmpty));
                    dirStructure.put(indexRoot.relativize(file).toString(),
                            new DirElement(ElementStatus.MODIFIED, new ArrayList<String>(), hasLastLineEmpty));
                }
                return CONTINUE;
            }
        }

        DirectoryStructureCreator pf = new DirectoryStructureCreator();
        Files.walkFileTree(indexRoot, pf);
    }

    public void checkout() throws PatchFailedException, IOException, ClassNotFoundException {
        for (Map.Entry<String, DirElement> e : dirStructure.entrySet()) {
            String fileName = e.getKey();
            DirElement de = e.getValue();
            HashSet<String> deleted = im.getDeletedFiles();

            if (deleted.contains(fileName)) {
                Path p = Paths.get(bm.getFilePathAbsolute(fileName));

                if (!Files.exists(p) || Files.isDirectory(p)) {
                    continue;
                }

                try {
                    Files.delete(Paths.get(bm.getFilePathAbsolute(fileName)));
                } catch (DirectoryNotEmptyException x) { // impossible unless some miracle
                    x.printStackTrace();
                }

                continue;
            }
            List<String> fileContents = getFileContents(bm.getFilePathRelativeToRoot(fileName));

            File file = repoRoot.resolve(fileName).toFile();
            logger.info(String.format("file %s has last line empty: %b", e.getKey(), de.hasLastLineEmpty));
            if (de.hasLastLineEmpty) {
                FileUtils.writeLines(file, fileContents);
                continue;
            } else if (fileContents.size() > 1) {
                FileUtils.writeLines(file, fileContents.subList(0, fileContents.size() - 1));
                FileUtils.write(file, fileContents.get(fileContents.size() - 1), true);
            } else {
                FileUtils.write(file, fileContents.get(fileContents.size() - 1));
            }
        }
    }

    private Revision findLCA(Revision other) throws IOException, ClassNotFoundException {
        Revision currentPredecessor = this;
        while (other.depth > currentPredecessor.depth) {
            other = bm.getRevisionByHash(other.getHash());
        }

        while (other.depth < currentPredecessor.depth) {
            currentPredecessor = bm.getRevisionByHash(currentPredecessor.getHash());
        }

        while (!other.getHash().equals(currentPredecessor.getHash()) && other.depth > 0) {
            other = bm.getRevisionByHash(other.getHash());
            currentPredecessor = bm.getRevisionByHash(currentPredecessor.getHash());
        }

        if (!other.getHash().equals(currentPredecessor.getHash()))
            return null;
        return other;
    }

    public void merge(Revision other) throws IOException, PatchFailedException, ClassNotFoundException {
        Path mergeResultPath = repoRoot.resolve("merge-result");

        HashSet<String> deleted = im.getDeletedFiles();
        HashSet<String> otherDeleted = bm.getRevisionsDeletedFiles(other.getHash());
        for (Map.Entry<String, DirElement> e : dirStructure.entrySet()) {
            String fileName = e.getKey();
            DirElement de = e.getValue();
            File absoluteFilePath = repoRoot.resolve(fileName).toFile();

            List<String> ownContent = getFileContents(fileName);
            ElementStatus otherStatus = other.getStatusForFile(fileName);
            switch (otherStatus) {
                case NOT_TRACKED:
                    FileUtils.writeLines(absoluteFilePath, ownContent);
                    continue;
            }

            List<String> otherContent = otherDeleted.contains(fileName) ?
                    new ArrayList<>() : other.getFileContents(fileName);
            Patch<String> p = diff(ownContent, otherContent);
            if (p.getDeltas().isEmpty()) {
                FileUtils.writeLines(absoluteFilePath, ownContent);
                continue;
            }

            List<String> diff = DiffUtils.generateUnifiedDiff(ownHash,
                    other.getHash(), ownContent, p, 1000000);

            FileUtils.writeLines(absoluteFilePath, diff);
        }

        for (Map.Entry<String, DirElement> e : other.dirStructure.entrySet()) {
            String fileName = e.getKey();
            DirElement de = e.getValue();

            if (dirStructure.containsKey(fileName))
                continue;

            File absoluteFilePath = repoRoot.resolve(fileName).toFile();
            List<String> otherContent = other.getFileContents(fileName);
            if (!deleted.contains(fileName)) {
                FileUtils.writeLines(absoluteFilePath, otherContent);
            } else {
                List<String> ownContent = new ArrayList<>();
                Patch<String> p = diff(ownContent, otherContent);
                if (p.getDeltas().isEmpty()) {
                    FileUtils.write(absoluteFilePath, "");
                    continue;
                }
                List<String> diff = DiffUtils.generateUnifiedDiff(ownHash,
                        other.getHash(), ownContent, p, 1000000);
                FileUtils.writeLines(absoluteFilePath, diff);
            }
        }
    }

    public List<String> getFileContents(String fileName) throws IOException, ClassNotFoundException, PatchFailedException {
        if (!dirStructure.containsKey(fileName))
            return null;

        logger.debug("Reading contents of " + fileName);
        DirElement de = dirStructure.get(fileName);

        Path firstRevisionCommit = null;

        if (!de.prevModifyingRevisions.isEmpty())
            firstRevisionCommit = Paths.get(bm.getCommitPath(de.prevModifyingRevisions.get(0)));
        else
            return null;

        Path commitedFile = firstRevisionCommit.resolve(fileName);

        List<String> data = FileUtils.readLines(commitedFile.toFile());

        for (int i = 1; i < de.prevModifyingRevisions.size(); i++) {
            String revisionHash = de.prevModifyingRevisions.get(i);
            Path commitPath = Paths.get(bm.getCommitPath(revisionHash));
            Path revisionPath = Paths.get(bm.getRevisionFile(revisionHash));

            Revision r = (Revision) Serializer.deserialize(revisionPath.toString());
            if (r.dirStructure.get(fileName) == null) {
                data.clear();
            } else {
                data = applyPatch(commitPath.resolve(fileName), data);
            }
        }

        dirStructure.put(fileName, de);
        return data;
    }

    static List<String> applyPatch(Path patchFile, List<String> target) throws IOException, ClassNotFoundException, PatchFailedException {
        Patch<String> restored;
        try (FileInputStream fis = new FileInputStream(patchFile.toString());
             ObjectInputStream ois = new ObjectInputStream(fis)) {

            restored = (Patch<String>) ois.readObject();
        }

        return restored.applyTo(target);
    }

    private void writeObject(ObjectOutputStream oos)
            throws IOException {
        assert(message != null); // we shouldn't write down temp revisions

        // default serialization
        for (String s : dirStructure.keySet()) {
            logger.debug("writing down revision. \nDirectory element " + s + " status:    " + dirStructure.get(s).status );
        }

        dirStructure.replaceAll((k, v) -> {
            if (v.status == MODIFIED) {
                v.prevModifyingRevisions.add(ownHash);
                v.status = UNMODIFIED;
                return v;
            }
            return v;
        });

        for (Map.Entry<String, DirElement> de : dirStructure.entrySet()) {
            logger.debug(String.format("Directory element %s which is" +
                            " being written has status %s and has been modified %d times",
                    de.getKey(), de.getValue().status,
                    de.getValue().prevModifyingRevisions.size()));
        }
        oos.defaultWriteObject();
        oos.writeObject(repoRoot.toString());
    }

    private void readObject(ObjectInputStream ois)
            throws ClassNotFoundException, IOException {
        // default deserialization
        ois.defaultReadObject();
        repoRoot = Paths.get((String)ois.readObject());
        initTransientFields();
    }

    public ElementStatus getStatusForFile(String fileName) {
        DirElement de = dirStructure.get(fileName);
        if (de == null)
            return NOT_TRACKED;
        return de.status;
    }
}
