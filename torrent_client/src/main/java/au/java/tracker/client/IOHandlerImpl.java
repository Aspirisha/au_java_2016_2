package au.java.tracker.client;

import au.java.tracker.protocol.FileDescriptor;
import com.google.common.base.Strings;

import java.util.List;
import java.util.Scanner;

/**
 * Created by andy on 11/7/16.
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
        System.out.println("  Available commands:");
        System.out.println("\tupload <file_path> : upload file");
        System.out.println("\tdownload <file id> <output path>: download file with given id");
        System.out.println("\tlist : list files uploaded to tracker");
        System.out.println("\thelp : show this help");
        System.out.println("\texit : exit program");
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
    public void showFileList(List<FileDescriptor> l) {
        printHeader("List of uploaded files");

        for (FileDescriptor fd : l) {
            System.out.format("\t%d\t%s\t%d\n", fd.getId(), fd.getName(), fd.getSize());
        }

        printFooter();
    }

    private void printFileDescr(FileDescriptor fd) {
        System.out.println("\tfile name: " + fd.getName());
        System.out.println("\tfile id: " + fd.getId());
        System.out.println("\tfile size: " + fd.getSize());
    }

    private void printDownloadedFileDescr(DownloadingFileDescriptor fd) {
        printFileDescr(fd);
        System.out.println("\tfile path: " + fd.outputPath);
    }

    @Override
    public void onSuccessfulUpload(DownloadingFileDescriptor fd) {
        printHeader("File uploaded succesfully");
        printDownloadedFileDescr(fd);
        printFooter();
    }

    @Override
    public void onFileDownloaded(DownloadingFileDescriptor fd,
                                 FileDownloadResult result) {
        printHeader("File download result");

        switch (result) {
            case FILE_IS_DOWNLOADED:
                System.out.format("\tstatus: SUCCESS\n");
                printDownloadedFileDescr(fd);
                break;
            case FILE_IS_ALREADY_DOWNLOADING:
                break;
            case ERROR_OPENING_OUTPUT_FILE:
                System.out.format("\tstatus: FAILURE: failed to open output path\n");
                printFileDescr(fd);
                break;
            case CANT_CONNECT_TO_SERVER:
                System.out.format("\tstatus: FAILURE: couldn't connect to tracker server\n");
                printFileDescr(fd);
                break;
            case INTERRUPTED:
                break;
        }
        printFooter();
    }

    @Override
    public void fileMovingNotSupported(DownloadingFileDescriptor fd) {
        printHeader("Download info");
        System.out.format("\tFile with id %d is already tracked.\n", fd.getId());
        System.out.println("\tChanging output path is currently not supported.");
        System.out.println("\tFile will be downloaded to " + fd.outputPath);
        printFooter();
    }

}
