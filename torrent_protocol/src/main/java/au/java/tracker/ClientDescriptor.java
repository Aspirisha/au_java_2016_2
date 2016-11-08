package au.java.tracker;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

/**
 * Created by andy on 11/7/16.
 */
@Data
public class ClientDescriptor {
    private static final int IP_BYTES = 4;
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientDescriptor.class);

    private final Byte ip[];
    private final short port;

    private static final String IPADDRESS_PATTERN =
            String.format("^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){%d}" +
                    "([01]?\\d\\d?|2[0-4]\\d|25[0-5])$", IP_BYTES - 1);

    public ClientDescriptor(String ip, short port) throws Exception {
        if (!ip.matches(IPADDRESS_PATTERN)) {
            LOGGER.info("Passed wrong ip address: " + ip);
            throw new Exception("Invalid ip: " + ip);
        }
        this.ip = Arrays.stream(ip.split(".")).map(Byte::valueOf).toArray(Byte[]::new);
        this.port = port;
    }

    public ClientDescriptor(byte[] ip, short port) throws Exception {
        if (ip.length != IP_BYTES) {
            throw new Exception("Invalid ip");
        }

        this.ip = new Byte[IP_BYTES];
        for (int i = 0; i < IP_BYTES; i++) {
            this.ip[i] = ip[i];
        }

        this.port = port;
    }

    public String getStringIp() {
        return String.format(StringUtils.repeat("%b.", IP_BYTES - 1).concat("%b"), (Object[]) ip);
    }

}
