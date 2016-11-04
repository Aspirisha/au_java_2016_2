package au.java.sftp;

import com.google.common.collect.Range;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.softhouse.jargo.Argument;
import se.softhouse.jargo.CommandLineParser;
import se.softhouse.jargo.ParsedArguments;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.concurrent.*;

import static se.softhouse.jargo.Arguments.integerArgument;

/**
 * Created by andy on 10/12/16.
 */
public class Server {
    private static final Logger logger = LoggerFactory.getLogger(Server.class);
    private static final Logger userLogger = LoggerFactory.getLogger("usermsg");

    private final Thread listeningThread;
    private final ExecutorService executorService;

    private class ClientServerInstance implements Runnable {
        private final Socket clientSocket;

        ClientServerInstance(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {
            SftpProtocol.SftpServerProtocol p = SftpProtocol.getServerProtocol();

            try(DataOutputStream dos = new DataOutputStream(clientSocket.getOutputStream());
                DataInputStream dis = new DataInputStream(clientSocket.getInputStream())) {

                while (!p.isFinished()) {
                    int request = p.process(dis, dos);
                    switch (request) {
                        case SftpProtocol.REQUEST_GREET: {
                            System.out.println("Client connected");
                            break;
                        }
                        case SftpProtocol.REQUEST_BYE: {
                            System.out.println("Client disconnected");
                            break;
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    logger.error("", e);
                    System.out.println("Couldn't close client socket");
                }
            }
        }
    }

    public void stop() {
        userLogger.info("Stopping server...");
        listeningThread.stop();
        executorService.shutdownNow();
    }

    private Server(int port, int maxPoolSize, int corePoolSize) {
        final int queueSize = 100;

        logger.info("Starting server on port " + port);

        final BlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(queueSize);
        executorService = new ThreadPoolExecutor(
                corePoolSize, maxPoolSize, 1, TimeUnit.MINUTES, queue);

        listeningThread = new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(port)) {
                userLogger.info("Listening port " + port);
                while (true) {
                    try {
                        executorService.execute(new ClientServerInstance(serverSocket.accept()));
                    } catch (RejectedExecutionException rej) {
                        logger.info("Rejected connection with client");
                    } catch (IOException e) {
                        logger.error("", e);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        listeningThread.start();
    }

    public static void main(String[] args) {
        final String stopCommand = "stop";
        final String startCommand = "start";
        final String exitCommand = "exit";

        System.setProperty("app.workdir", System.getProperty("user.dir"));

        Server server = null;
        System.out.println("Commands: \n start [-p port] [-c core-pool-size] " +
                "[-m max-threads] \t starts server\n stop \t stop server\n exit \t exits this shell");
        Scanner sc = new Scanner(System.in);
        while (sc.hasNextLine()) {
            System.out.print("> ");
            String commandStr = sc.nextLine();
            logger.info("New command: " + commandStr);
            String[] s = commandStr.split(" ");

            switch (s[0]) {
                case startCommand:
                    if (null != server) {
                        System.out.println("Server is already running");
                        continue;
                    }
                    server = parseArgsAndStartServer(Arrays.asList(s).subList(1, s.length));
                    break;
                case stopCommand:
                    if (null == server) {
                        System.out.println("Server is not running");
                        continue;
                    }
                    server.stop();
                    server = null;
                    break;
                case exitCommand:
                    if (null != server) {
                        server.stop();
                        server = null;
                    }
                    System.out.println("Exiting...");
                    System.exit(0);
                case "":
                    continue;
                default:
                    System.out.format("Command should be either %s or %s\n",
                            startCommand, stopCommand);
                    break;
            }
        }
    }

    private static Server parseArgsAndStartServer(List<String> args) {
        final int MAX_MAX_THREADS = 1000;
        final int DEFAULT_MAX_THREADS = 100;
        final int MIN_MAX_THREADS = 10;
        final int MIN_PORT = 1024;

        Argument<Integer> corePoolSize = integerArgument("-c", "--core-pool-size")
                .description("Core amount of threads in pool")
                .metaDescription("<n>")
                .defaultValue(10)
                .limitTo(Range.closed(MIN_MAX_THREADS, MAX_MAX_THREADS))
                .build();

        Argument<Integer> maxPoolSize = integerArgument("-m", "--max-pool-size")
                .defaultValue(DEFAULT_MAX_THREADS)
                .description("Max thread pool size")
                .metaDescription("<n>")
                .limitTo(Range.closed(MIN_MAX_THREADS, MAX_MAX_THREADS))
                .build();

        Argument<Integer> portNumber = integerArgument("-p", "--port")
                .description("Port number to use")
                .metaDescription("<n>")
                .defaultValue(MIN_PORT)
                .limitTo(Range.closed(MIN_PORT, 2 * Short.MAX_VALUE - 1))
                .build();

        ParsedArguments arguments = CommandLineParser.withArguments(corePoolSize, maxPoolSize, portNumber)
                .programName("sftpserver").parse(args);

        int port = Optional.ofNullable(arguments.get(portNumber)).orElse(MIN_PORT);
        int coreSize = Optional.ofNullable(arguments.get(corePoolSize)).orElse(MIN_MAX_THREADS);
        int maxSize = Optional.ofNullable(arguments.get(maxPoolSize)).orElse(DEFAULT_MAX_THREADS);

        return new Server(port, maxSize, coreSize);
    }
}
