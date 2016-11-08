package au.java.tracker.client;

import au.java.tracker.protocol.FileDescriptor;
import au.java.tracker.protocol.TrackerProtocol;

import java.io.Serializable;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by andy on 11/8/16.
 */
class DownloadingFileDescriptor extends FileDescriptor implements Serializable {
    final class PART_TYPE {
        static final int NOT_DOWNLOADED = 0;
        static final int IS_DOWNLOADING = 1;
        static final int IS_DOWNLOADED = 2;
    }

    final AtomicBoolean isBeingDownloaded;
    final String outputPath;
    int partsNumber;
    final AtomicInteger[] partsMap;
    final int lastPartSize;
    private final AtomicInteger downloadedPartsNumber = new AtomicInteger(0);

    DownloadingFileDescriptor(FileDescriptor desc, Set<Integer> downloadedParts, String outputPath) {
        super(desc);

        this.outputPath = outputPath;
        int floorParts = (int) (size / TrackerProtocol.CHUNK_SIZE);

        boolean noRem = size % TrackerProtocol.CHUNK_SIZE == 0;
        partsNumber = floorParts + (noRem ? 0 : 1);
        if (partsNumber == 0) {
            lastPartSize = 0;
        } else {
            lastPartSize = noRem ? TrackerProtocol.CHUNK_SIZE :
                    (int) (size % TrackerProtocol.CHUNK_SIZE);
        }

        partsMap = new AtomicInteger[partsNumber]; // atomicity restricts action repetitions over different parts
        for (int i = 0; i < partsNumber; i++) {
            partsMap[i].set(PART_TYPE.NOT_DOWNLOADED);
        }
        isBeingDownloaded = new AtomicBoolean(false);
    }

    boolean isCompletelyDownloaded() {
        return partsNumber == downloadedPartsNumber.intValue();
    }

    int getPartSize(int part) {
        if (part < 0 || part >= partsNumber) {
            return -1;
        }
        if (part == partsNumber - 1) {
            return lastPartSize;
        }
        return TrackerProtocol.CHUNK_SIZE;
    }
}