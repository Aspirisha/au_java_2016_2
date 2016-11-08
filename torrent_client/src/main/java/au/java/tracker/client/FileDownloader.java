package au.java.tracker.client;

import au.java.tracker.protocol.ClientDescriptor;
import au.java.tracker.protocol.TrackerProtocol;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;

import static au.java.tracker.client.FileDownloadResult.*;
import static javafx.scene.input.KeyCode.L;

/**
 * Created by andy on 11/8/16.
 */
enum FileDownloadResult {
    FILE_IS_DOWNLOADED,
    FILE_IS_ALREADY_DOWNLOADING,
    ERROR_OPENING_OUTPUT_FILE,
    CANT_CONNECT_TO_SERVER,
    INTERRUPTED
}

class FileDownloader implements Callable<FileDownloadResult> {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileDownloader.class);
    private static final int MAX_DOWNLOAD_THREADS = 4;
    private static final int TIME_TO_SLEEP_BETWEEN_POLLING_SERVER_SECS = 60;
    private final String serverIp;
    private final DownloadingFileDescriptor fd;

    private ExecutorService partsDownloadExecutor;
    private RandomAccessFile outFile;
    private TrackerProtocol.ClientToServerProtocol p = null;

    private class FilePartDownloader implements Callable<Pair<Integer, byte[]>> {
        final int filePart;
        final ClientDescriptor clientDescriptor;
        final int expectedPartSize;

        FilePartDownloader(ClientDescriptor clientDescriptor, int filePart,
                           int expectedPartSize) {
            this.clientDescriptor = clientDescriptor;
            this.filePart = filePart;
            this.expectedPartSize = expectedPartSize;
        }

        @Override
        public Pair<Integer, byte[]> call() throws Exception {
            TrackerProtocol.PeerClientProtocol p =
                    TrackerProtocol.getPeerClientProtocol(clientDescriptor);

            byte[] data = p.clientRequestGet(fd.getId(), filePart);

            if (data != null && data.length == expectedPartSize) {
                return Pair.of(filePart, data);
            }
            return Pair.of(filePart, null);
        }
    }

    FileDownloader(DownloadingFileDescriptor fd, String serverIp) {
        this.serverIp = serverIp;
        this.fd = fd;
    }

    private FileDownloadResult prepareDownloadLoop() {
        try {
            p = TrackerProtocol.getClientToServerProtocol(serverIp);
        } catch (Exception e) {
            LOGGER.error("", e);
            return CANT_CONNECT_TO_SERVER;
        }

        if (!fd.isBeingDownloaded.compareAndSet(false, true))
            return FILE_IS_ALREADY_DOWNLOADING;

        partsDownloadExecutor = new ThreadPoolExecutor(0, MAX_DOWNLOAD_THREADS,
                60L, TimeUnit.SECONDS, new SynchronousQueue<>());

        try {
            outFile = new RandomAccessFile(fd.outputPath, "rw");
        } catch (FileNotFoundException e) {
            fd.isBeingDownloaded.compareAndSet(true, false);
            return ERROR_OPENING_OUTPUT_FILE;
        }
        return null;
    }

    @Override
    public FileDownloadResult call() {
        FileDownloadResult res = prepareDownloadLoop();
        if (null != res) {
            return exitDownload(res);
        }

        List<Future<Pair<Integer, byte[]>>> futures = new LinkedList<>();
        while (!fd.isCompletelyDownloaded()) {
            for (Future<Pair<Integer, byte[]>> f : futures) {
                if (!f.isDone()) {
                    continue;
                }
                processFinishedFuture(f);
            }

            for (ClientDescriptor cd : p.serverRequestSources(fd.getId())) {
                TrackerProtocol.PeerClientProtocol pp;
                try {
                    pp = TrackerProtocol.getPeerClientProtocol(cd);
                } catch (IOException e) {
                    continue;
                }

                for (Integer part : pp.clientRequestStat(fd.getId())) {
                    if (!fd.partsMap[part].compareAndSet(
                            DownloadingFileDescriptor.PART_TYPE.NOT_DOWNLOADED,
                            DownloadingFileDescriptor.PART_TYPE.IS_DOWNLOADING)) {
                        continue;
                    }
                    // ok, we can now safely download this part
                    futures.add(partsDownloadExecutor.submit(
                            new FilePartDownloader(cd, part, fd.lastPartSize)));
                }
            }

            if (!trySleep(futures)) {
                partsDownloadExecutor.shutdownNow();
                return exitDownload(INTERRUPTED);
            }
        }

        return exitDownload(FILE_IS_DOWNLOADED);
    }

    private FileDownloadResult exitDownload(FileDownloadResult result) {
        fd.isBeingDownloaded.set(false);
        return result;
    }

    /**
     * @return false if was interrupted during sleep
     */
    private boolean trySleep(List<Future<Pair<Integer, byte[]>>> futures) {
        try {
            Thread.sleep(TIME_TO_SLEEP_BETWEEN_POLLING_SERVER_SECS * 1000);
        } catch (InterruptedException e) {
            for (Future<Pair<Integer, byte[]>> f : futures) {
                if (!f.isDone()) {
                    f.cancel(true);
                    continue;
                }

                processFinishedFuture(f);
            }
            return false;
        }

        return true;
    }

    private void processFinishedFuture(Future<Pair<Integer, byte[]>> f) {
        Pair<Integer, byte[]> result = null;
        try {
            result = f.get();
            if (result.getValue() != null) {
                outFile.write(result.getValue(), TrackerProtocol.CHUNK_SIZE * result.getKey(),
                        result.getValue().length);
                fd.partsMap[result.getKey()].set(DownloadingFileDescriptor.PART_TYPE.IS_DOWNLOADED);
                fd.partsNumber++;
                return;
            }
            fd.partsMap[result.getKey()].set(
                    DownloadingFileDescriptor.PART_TYPE.NOT_DOWNLOADED);
        } catch (InterruptedException e) {
            LOGGER.error("", e);
            // TODO consistency
        } catch (ExecutionException e) {
            LOGGER.error("", e);
            // TODO consistency
        } catch (IOException e) {
            LOGGER.error("", e);
            fd.partsMap[result.getKey()].set(
                    DownloadingFileDescriptor.PART_TYPE.NOT_DOWNLOADED);
        }

    }
}