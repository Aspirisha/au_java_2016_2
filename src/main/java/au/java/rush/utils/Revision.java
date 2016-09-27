package au.java.rush.utils;

import difflib.Patch;
import difflib.PatchFailedException;

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
        DELETED
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

    // creates revision from index
    public Revision(Path repoRoot, String message) throws IOException {
        BranchManager bm = new BranchManager(repoRoot.toString());
        IndexManager im = new IndexManager(repoRoot.toString());
        this.parentHash = bm.getHeadRevisionHash();
        this.repoRoot = repoRoot;
        this.message = message;
        ownHash = FileHasher.getDirectoryOrFileHash(im.getIndexDir()).toString();
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
            parent = (Revision) Serializer.deserialize(String.valueOf(revisionsPath.resolve(parentHash)));
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
                        updatedElement = parent.dirStructure.get(key);
                        updatedElement.status = deletedFiles.contains(key) ? DELETED : UNMODIFIED;
                    }
                    dirStructure.put(key, updatedElement);
                    break;
                case DELETED: // if this file was created, then it's already added as new
                    break;
            }
        }
    }

    private void initTransientFields() {
        rm = new RepoManager(repoRoot.toString());
        revisionsPath = Paths.get(rm.getRevisionsDir());
    }

    private void createNewDirectoryStructure() {
        String indexRoot = rm.getIndexDir();

        class DirectoryStructureCreator extends SimpleFileVisitor<Path> {
            @Override
            public FileVisitResult visitFile(Path file,
                                             BasicFileAttributes attr) {
                if (attr.isRegularFile() && !attr.isDirectory()) {
                    RepoManager.PathRelativeToRoot p = rm.getFilePathRelativeToRoot(
                            file.toString());
                    dirStructure.put(p.toString(), new DirElement(ElementStatus.NEW, new ArrayList<String>()));
                }
                return CONTINUE;
            }
        }

        DirectoryStructureCreator pf = new DirectoryStructureCreator();
        try {
            Files.walkFileTree(Paths.get(indexRoot), pf);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void checkout() {

    }

    public List<String> getFileContents(RepoManager.PathRelativeToRoot fileName) {
        if (!dirStructure.containsKey(fileName.toString()))
            return null;

        DirElement de = dirStructure.get(fileName.toString());

        Path firstRevision = revisionsPath.resolve(de.prevModifyingRevisions.get(0));
        List<String> data = new LinkedList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(firstRevision.toString())))
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
            Path revPath = Paths.get(rm.getRevisionsDir()).resolve(de.prevModifyingRevisions.get(i));
            data = applyPatch(revPath, data);
        }

        return data;
    }

    static List<String> applyPatch(Path patchFile, List<String> target) {
        Patch restored;
        try {
            FileInputStream fis = new FileInputStream(patchFile.toString());
            ObjectInputStream ois = new ObjectInputStream(fis);
            restored = (Patch) ois.readObject();
            ois.close();
            fis.close();
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

        try {
            return restored.applyTo(target);
        } catch (PatchFailedException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void writeObject(ObjectOutputStream oos)
            throws IOException {
        // default serialization
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
