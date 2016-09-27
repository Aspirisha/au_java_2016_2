package au.java.rush.utils;

import com.google.common.base.Charsets;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashCodes;
import com.google.common.hash.Hashing;
import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import static java.nio.file.FileVisitResult.CONTINUE;

/**
 * Created by andy on 9/25/16.
 */
public class FileHasher {
    private static class DirectoryVisitor extends SimpleFileVisitor<Path>  {
        String hashesConcat = "";
        @Override
        public FileVisitResult visitFile(Path file,
                                         BasicFileAttributes attr) {
            if (attr.isRegularFile() && !attr.isDirectory()) {
                HashCode md5 = null;
                try {
                    hashesConcat += Files.hash(file.toFile(), Hashing.md5());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return CONTINUE;
        }
    }

    public static HashCode getDirectoryOrFileHash(String dir) throws IOException {
        DirectoryVisitor dv = new DirectoryVisitor();
        java.nio.file.Files.walkFileTree(Paths.get(dir), dv);

        return Hashing.md5().hashString(dv.hashesConcat);
    }
}
