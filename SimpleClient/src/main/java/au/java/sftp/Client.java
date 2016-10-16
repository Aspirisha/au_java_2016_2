package au.java.sftp;

import com.google.common.base.Strings;
import javafx.util.Pair;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.*;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Created by andy on 10/16/16.
 */
public class Client {
    private static final Logger logger = LoggerFactory.getLogger(Client.class);
    private final int MIN_PORT = 1024;

    private Socket socket = null;
    private DataOutputStream dos = null;
    private DataInputStream dis = null;

    private SftpProtocol.SftpClientProtocol protocol = SftpProtocol.getClientProtocol();
    private ExecutorService executorService;
    private  ArgumentParser parser = null;


    public Client() {
        executorService = Executors.newSingleThreadExecutor();

        parser = ArgumentParsers.newArgumentParser("", false)
                .defaultHelp(true)
                .description("Simple ftp client");
        Subparsers subparsers = parser.addSubparsers().help("sub-command help");

        Subparser parserConnect = subparsers.addParser("connect").help("connect to host")
                .setDefault("func", new ConnectCommand());
        parserConnect.addArgument("-p", "--port")
                .metavar("port")
                .type(Integer.class)
                .setDefault(MIN_PORT)
                .help("Port number");
        parserConnect.addArgument("host")
                .metavar("host")
                .type(String.class)
                .help("Host name");

        subparsers.addParser("disconnect").help("disconnect from host")
                .setDefault("func", new DisconnectCommand());

        subparsers.addParser("exit").help("Exit, closing current connection, if opened")
                .setDefault("func", new ExitCommand());

        Subparser parserGet = subparsers.addParser("get")
                .help("get file from server")
                .setDefault("func", new GetCommand());
        parserGet.addArgument("path")
                .type(String.class)
                .metavar("path")
                .help("Server path to file");
        parserGet.addArgument("output")
                .type(String.class)
                .metavar("output")
                .help("Path where file will be saved");


        Subparser parserList = subparsers.addParser("list")
                .help("list files in server directory")
                .setDefault("func", new ListCommand());

        parserList.addArgument("path")
                .type(String.class)
                .metavar("path")
                .help("Server path");

        parser.printHelp();
    }

    public static void main(String[] args) {
        System.setProperty("app.workdir", System.getProperty("user.dir"));

        Scanner sc = new Scanner(System.in);

        Client c = new Client();
        System.out.print("> ");
        while (sc.hasNextLine()) {

            String commandStr = sc.nextLine();
            if (!commandStr.isEmpty()) {
                logger.info("New command: " + commandStr);
                c.processCommand(commandStr);
            }
            System.out.print("> ");
        }
    }

    void processCommand(String command) {
        String[] args = command.split(" ");

        try {
            Namespace ns = parser.parseArgs(args);
            ((Command) ns.get("func")).execute(ns);
        } catch (ArgumentParserException e) {
            parser.handleError(e);
        }
    }


    private interface Command {
       void execute(Namespace n);
    }

    private class ExitCommand implements Command {
        @Override
        public void execute(Namespace n) {
            DisconnectCommand dc = new DisconnectCommand();
            dc.execute(n);
            executorService.shutdown();
            System.exit(0);
        }
    }

    private class ConnectCommand implements Command {

        @Override
        public void execute(Namespace n) {
            executorService.execute(() -> {
                String host = n.getString("host");
                int port = Optional.ofNullable(n.getInt("port")).orElse(MIN_PORT);

                System.out.format("Connecting to host %s using port %d\n", host, port);
                if (host == null) {
                    System.out.print("Host not specified\n> ");
                }

                try {
                    Client.this.socket = new Socket(host, port);
                    Client.this.dis = new DataInputStream(socket.getInputStream());
                    Client.this.dos = new DataOutputStream(socket.getOutputStream());
                } catch (UnknownHostException e) {
                    logger.info("", e);
                    System.out.format("Couldn't resolve host name %s\n", host);
                } catch (IOException e) {
                    e.printStackTrace();
                    if (socket != null) {
                        try {
                            socket.close();
                        } catch (IOException e1) {
                            logger.error("", e);
                        }
                    }
                    return;
                }
                System.out.println("Connected successfully");

                try {
                    protocol.greet(dos);
                    System.out.println(dis.readUTF());
                } catch (IOException e) {
                    logger.error("", e);
                }
                System.out.print("> ");
            });
        }
    }

    private class ListCommand implements Command {

        @Override
        public void execute(Namespace n) {
            executorService.execute(() -> {
                if (socket == null) {
                    System.out.print("Can't execute list command: not connected to host\n> ");
                    return;
                }
                List<Pair<String, Boolean>> res = null;
                try {
                    res = protocol.requestList(dis, dos, n.getString("path"));
                } catch (IOException e) {
                    System.out.print("Error getting file list\n> ");
                    logger.error("", e);
                    return;
                }
                System.out.format("Listing files in %s. \nTotal files number %d\n",
                        n.getString("path"), res.size());
                System.out.println(Strings.repeat("-", 40));
                res.stream().forEach(p -> System.out.println(p.getKey()));
                System.out.println(Strings.repeat("-", 40));
                System.out.print("> ");
            });
        }
    }

    private class DisconnectCommand implements Command {

        @Override
        public void execute(Namespace _) {
            executorService.execute(() -> {
                if (socket == null) {
                    System.out.print("Can't disconnect: not connected to host\n> ");
                    return;
                }

                System.out.println("Disconnecting from host...");
                try {
                    protocol.farewell(dos);
                    socket.close();
                } catch (IOException e) {
                    logger.error("", e);
                    e.printStackTrace();
                }
                socket = null;
                dis = null;
                dos = null;
                System.out.print("> ");
            });
        }
    }

    private class GetCommand implements Command {

        @Override
        public void execute(Namespace n) {
            executorService.execute(() -> {
                if (socket == null) {
                    System.out.print("Can't get file: not connected to host\n> ");
                    return;
                }
                byte[] res = null;
                final String file = n.getString("path");
                try {
                    res = protocol.requestFile(dis, dos, file);
                } catch (IOException e) {
                    System.out.print("Error getting file\n> ");
                    logger.error("", e);
                    return;
                }
                System.out.println("Read file " + file);
                System.out.println("Size is " + res.length);

                String storePath = n.getString("output");
                File out = FileUtils.getFile(storePath);
                try {
                    System.out.println("Saving file to " + out.getCanonicalPath());
                } catch (IOException e) {
                    e.printStackTrace();
                }

                try {
                    FileUtils.writeByteArrayToFile(FileUtils.getFile(storePath), res);
                } catch (IOException e) {
                    logger.error("", e);
                    System.out.println("Failed to write destination file");
                }
                System.out.print("> ");
            });
        }
    }
}
