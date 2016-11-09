package au.java.tracker.server;

import au.java.tracker.protocol.ClientDescriptor;
import au.java.tracker.protocol.FileDescriptor;
import com.google.common.base.Strings;

import java.util.Collection;
import java.util.Scanner;
import java.util.Set;

/**
 * Created by andy on 11/8/16.
 */
public class IOHandlerImpl implements IOHandler {
    private final Scanner sc = new Scanner(System.in);

    @Override
    public String readCommand() {
        if (!sc.hasNextLine()) {
            return null;
        }

        return sc.nextLine();
    }

    @Override
    public void onHelpRequested() {
        printHeader("");
        System.out.println("  Commands:");
        System.out.println("\t clients : list live clients");
        System.out.println("\t files : list uploaded files");
        System.out.println("\t exit : stop server and exit");
        printFooter();
    }

    private void printHeader(String header) {
        int prefixLength = (78 - header.length()) / 2;
        int suffixLength = 78 - prefixLength - header.length();
        System.out.format("/%s%s%s\\\n", Strings.repeat("-", prefixLength), header,
                Strings.repeat("-", suffixLength));
    }

    private void printFooter() {
        System.out.format("\\%s/\n", Strings.repeat("-", 78));
    }

    @Override
    public void listClients(Set<ClientDescriptor> aliveCLients) {
        printHeader("List of clients");

        for (ClientDescriptor cd : aliveCLients) {
            System.out.format("\t%s\t%d\n", cd.getStringIp(), cd.getPort());
        }
        printFooter();
    }

    @Override
    public void listFiles(Collection<FileDescriptor> files) {
        printHeader("List of files");

        for (FileDescriptor fd : files) {
            System.out.format("\t%d\t%s\t%d\n", fd.getId(), fd.getName(), fd.getSize());
        }
        printFooter();
    }
}
