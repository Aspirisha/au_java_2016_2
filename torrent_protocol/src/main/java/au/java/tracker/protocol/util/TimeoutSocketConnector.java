package au.java.tracker.protocol.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Socket;

/**
 * Created by andy on 11/8/16.
 */
public class TimeoutSocketConnector {
    private static Logger LOGGER = LoggerFactory.getLogger(TimeoutSocketConnector.class);

    public static Socket tryConnectToServer(String serverIp, int port) {
        final int WAIT_TIMOUT = 2000;

        final Socket[] s = new Socket[1];
        Thread socketThread=new Thread() {
            public void run() {
                try {
                    s[0] = new Socket(serverIp, port);
                }
                catch (Exception e) {
                    // don't care here
                }
            }
        };
        socketThread.start();

        try {
            socketThread.join(WAIT_TIMOUT);
        } catch (Exception e) {
            LOGGER.error("", e);
        }

        return s[0];
    }
}