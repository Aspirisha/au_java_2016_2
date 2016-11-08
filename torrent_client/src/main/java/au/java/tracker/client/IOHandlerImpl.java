package au.java.tracker.client;

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

}
