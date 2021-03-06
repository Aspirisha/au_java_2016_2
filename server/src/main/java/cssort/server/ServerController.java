package cssort.server;


import com.google.common.collect.Range;
import cssort.common.Settings;
import cssort.common.Settings.Architecture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.softhouse.jargo.Argument;
import se.softhouse.jargo.ArgumentException;
import se.softhouse.jargo.CommandLineParser;
import se.softhouse.jargo.ParsedArguments;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static java.lang.Thread.sleep;
import static se.softhouse.jargo.Arguments.booleanArgument;
import static se.softhouse.jargo.Arguments.helpArgument;
import static se.softhouse.jargo.Arguments.integerArgument;

/**
 * Created by andy on 2/15/17.
 */
public class ServerController {
    private static final Logger logger = LoggerFactory.getLogger(ServerController.class);
    private AbstractServer server = null;
    private volatile Architecture arch = null;
    private volatile int serverVersion = 0;

    private static Map<Integer, Architecture> idToArch;
    static {
        idToArch = new HashMap<>();
        idToArch.put(0, Architecture.TCP_CLIENT_PERSISTENT_SERVER_THREAD_PER_CLIENT);
        idToArch.put(1, Architecture.TCP_CLIENT_PERSISTENT_SERVER_CACHING_THREAD_POOL);
        idToArch.put(2, Architecture.TCP_CLIENT_PERSISTENT_SERVER_NON_BLOCKING);
        idToArch.put(3, Architecture.TCP_CLIENT_SPAWNING_SERVER_SINGLE_THREADED_SERIAL);
        idToArch.put(4, Architecture.TCP_CLIENT_PERSISTENT_SERVER_ASYNCHRONOUS);
        idToArch.put(5, Architecture.UDP_CLIENT_THREAD_PER_REQUEST);
        idToArch.put(6, Architecture.UDP_CLIENT_FIXED_THREAD_POOL);
    }
    public static void main(String[] args) {
        Argument<?> helpArg = helpArgument("-h", "--help"); //Will throw when -h is encountered
        Argument<Integer> archArg = integerArgument("-a", "--architecture")
                .description("Architecture to use. Following architectures are supported:\n" +
                        "0: client opens tcp socket once and sends requests until is done; server creates" +
                        "one thread per every client;\n" +
                        "1: client opens tcp socket once and sends requests until is done; server uses" +
                        "caching thread pool with one task per client;\n" +
                        "2: client opens tcp socket once and sends requests until is done; server performs" +
                        "non-blocking processing: every single sort request is processed as task in fixed" +
                        "size thread pool;\n" +
                        "3: client opens tcp socket for every sort request; server is single threaded with" +
                        "serial request processing;\n" +
                        "4: tcp connection; server performs asynchronous processing of incoming requests;\n" +
                        "5: udp connection; every sort request spawns new thread;\n" +
                        "6: udp connection; sort requests are processed in fixed sie thread pool")

                .metaDescription("<arch>")
                .defaultValue(0)
                .limitTo(Range.closed(0, 6))
                .build();
        Argument<Boolean> withProfilerArg = booleanArgument("-p", "--with-profiler")
                .description(String.format("If true, listens to profiler commands on port %d", Settings.SERVER_PORT_FOR_PROFILER))
                .defaultValue(true)
                .build();

        ParsedArguments arguments = null;

        try {
            arguments = CommandLineParser.withArguments(
                    archArg, withProfilerArg).andArguments(helpArg)
                    .programName("tracker-client").parse(args);
        } catch (ArgumentException e) {
            System.out.println(e.getMessageAndUsage());
            System.exit(1);
        }

        Integer architectureId = Optional.ofNullable(arguments.get(archArg)).orElse(0);
        Architecture arch = idToArch.get(architectureId);

        ServerController serverContoller = new ServerController(arch);
        serverContoller.start();
    }

    private ServerController(Architecture arch) {
        this.arch = arch;
    }

    private void start() {
        Thread serverRunner = new Thread(() -> {
            while (true) {
                switch (arch) {
                    case TCP_CLIENT_PERSISTENT_SERVER_THREAD_PER_CLIENT:
                        server = new ThreadPerClientServerOldIO();
                        logger.debug("Creating tcp server with 1 thread per client");
                        break;
                    case TCP_CLIENT_PERSISTENT_SERVER_CACHING_THREAD_POOL:
                        server = new CachingThreadPoolServerOldIO();
                        logger.debug("Creating tcp server with caching thread pool");
                        break;
                    case TCP_CLIENT_PERSISTENT_SERVER_NON_BLOCKING:
                        server = new NonBlockingFixedThreadPoolServer();
                        logger.debug("Creating tcp non-blocking server");
                        break;
                    case TCP_CLIENT_SPAWNING_SERVER_SINGLE_THREADED_SERIAL:
                        server = new SingleThreadedServer();
                        logger.debug("Creating tcp single threaded server");
                        break;
                    case TCP_CLIENT_PERSISTENT_SERVER_ASYNCHRONOUS:
                        server = new TcpAsyncServer();
                        logger.debug("Creating tcp async server");
                        break;
                    case UDP_CLIENT_THREAD_PER_REQUEST:
                        server = new UdpThreadPerClient();
                        logger.debug("Creating udp thread per client server");
                        break;
                    case UDP_CLIENT_FIXED_THREAD_POOL:
                        server = new UdpFixedThreadPool();
                        logger.debug("Creating udp thread per client server");
                        break;
                }
                serverVersion++;
                // this returns only when current thread interrupted
                server.run();
            }
        });

        serverRunner.start();

        try (ServerSocket s = new ServerSocket(Settings.SERVER_PORT_FOR_PROFILER)) {
            while (true) {
                try (Socket client = s.accept();
                     DataInputStream dis = new DataInputStream(client.getInputStream());
                     DataOutputStream dos = new DataOutputStream(client.getOutputStream())) {
                    String newArchitecture = dis.readUTF();
                    Architecture newArch = Architecture.fromString(newArchitecture);
                    if (newArch == null) {
                        logger.error("Malformed architecture; using default (tcp thread per client)");
                        newArch = Architecture.TCP_CLIENT_PERSISTENT_SERVER_THREAD_PER_CLIENT;
                    }

                    if (arch == newArch) {
                        dos.writeBoolean(true);
                        continue;
                    }
                    arch = newArch;
                    int oldServerVersion = serverVersion;
                    serverRunner.interrupt();
                    do {
                        try {
                            sleep(300);
                        } catch (InterruptedException ignored) {
                        }
                    } while (serverVersion == oldServerVersion);

                    dos.writeBoolean(true);
                } catch (IOException e) {
                    logger.debug("Profiler probably disconnected");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            serverRunner.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
