package cssort.server;

import cssort.common.Settings;

/**
 * Created by andy on 2/19/17.
 */
public class UdpThreadPerClient extends AbstractUdpServer {

    UdpThreadPerClient() {
        System.out.println("UdpThreadPerClient is listening on port " + Integer.toString(Settings.SERVER_PORT));
    }

    @Override
    void onReceivedRequest(RequestProcessor p) {
        new Thread(p).start();
    }
}
