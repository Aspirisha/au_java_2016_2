package cssort.client;

import cssort.common.*;
import cssort.protocol.ProfilerClientProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.List;

import static java.lang.System.in;

public class ClientController {
    protected ProfilerClientProtocol.ProfilerToClientInfo execInfo;
    protected static final Logger logger = LoggerFactory.getLogger(ClientController.class);
    AbstractClient client;

    protected void EstablishConnectionWithProfiler() throws IOException {
        try (Socket s = new Socket("localhost", Settings.PROFILER_PORT);
             DataInputStream dis = new DataInputStream(s.getInputStream());
             DataOutputStream dos = new DataOutputStream(s.getOutputStream())) {

            logger.debug("Connected to profiler via socket");
            s.setSoTimeout(5000);

            int size = dis.readInt();
            byte[] buf = new byte[size];

            int readBytes = 0;
            do {
                readBytes += in.read(buf, readBytes, buf.length - readBytes);
            } while (readBytes < buf.length);
            execInfo = ProfilerClientProtocol.ProfilerToClientInfo.parseFrom(buf);

            switch (execInfo.getArch()) {
                case Settings.TCP_CLIENT_PERSISTENT_CACHING_THREAD_POOL:
                case Settings.TCP_CLIENT_PERSISTENT_FIXED_THREAD_POOL:
                case Settings.TCP_CLIENT_PERSISTENT_THREAD_PER_CLIENT:
                    client = new TcpPersistentClient(execInfo.getN(),
                            execInfo.getDelta(), execInfo.getX());
                    break;
            }

            long startTime = System.currentTimeMillis();
            List<ServerRunResult> result = client.run();
            long clientRuntime = System.currentTimeMillis() - startTime;
            long averageProcessTime = 0;
            long averageRequestTime = 0;

            for (ServerRunResult r: result) {
                averageRequestTime += r.requestTime;
                averageProcessTime += r.getProcessTime();
            }
            averageRequestTime /= result.size();

            ProfilerClientProtocol.ClientToProfiler.Builder b = ProfilerClientProtocol.ClientToProfiler.newBuilder();
            b.setAverageProcessTime(averageProcessTime / result.size());
            b.setAverageRequestTime(averageRequestTime / result.size());
            b.setClientRunTime(clientRuntime);

            ProfilerClientProtocol.ClientToProfiler msg = b.build();

            dos.writeInt(msg.getSerializedSize());
            msg.writeTo(dos);
        }
    }
}
