package cssort.server;

import cssort.common.Settings;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by andy on 2/19/17.
 */
public class UdpFixedThreadPool extends AbstractUdpServer {
    private final ExecutorService requestThreadPool = Executors.newFixedThreadPool(20);

    UdpFixedThreadPool() {
        System.out.println("UdpFixedThreadPool is listening on port " + Integer.toString(Settings.SERVER_PORT));
    }

    @Override
    void onReceivedRequest(RequestProcessor p) {
        requestThreadPool.submit(p);
    }
}
