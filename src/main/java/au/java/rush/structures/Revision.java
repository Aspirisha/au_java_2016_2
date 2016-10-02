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
 * Created by andy on 9/25/16.
 */
public class Revision implements Serializable {
    public enum ElementStatus implements Serializable {
        NEW,
        MODIFIED,
        UNMODIFIED,
        DELETED,
        DELETED_EARLIER,
        NOT_TRACKED;
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
    String parentHash; // it is empty for the first revision
    String message;
    HashMap<String, DirElement> dirStructure; // key is file name relative to repo root
    int depth; // number of revisions from this to first revision (0 for first revision)
    transient Path repoRoot;
    transient BranchManager bm;
    transient Path revisionsPath;
    private transient Logger logger;


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

        logger.debug("New revision's parent is " + parentHash);
    }

    public Map<String, ElementStatus> getModifiedFiles() {
        return dirStructure.entrySet().stream()
                .filter(e -> e.getValue().status != DELETED_EARLIER &&
                        e.getValue().status != UNMODIFIED)
                .collect(Collectors.toMap(Map.Entry::getKey,
                s -> s.getValue().status));
    }

    public Map<String, ElementStatus> getModifiedRelativeToIndexFiles() throws PatchFailedException, IOException, ClassNotFoundException {
        Map<String, ElementStatus> result = new HashMap<>();
        for (Map.Entry<String, DirElement> k : dirStructure.entrySet()) {
            final String fileName = k.getKey();
            List<String> myContent = getFileContents(fileName);

            File absoluteFile = repoRoot.resolve(fileName).toFile();
            if (!absoluteFile.exists()) {
                if (myContent != null) {
                    result.put(fileName, DELETED);
                }
                continue;
            }

            if (myContent == null) {
                result.put(fileName, NEW);
                continue;
            }

            List<String> currentContent = FileUtils.readLines(absoluteFile);
            Patch<String> p = DiffUtils.diff(myContent, currentContent);
            if (!p.getDeltas().isEmpty()) {
                result.put(fileName, MODIFIED);
            }
        }

        return result;
    }

    private void createDirectoryStructureFromParent() throws IOException, ClassNotFoundException {
        Revision parent;
        String parentRevisionFile = bm.getRevisionFile(parentHash);
        if (!FileUtils.getFile(parentRevisionFile).exists())
            return;

        parent = (Revision) Serializer.deserialize(parentRevisionFile);

        IndexManager im = new IndexManager(repoRoot.toString());
        HashSet<String> deletedFiles = new HashSet<>();
        deletedFiles = (HashSet<String>) Serializer.deserialize(im.getDeletedFilesFile());

        for (Map.Entry<String, DirElement> k : parent.dirStructure.entrySet()) {
            final String key = k.getKey();
            switch (k.getValue().status) {
                case NEW:
                case MODIFIED:
                case UNMODIFIED:
                    DirElement updatedElement = k.getValue();
                    if (dirStructure.containsKey(key)) {
                        updatedElement.status = MODIFIED;
                        updatedElement.hasLastLineEmpty = dirStructure.get(key).hasLastLineEmpty;
                    } else {
                        updatedElement.status = deletedFiles.contains(key) ? DELETED : UNMODIFIED;
                    }
                    logger.debug(String.format("directory structure element: %s with status %s " +
                            "and modification timess %d",
                            key, updatedElement.status, updatedElement.prevModifyingRevisions.size()));

                    dirStructure.put(key, updatedElement);
                    break;
                case DELETED:
                case DELETED_EARLIER:
                    if (!dirStructure.containsKey(key)) {
                        updatedElement = k.getValue();
                        updatedElement.status = DELETED_EARLIER;
                        dirStructure.put(key, updatedElement);
                    } // else this file was created, so it's already added as new
                    break;
            }
        }
    }

