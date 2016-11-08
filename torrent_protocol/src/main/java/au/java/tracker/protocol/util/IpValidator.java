package au.java.tracker.protocol.util;

/**
 * Created by andy on 11/7/16.
 */
public class IpValidator {
    public static final int IP_BYTES = 4;

    private static final String IPADDRESS_PATTERN =
            String.format("^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){%d}" +
                    "([01]?\\d\\d?|2[0-4]\\d|25[0-5])$", IP_BYTES - 1);

    public static boolean validateIp(String ip) {
        return ip.matches(IPADDRESS_PATTERN);
    }
}
