package cssort.client;

import cssort.common.*;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Slf4j
public class ClientController implements Runnable {
    final int n;
    final int x;
    final int delta;
    final Settings.Architecture arch;
    final CompleteListener completeListener;

    boolean finishedSuccesfull = true;

    public interface CompleteListener {
        void onComplete(Statistics.RunResult r);
    }

    protected static final Logger logger = LoggerFactory.getLogger(ClientController.class);
    AbstractClient client;

    public ClientController(int n, int x, int delta, Settings.Architecture arch, CompleteListener l) {
        this.n = n;
        this.x = x;
        this.delta = delta;
        this.arch = arch;
        completeListener = l;
    }

    @Override
    public void run() {
        switch (arch) {
            case TCP_CLIENT_PERSISTENT_SERVER_THREAD_PER_CLIENT:
            case TCP_CLIENT_PERSISTENT_SERVER_CACHING_THREAD_POOL:
                logger.debug("Using tcp persistent client");
                client = new TcpPersistentClient(n, delta, x);
                break;
        }

        long startTime = System.currentTimeMillis();
        List<Statistics.ServerRunResult> serverResults = client.run();
        if (serverResults == null) {
            finishedSuccesfull = false;
            completeListener.onComplete(null);
            return;
        }
        long clientRuntime = System.currentTimeMillis() - startTime;
        long averageSortTime = 0;
        long averageRequestTime = 0;

        for (Statistics.ServerRunResult r: serverResults) {
            averageRequestTime += r.getRequestTime();
            averageSortTime += r.getSortTime();
        }
        averageRequestTime /= serverResults.size();
        averageSortTime /= serverResults.size();

        Statistics.ServerRunResult averageServerResult = new Statistics.ServerRunResult(averageSortTime,
                averageRequestTime);
        Statistics.RunResult result = new Statistics.RunResult(averageServerResult, clientRuntime);
        completeListener.onComplete(result);
    }

}
