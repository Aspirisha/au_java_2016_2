package cssort.profiler;

import cssort.client.ClientController;
import cssort.common.Settings;
import cssort.common.Statistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by andy on 2/17/17.
 */
public class CaseRunner implements ClientController.CompleteListener {
    protected static final Logger logger = LoggerFactory.getLogger(ClientController.class);
    final int m;
    final int n;
    final int delta;
    final int clientArch;
    final int x;

    private AtomicInteger finishedClients = new AtomicInteger(0);
    private AtomicLong sumProcessTime = new AtomicLong(0);
    private AtomicLong sumRequestTime = new AtomicLong(0);
    private AtomicLong sumClientRuntime = new AtomicLong(0);
    private ArrayList<Thread> clientThreads = new ArrayList<>();

    CaseRunner(int m, int n, int delta, int x, int clientArch) {
        this.m = m;
        this.n = n;
        this.delta = delta;
        this.clientArch = clientArch;
        this.x = x;
    }

    @Override
    public void onComplete(Statistics.RunResult r) {
        sumProcessTime.addAndGet(r.getServerResult().getProcessTime());
        sumRequestTime.addAndGet(r.getServerResult().getRequestTime());
        sumClientRuntime.addAndGet(r.getClientRuntime());
        finishedClients.incrementAndGet();
    }


    class CaseResult {
        CaseResult(long averageProcessTime, long averageRequestTime, long averageClientRuntime) {
            this.averageClientRuntime = averageClientRuntime;
            this.averageRequestTime = averageRequestTime;
            this.averageProcessTime = averageProcessTime;
        }

        long averageProcessTime;
        long averageRequestTime;
        long averageClientRuntime;
    }

    public CaseResult run() throws IOException {
        for (int i = 0; i < m; i++) {
            Thread t = new Thread(new ClientController(n, x, delta, clientArch, this));
            t.setName(String.format("Client #%d", i));
            clientThreads.add(t);
            t.start();
        }
        for (Thread t : clientThreads) {
            for (;;) {
                try {
                    t.join();
                    break;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        return new CaseResult(sumProcessTime.get() / m,
                sumRequestTime.get() / m, sumClientRuntime.get() / m);
    }
}
