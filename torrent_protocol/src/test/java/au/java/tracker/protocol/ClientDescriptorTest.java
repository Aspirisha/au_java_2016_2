package au.java.tracker.protocol;

import org.junit.Test;

import static junit.framework.TestCase.fail;

/**
 * Created by andy on 11/7/16.
 */
public class ClientDescriptorTest {
    @Test
    public void testClientDescriptor() {
        String[] valid_ips = {"127.0.0.1", "255.255.255.255", "0.0.0.0", "12.33.255.99"};
        String[] invalid_ips = {"0.0.0.0.0", "0.0.0", "0.0.0.0.", "256.0.0.0", "Wow! Ip!",
        "1.2.3.09338"};

        try {
            for (String ip : valid_ips) {
                new ClientDescriptor(ip, (short) 8080);
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            fail("Should not throw exception on valid ip");
        }

        for (String ip : invalid_ips) {
            try {
                new ClientDescriptor(ip, (short) 8080);
            } catch (Exception e) {
                continue;
            }

            fail("Should throw on invalid ip " + ip);
        }
    }
}
