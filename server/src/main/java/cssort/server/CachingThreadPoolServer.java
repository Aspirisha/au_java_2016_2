package cssort.server;

import cssort.common.Settings;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.*;

/**
 * Created by andy on 2/18/17.
 */
public class CachingThreadPoolServer extends AbstractPersistentTcpServer {
    private ExecutorService threadPool = Executors.newCachedThreadPool();

    @Override
    void run() {
        logger.debug("CachingThreadPoolServer started");
        System.out.println("CachingThreadPoolServer is listening on port " + Integer.toString(Settings.SERVER_PORT));

        try (ServerSocket s = new ServerSocket(Settings.SERVER_PORT)) {
            s.setSoTimeout(3000); // this is set only for being able to
                                  // interrupt this loop fast enough when profiler asks to
            while (!Thread.interrupted()) {
                try {
                    Socket client = s.accept();
                    logger.debug("Client connected");
                    threadPool.submit(new ClientProcessor(client));
                } catch (SocketTimeoutException ignored) {}
            }
        } catch (IOException e) {
            logger.error("", e);
            e.printStackTrace();
        }
    }
}
