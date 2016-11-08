package au.java.tracker.protocol;
import au.java.tracker.protocol.util.IpValidator;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

import static au.java.tracker.protocol.util.IpValidator.IP_BYTES;

/**
 * Created by andy on 11/7/16.
 */
@Data
public class ClientDescriptor {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientDescriptor.class);

    private final Byte ip[];
    private final int port;

    public ClientDescriptor(String ip, int port) throws Exception {
        if (!IpValidator.validateIp(ip)) {
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
