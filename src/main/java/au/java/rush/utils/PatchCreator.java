package au.java.rush.utils;

import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import static java.nio.file.FileVisitResult.CONTINUE;

/**
 * Created by andy on 9/26/16.
 */
public class PatchCreator extends SimpleFileVisitor<Path> {
    public final RepoManager manager;
    public final Revision revision;

    public PatchCreator(RepoManager manager, Revision revision) {
        this.manager = manager;
        this.revision = revision;
    }
    // Print information about
    // each type of file.
    @Override
    public FileVisitResult visitFile(Path file,
                                     BasicFileAttributes attr) {
        if (attr.isRegularFile() && !attr.isDirectory()) {
            System.out.format("Regular file: %s ", file);
        }
        return CONTINUE;
    }
}
