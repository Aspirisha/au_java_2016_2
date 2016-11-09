package au.java.tracker.client;

import au.java.tracker.protocol.ClientDescriptor;
import au.java.tracker.protocol.TrackerProtocol;
import au.java.tracker.protocol.util.TimeoutSocketConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.Socket;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by andy on 11/8/16.
 */
class Updater implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(Updater.class);
    private static final int UPDATE_INTERVAL_SECONDS = 4 * 60;
    private final IOHandler iohandler = new IOHandlerImpl();
    private final String serverIp;
    private final ClientDescriptor myDescriptor;
    private final FileListProvider listProvider;

    public Updater(String serverIp, ClientDescriptor myDescriptor,
                   FileListProvider fileListProvider) {
        this.serverIp = serverIp;
        this.myDescriptor = myDescriptor;
        listProvider = fileListProvider;
    }

    @Override
    public void run() {
        while (!Thread.interrupted()) {
            try (Socket serverSocket = TimeoutSocketConnector.tryConnectToServer(serverIp,
                    TrackerProtocol.SERVER_PORT)) {

                if (serverSocket == null) {
                    return;
                }

                TrackerProtocol.ClientToServerProtocol p = TrackerProtocol.getClientToServerProtocol(serverSocket);

                Set<Integer> fileIdSet = listProvider.listFiles();

                p.serverRequestUpdate(myDescriptor, fileIdSet);
            } catch (Exception e) {
                LOGGER.error("", e);
                iohandler.onErrorUpdating();
            }

            try {
                Thread.sleep(UPDATE_INTERVAL_SECONDS * 1000);
            } catch (InterruptedException e) {
                return;
            }
        }
    }
}
