package au.java.tracker.server;

import java.util.Scanner;

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
}
