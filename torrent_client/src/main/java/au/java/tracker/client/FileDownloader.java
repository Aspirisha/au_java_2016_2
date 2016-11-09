package au.java.tracker.client;

import au.java.tracker.protocol.ClientDescriptor;
import au.java.tracker.protocol.TrackerProtocol;
import au.java.tracker.protocol.util.TimeoutSocketConnector;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;

import static au.java.tracker.client.FileDownloadResult.*;

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
    private Socket serverSocket;
    private final TrackerClient.onDownloadFinishedListener listener;

    private static class FilePartDownloader implements Callable<Pair<Integer, byte[]>> {
        final int filePart;
        final ClientDescriptor clientDescriptor;
        final DownloadingFileDescriptor fd;

        FilePartDownloader(ClientDescriptor clientDescriptor, DownloadingFileDescriptor fd,
                           int filePart) {
            this.clientDescriptor = clientDescriptor;
            this.filePart = filePart;
            this.fd = fd;
        }

        @Override
        public Pair<Integer, byte[]> call() {
            try(Socket socket = TimeoutSocketConnector.tryConnectToServer(clientDescriptor.getStringIp(),
                    clientDescriptor.getPort())) {

                if (socket == null) {
                    return Pair.of(filePart, null);
                }

                TrackerProtocol.PeerClientProtocol p = TrackerProtocol.getPeerClientProtocol(socket);

                byte[] data = p.clientRequestGet(fd.getId(), filePart);

                if (data != null && data.length == fd.getPartSize(filePart)) {
                    return Pair.of(filePart, data);
                }
            } catch (IOException e) {
                LOGGER.error("", e);
            }
            return Pair.of(filePart, null);
        }
    }

    FileDownloader(DownloadingFileDescriptor fd, String serverIp,
                   TrackerClient.onDownloadFinishedListener listener) {
        this.serverIp = serverIp;
        this.fd = fd;
        this.listener = listener;
    }

    private FileDownloadResult prepareDownloadLoop() {
        serverSocket = TimeoutSocketConnector.tryConnectToServer(serverIp,
                TrackerProtocol.SERVER_PORT);
        try {
            p = TrackerProtocol.getClientToServerProtocol(serverSocket);
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
    public FileDownloadResult call() throws IOException {
        FileDownloadResult res = prepareDownloadLoop();
        if (null != res) {
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    LOGGER.error("", e);
                }
            }

            listener.onDownloadFinished(fd, res);
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
                Socket clientSocket = TimeoutSocketConnector.tryConnectToServer(
                        cd.getStringIp(), cd.getPort());

                if (null == clientSocket) {
                    continue;
                }

                TrackerProtocol.PeerClientProtocol pp = TrackerProtocol.getPeerClientProtocol(clientSocket);

                for (Integer part : pp.clientRequestStat(fd.getId())) {
                    if (!fd.partsMap[part].compareAndSet(
                            DownloadingFileDescriptor.PART_TYPE.NOT_DOWNLOADED,
                            DownloadingFileDescriptor.PART_TYPE.IS_DOWNLOADING)) {
                        continue;
                    }
                    // ok, we can now safely download this part
                    futures.add(partsDownloadExecutor.submit(
                            new FilePartDownloader(cd, fd, part)));
                }

                try {
                    clientSocket.close();
                } catch (IOException e) {
                    LOGGER.error("", e);
                }
            }

            if (!trySleep(futures)) {
                partsDownloadExecutor.shutdownNow();
                listener.onDownloadFinished(fd, INTERRUPTED);
                return exitDownload(INTERRUPTED);
            }
        }

        listener.onDownloadFinished(fd, FILE_IS_DOWNLOADED);
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
                fd.onPartDownloaded(result.getKey());
                return;
            }
            fd.partsMap[result.getKey()].set(
                    DownloadingFileDescriptor.PART_TYPE.NOT_DOWNLOADED);
        } catch (InterruptedException e) {
            LOGGER.error("", e);
            // This is unreachable since by contract passed future is done
        } catch (ExecutionException | IOException e) {
            LOGGER.error("", e);
            if (result != null) {
                fd.partsMap[result.getKey()].set(
                        DownloadingFileDescriptor.PART_TYPE.NOT_DOWNLOADED);
            }
        }

    }
}