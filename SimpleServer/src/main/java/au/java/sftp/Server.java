package au.java.sftp;

import com.google.common.collect.Range;
import com.google.common.primitives.Shorts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.softhouse.jargo.Argument;
import se.softhouse.jargo.ArgumentBuilder;
import se.softhouse.jargo.CommandLineParser;
import se.softhouse.jargo.ParsedArguments;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.concurrent.*;

import static se.softhouse.jargo.Arguments.integerArgument;
import static se.softhouse.jargo.Arguments.optionArgument;

/**
 * Created by andy on 10/12/16.
 */
public class Server {
    private final Logger logger = LoggerFactory.getLogger(Server.class);

    private class ClientServerInstance implements Runnable {
        private final Socket clientSocket;

        ClientServerInstance(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {

        }
    }

    public void stop() {
        logger.info("Stopping server...");
    }

    private Server(int port, int maxPoolSize, int corePoolSize) {
        final int queueSize = 100;

        logger.info("Starting server on port " + port);

        final BlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(queueSize);
        ExecutorService executorService = new ThreadPoolExecutor(
                corePoolSize, maxPoolSize, 1, TimeUnit.MINUTES, queue);

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Listening port " + port);
            while (true) {
                try (Socket clientSocket = serverSocket.accept()) {
                    executorService.execute(new ClientServerInstance(clientSocket));
                } catch (RejectedExecutionException rej) {
                    logger.info("Rejected connection with client");
                } catch (IOException e) {
                    logger.error("", e);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        final String stopCommand = "stop";
        final String startCommand = "start";

        Server server = null;

        Scanner sc = new Scanner(System.in);
        while (sc.hasNextLine()) {
            String[] s = sc.nextLine().split(" ");
            if (s[0].equals(startCommand)) {
                if (null != server) {
                    System.out.println("Server is already running");
                    continue;
                }
                server = parseArgsAndStartServer(Arrays.asList(s).subList(1, s.length));
            } else if (s[0].equals(stopCommand)) {
                if (null == server) {
                    System.out.println("Server is not running");
                    continue;
                }
                server.stop();
                server = null;
            } else {
                System.out.format("Command should be either %s or %s\n",
                        startCommand, stopCommand);
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
