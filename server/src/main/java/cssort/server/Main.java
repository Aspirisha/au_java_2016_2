package cssort.server;


import cssort.common.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

import static java.lang.System.exit;

/**
 * Created by andy on 2/15/17.
 */
public class Main {
    protected static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        AbstractServer server = null;

        if (args.length < 1) {
            System.out.println("Server architecture not provided");
            return;
        }

        int arch = Integer.valueOf(args[0]);
        switch (arch) {
            case Settings.TCP_SERVER_THREAD_PER_CLIENT:
                server = new ThreadPerClientServer();
                logger.debug("Creating tcp server with 1 thread per client");
                break;
        }

        server.run();
    }
}
