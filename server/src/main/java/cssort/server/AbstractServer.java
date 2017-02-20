package cssort.server;

import cssort.common.Util;
import cssort.protocol.ClientServerProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by andy on 2/16/17.
 */
public abstract class AbstractServer {
    static final int CHECK_INTERRUPT_PERIOD_MILLIS = 3000;
    static final Logger logger = LoggerFactory.getLogger(ServerController.class);

    void sort(ArrayList<Integer> data) {
        for (int i = 0; i < data.size(); i++) {
            int minIdx = i;
            for (int j = i + 1; j < data.size(); j++) {
                if (data.get(j) < data.get(minIdx)) {
                    minIdx = j;
                }
            }

            Collections.<Integer>swap(data, minIdx, i);
        }
    }

    abstract void run();
}
