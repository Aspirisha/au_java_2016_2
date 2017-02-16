package cssort.client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.AsynchronousSocketChannel;

/**
 * Created by andy on 2/15/17.
 */
public class Main {
    // arguments: N \delta X architecture
    public static void main(String[] args) throws IOException {
        AsynchronousSocketChannel channel = AsynchronousSocketChannel.open();
        SocketAddress serverAddr = new InetSocketAddress("localhost", 8989);
    }
}
