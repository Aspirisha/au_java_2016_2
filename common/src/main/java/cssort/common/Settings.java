package cssort.common;

/**
 * Created by andy on 2/15/17.
 */
public class Settings {
    public enum Architecture {
        TCP_CLIENT_PERSISTENT_SERVER_THREAD_PER_CLIENT("Tcp persistent thread/client"),
        TCP_CLIENT_PERSISTENT_SERVER_CACHING_THREAD_POOL("Tcp persistent caching thread pool"),
        TCP_CLIENT_PERSISTENT_SERVER_NON_BLOCKING("Tcp persistent non blocking"),
        TCP_CLIENT_SPAWNING_SERVER_SINGLE_THREADED_SERIAL("Tcp spawning serial"),
        TCP_CLIENT_SPAWNING_SERVER_ASYNCHRONOUS("Tcp spawning asynchronous"),
        UDP_CLIENT_THREAD_PER_REQUEST("Udp thread/request"),
        UDP_CLIENT_FIXED_THREAD_POOL("Udp thread pool"),
        ;
        private final String name;

        Architecture(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }

        public static Architecture fromString(String text) {
            for (Architecture b : Architecture.values()) {
                if (b.name.equalsIgnoreCase(text)) {
                    return b;
                }
            }
            return null;
        }
    }

    public static final int SERVER_PORT = 1235;
}
