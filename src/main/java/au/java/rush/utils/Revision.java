package au.java.rush.utils;

import com.google.common.hash.HashCodes;
import com.google.common.hash.Hashing;
import difflib.Patch;
import difflib.PatchFailedException;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

import static au.java.rush.utils.Revision.ElementStatus.*;
import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.FileVisitResult.SKIP_SUBTREE;
import static javafx.scene.input.KeyCode.T;

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
    String parentHash;
    String message;
    HashMap<String, DirElement> dirStructure;
    transient Path repoRoot;
    transient RepoManager rm;
    transient Path revisionsPath;

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
    public Revision(Path repoRoot, String message) throws IOException {
        BranchManager bm = new BranchManager(repoRoot.toString());
        this.parentHash = bm.getHeadRevisionHash();
        this.repoRoot = repoRoot;
        this.message = message;
        String filesHash = FileHasher.getDirectoryOrFileHash(bm.getIndexDir(),
                Paths.get(bm.getIndexDir())).toString();

        // add dependency on parent revision
        ownHash = String.valueOf(Hashing.md5().hashString(filesHash +
                (parentHash == null ? "" : parentHash)));
        dirStructure = new HashMap<>();

        initTransientFields();
        // create directory structure
        if (parentHash == null) {
            createNewDirectoryStructure();
        } else {
            createNewDirectoryStructure();
            createDirectoryStructureFromParent();
        }
    }

    private void createDirectoryStructureFromParent() {
        Revision parent;
        try {
            parent = (Revision) Serializer.deserialize(rm.getRevisionFile(parentHash));
        } catch (IOException e) {
            e.printStackTrace();
            return;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return;
        }

        IndexManager im = new IndexManager(repoRoot.toString());
        HashSet<String> deletedFiles = new HashSet<>();
        try {
            deletedFiles = (HashSet<String>) Serializer.deserialize(im.getDeletedFilesFile());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        for (Map.Entry<String, DirElement> k : parent.dirStructure.entrySet()) {
            final String key = k.getKey();
            switch (k.getValue().status) {
                case NEW:
                case MODIFIED:
                case UNMODIFIED:
                    DirElement updatedElement = null;
                    if (dirStructure.containsKey(key)) {
                        updatedElement = dirStructure.get(key);
                        updatedElement.status = MODIFIED;
                    } else {
                        updatedElement = k.getValue();
                        updatedElement.status = deletedFiles.contains(key) ? DELETED : UNMODIFIED;
                    }
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
    }

    private void createNewDirectoryStructure() {
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
        try {
            Files.walkFileTree(indexRoot, pf);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void checkout() throws PatchFailedException, IOException, ClassNotFoundException {
        for (Map.Entry<String, DirElement> e : dirStructure.entrySet()) {
            String f = e.getKey();
            DirElement de = e.getValue();

            if (de.status == DELETED || de.status == DELETED_EARLIER) {
                Path p = Paths.get(rm.getFilePathAbsolute(f));

                if (!Files.exists(p)) {
                    continue;
                }

                try {
                    Files.delete(Paths.get(rm.getFilePathAbsolute(f)));
                } catch (DirectoryNotEmptyException x) {
                    x.printStackTrace();
                } catch (IOException x) {
                    x.printStackTrace();
                }

                continue;
            }
            List<String> fileContents = getFileContents(rm.getFilePathRelativeToRoot(f));

            try (Writer writer = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(repoRoot.resolve(f).toString()), "utf-8"))) {

                for (String s: fileContents)
                    writer.write(s);
            } catch (IOException ex) {
                // report
            }
        }
    }

    public List<String> getFileContents(RepoManager.PathRelativeToRoot fileName) throws IOException, ClassNotFoundException, PatchFailedException {
        if (!dirStructure.containsKey(fileName.toString()))
            return null;

        DirElement de = dirStructure.get(fileName.toString());

        Path firstRevisionCommit = Paths.get(rm.getCommitPath(de.prevModifyingRevisions.get(0)));
        List<String> data = new LinkedList<>();

        Path commitedFile = firstRevisionCommit.resolve(fileName.path);
        try (BufferedReader br = new BufferedReader(new FileReader(commitedFile.toString())))
        {
            String sCurrentLine;
            while ((sCurrentLine = br.readLine()) != null) {
                data.add(sCurrentLine);
            }

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        for (int i = 1; i < de.prevModifyingRevisions.size(); i++) {
            String revisionHash = de.prevModifyingRevisions.get(i);
            Path commitPath = Paths.get(rm.getCommitPath(revisionHash));
            Path revisionPath = Paths.get(rm.getRevisionFile(revisionHash));

            Revision r = (Revision) Serializer.deserialize(revisionPath.toString());
            if (r.dirStructure.get(fileName.toString()).status == DELETED) {
                data.clear();
            }
            data = applyPatch(commitPath.resolve(fileName.path), data);
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
