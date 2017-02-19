package cssort.client;

import cssort.common.Statistics;
import cssort.common.Util;
import cssort.protocol.ClientServerProtocol;
import lombok.extern.slf4j.Slf4j;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by andy on 2/15/17.
 */
@Slf4j
public abstract class AbstractClient {
    final int N;
    final long delta;
    final int X;
    final InetAddress serverAddress;
    long lastResponceTimestamp = -1;

    AbstractClient(int N, int deltaMillis, int X, InetAddress serverAddress) {
        this.N = N;
        this.delta = deltaMillis;
        this.X = X;
        this.serverAddress = serverAddress;
    }

    void sleep() {
        long sleptTime = System.currentTimeMillis() - lastResponceTimestamp;
        while (sleptTime < delta) {
            try {
                Thread.sleep(delta - sleptTime);
            } catch (InterruptedException e) {
                return;
            }
            sleptTime = System.currentTimeMillis() - lastResponceTimestamp;
        }
    }

    List<Integer> generateInput() {
        List<Integer> a = new ArrayList<>(N);
        Random r = new Random(System.nanoTime());
        for (int i = 0; i < N; i++) {
            a.add(r.nextInt());
        }
        return a;
    }

    abstract List<Statistics.ServerRunResult> run();
}
