package au.java.tracker.protocol;
import au.java.tracker.protocol.util.IpValidator;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static au.java.tracker.protocol.util.IpValidator.IP_BYTES;

/**
 * Created by andy on 11/7/16.
 */
@Data
public class ClientDescriptor implements Comparable<ClientDescriptor> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientDescriptor.class);

    private final List<Integer> ip;
    private final int port;

    public ClientDescriptor(String ip, int port) throws Exception {
        if (!IpValidator.validateIp(ip)) {
            LOGGER.info("Passed wrong ip address: " + ip);
            throw new Exception("Invalid ip: " + ip);
        }
        this.ip = Arrays.stream(ip.split("\\."))
                .map(Integer::valueOf)
                .collect(Collectors.toList());

        this.port = port;
    }

    public ClientDescriptor(List<Integer> ip, int port) throws Exception {
        if (ip.size() != IP_BYTES) {
            throw new Exception("Invalid ip");
        }

        this.ip = ip;
        this.port = port;
    }

    public String getStringIp() {
        return String.format(StringUtils.repeat("%d.", IP_BYTES - 1).concat("%d"), ip.toArray());
    }

    @Override
    public int compareTo(ClientDescriptor o) {
        for (int i = 0; i < IP_BYTES; i++) {
            int diff = ip.get(i) - o.ip.get(i);
            if (diff != 0) {
                return diff < 0 ? -1 : 1;
            }
        }

        return 0;
    }
}
