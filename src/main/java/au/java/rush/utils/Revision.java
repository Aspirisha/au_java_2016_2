package au.java.rush.utils;

import com.google.common.hash.Hashing;
import difflib.Patch;
import difflib.PatchFailedException;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

import static au.java.rush.utils.Revision.ElementStatus.*;
import static java.nio.file.FileVisitResult.CONTINUE;

/**
 * Created by andy on 9/25/16.
 */
public class Revision implements Serializable {
    enum ElementStatus implements Serializable {
        NEW,
        MODIFIED,
        UNMODIFIED,
        DELETED,
        DELETED_EARLIER
    }

    private static class DirElement implements Serializable {
        public DirElement() {}
        public DirElement(ElementStatus es, ArrayList<String> prevModifyingRevisions) {
            status = es;
            this.prevModifyingRevisions = prevModifyingRevisions;
        }

        ElementStatus status;
        ArrayList<String> prevModifyingRevisions;
    }

    private String ownHash;
    String parentHash; // it is empty for the first revision
    String message;
    HashMap<String, DirElement> dirStructure;
    transient Path repoRoot;
    transient RepoManager rm;
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

    // creates revision from index
    public Revision(Path repoRoot, String message) throws IOException, ClassNotFoundException {
        BranchManager bm = new BranchManager(repoRoot.toString());
        this.parentHash = bm.getHeadRevisionHash();
        this.repoRoot = repoRoot;
        this.message = message;
        String filesHash = FileHasher.getDirectoryOrFileHash(bm.getIndexDir(),
                Paths.get(bm.getIndexDir())).toString();

        // add dependency on parent revision
        ownHash = String.valueOf(Hashing.md5().hashString(filesHash + parentHash));
        dirStructure = new HashMap<>();

        initTransientFields();
        // create directory structure
        if (parentHash == null) {
            createNewDirectoryStructure();
        } else {
            createNewDirectoryStructure();
            createDirectoryStructureFromParent();
        }

        logger.debug("New revision's parent is " + parentHash);
    }

    private void createDirectoryStructureFromParent() throws IOException, ClassNotFoundException {
        Revision parent;
        String parentRevisionFile = rm.getRevisionFile(parentHash);
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
        rm = new RepoManager(repoRoot.toString());
        revisionsPath = Paths.get(rm.getRevisionsDir());
        logger = LoggerFactory.getLogger(Revision.class);
    }

    private void createNewDirectoryStructure() throws IOException {
        Path indexRoot = Paths.get(rm.getIndexDir());

        class DirectoryStructureCreator extends SimpleFileVisitor<Path> {
            @Override
            public FileVisitResult visitFile(Path file,
                                             BasicFileAttributes attr) {

                if (attr.isRegularFile() && !attr.isDirectory()) {
                    dirStructure.put(indexRoot.relativize(file).toString(), new DirElement(ElementStatus.NEW, new ArrayList<String>()));
                }
                return CONTINUE;
            }
        }

        DirectoryStructureCreator pf = new DirectoryStructureCreator();
        Files.walkFileTree(indexRoot, pf);
    }

    public void checkout() throws PatchFailedException, IOException, ClassNotFoundException {
        for (Map.Entry<String, DirElement> e : dirStructure.entrySet()) {
            String f = e.getKey();
            DirElement de = e.getValue();

            if (de.status == DELETED || de.status == DELETED_EARLIER) {
                Path p = Paths.get(rm.getFilePathAbsolute(f));

                if (!Files.exists(p) || Files.isDirectory(p)) {
                    continue;
                }

                try {
                    Files.delete(Paths.get(rm.getFilePathAbsolute(f)));
                } catch (DirectoryNotEmptyException x) { // impossible unless some miracle
                    x.printStackTrace();
                }

                continue;
            }
            List<String> fileContents = getFileContents(rm.getFilePathRelativeToRoot(f));

            FileUtils.writeLines(repoRoot.resolve(f).toFile(), fileContents);
        }
    }

    public List<String> getFileContents(RepoManager.PathRelativeToRoot fileName) throws IOException, ClassNotFoundException, PatchFailedException {
        if (!dirStructure.containsKey(fileName.toString()))
            return null;

        logger.debug("Reading contents of " + fileName);
        DirElement de = dirStructure.get(fileName.toString());

        Path firstRevisionCommit = Paths.get(rm.getCommitPath(de.prevModifyingRevisions.get(0)));
        Path commitedFile = firstRevisionCommit.resolve(fileName.path);

        List<String> data = FileUtils.readLines(commitedFile.toFile());

        for (int i = 1; i < de.prevModifyingRevisions.size(); i++) {
            String revisionHash = de.prevModifyingRevisions.get(i);
            Path commitPath = Paths.get(rm.getCommitPath(revisionHash));
            Path revisionPath = Paths.get(rm.getRevisionFile(revisionHash));

            Revision r = (Revision) Serializer.deserialize(revisionPath.toString());
            if (r.dirStructure.get(fileName.toString()).status == DELETED) {
                data.clear();
            } else {
                data = applyPatch(commitPath.resolve(fileName.path), data);
            }
        }

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

}
