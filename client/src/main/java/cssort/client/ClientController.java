package cssort.client;

import cssort.common.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ClientController implements Runnable {
    final int n;
    final int x;
    final int delta;
    final int arch;
    final CompleteListener completeListener;

    public interface CompleteListener {
        void onComplete(Statistics.RunResult r);
    }

    protected static final Logger logger = LoggerFactory.getLogger(ClientController.class);
    AbstractClient client;

    public ClientController(int n, int x, int delta, int arch, CompleteListener l) {
        this.n = n;
        this.x = x;
        this.delta = delta;
        this.arch = arch;
        completeListener = l;
    }

    @Override
    public void run() {
        switch (arch) {
            case Settings.TCP_CLIENT_PERSISTENT:
                client = new TcpPersistentClient(n, delta, x);
                break;
        }

        long startTime = System.currentTimeMillis();
        List<Statistics.ServerRunResult> serverResults = client.run();
        long clientRuntime = System.currentTimeMillis() - startTime;
        long averageProcessTime = 0;
        long averageRequestTime = 0;

        for (Statistics.ServerRunResult r: serverResults) {
            averageRequestTime += r.getRequestTime();
            averageProcessTime += r.getProcessTime();
        }
        averageRequestTime /= serverResults.size();

        Statistics.ServerRunResult averageServerResult = new Statistics.ServerRunResult(averageProcessTime / serverResults.size(),
                averageRequestTime / serverResults.size());
        Statistics.RunResult result = new Statistics.RunResult(averageServerResult, clientRuntime);
        completeListener.onComplete(result);
    }

}