    private void initTransientFields() {
        bm = new BranchManager(repoRoot.toString());
        revisionsPath = Paths.get(bm.getRevisionsDir());
        logger = LoggerFactory.getLogger(Revision.class);
    }

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
                    logger.debug(data);
                    dirStructure.put(indexRoot.relativize(file).toString(),
                            new DirElement(ElementStatus.NEW, new ArrayList<String>(), hasLastLineEmpty));
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

            if (de.status == DELETED || de.status == DELETED_EARLIER) {
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

        for (Map.Entry<String, DirElement> e : dirStructure.entrySet()) {
            String fileName = e.getKey();
            DirElement de = e.getValue();
            File absoluteFilePath = repoRoot.resolve(fileName).toFile();

            if (de.status == DELETED || de.status == DELETED_EARLIER) {
                Path p = Paths.get(bm.getFilePathAbsolute(fileName));

                if (!Files.exists(p) || Files.isDirectory(p)) {
                    continue;
                }

                try {
                    Files.delete(Paths.get(bm.getFilePathAbsolute(fileName)));
                } catch (DirectoryNotEmptyException x) { // impossible unless some miracle
                    x.printStackTrace();
                }

                ElementStatus otherStatus = other.getStatusForFile(fileName);
                switch (otherStatus) {
                    case DELETED:
                    case DELETED_EARLIER:
                    case NOT_TRACKED:
                        continue;
                }
                List<String> otherContent = other.getFileContents(fileName);

                FileUtils.writeLines(absoluteFilePath, otherContent);
                continue;
            }
            List<String> ownContent = getFileContents(fileName);
            ElementStatus otherStatus = other.getStatusForFile(fileName);
            switch (otherStatus) {
                case DELETED:
                case DELETED_EARLIER:
                case NOT_TRACKED:
                    FileUtils.writeLines(absoluteFilePath, ownContent);
                    continue;
            }

            List<String> otherContent = other.getFileContents(fileName);
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

            if (dirStructure.containsKey(fileName) || de.status == DELETED
                    || de.status == DELETED_EARLIER)
                continue;

            File absoluteFilePath = repoRoot.resolve(fileName).toFile();
            List<String> otherContent = other.getFileContents(fileName);
            FileUtils.writeLines(absoluteFilePath, otherContent);
        }
    }

    public List<String> getFileContents(String fileName) throws IOException, ClassNotFoundException, PatchFailedException {
        if (!dirStructure.containsKey(fileName))
            return null;

        switch (dirStructure.get(fileName).status) {
            case DELETED:
            case DELETED_EARLIER:
                return null;
        }

        logger.debug("Reading contents of " + fileName);
        DirElement de = dirStructure.get(fileName);

        Path firstRevisionCommit = null;

        if (!de.prevModifyingRevisions.isEmpty())
            firstRevisionCommit = Paths.get(bm.getCommitPath(de.prevModifyingRevisions.get(0)));
        else
            firstRevisionCommit = Paths.get(bm.getIndexDir());
        Path commitedFile = firstRevisionCommit.resolve(fileName);

        List<String> data = FileUtils.readLines(commitedFile.toFile());

        for (int i = 1; i < de.prevModifyingRevisions.size(); i++) {
            String revisionHash = de.prevModifyingRevisions.get(i);
            Path commitPath = Paths.get(bm.getCommitPath(revisionHash));
            Path revisionPath = Paths.get(bm.getRevisionFile(revisionHash));

            Revision r = (Revision) Serializer.deserialize(revisionPath.toString());
            if (r.dirStructure.get(fileName).status == DELETED) {
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
        // default serialization
        dirStructure.replaceAll((k, v) -> {
            if (v.status != UNMODIFIED && v.status != DELETED_EARLIER) {
                v.prevModifyingRevisions.add(ownHash);
                return v;
            }
            return v;
        });

        for (Map.Entry<String, DirElement> de : dirStructure.entrySet()) {
            logger.debug(String.format("Directory element %s which is" +
                            " being written has status %s and has bee modified %d times",
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
