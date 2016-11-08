package au.java.tracker.protocol;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.io.Serializable;

/**
 * Created by andy on 11/7/16.
 */
@Data
@RequiredArgsConstructor
public class FileDescriptor implements Serializable {
    protected final int id;
    protected final String name;
    protected final long size;

    public FileDescriptor(FileDescriptor other) {
        this(other.id, other.name, other.size);
    }
}
