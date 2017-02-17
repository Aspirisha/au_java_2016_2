package cssort.common;

/**
 * Created by andy on 2/15/17.
 */
public class Settings {
    public static final int TCP_CLIENT_PERSISTENT = 0;
    public static final int TCP_CLIENT_SPAWNING = 1;
    public static final int UDP_CLIENT = 2;

    public static final int TCP_SERVER_THREAD_PER_CLIENT = 0;
    public static final int TCP_SERVER_CACHING_THREAD_POOL = 1;
    public static final int TCP_SERVER_FIXED_THREAD_POOL = 2;
    public static final int TCP_SERVER_SERIAL = 3;
    public static final int TCP_SERVER_ASYNC = 4;
    public static final int UDP_SERVER_THREAD_PER_REQUEST = 5;
    public static final int UDP_SERVER_THREAD_POOL = 6;

    public static final int SERVER_PORT = 1235;
}
