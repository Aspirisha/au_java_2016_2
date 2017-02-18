package cssort.common;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Created by andy on 2/17/17.
 */
public class Statistics {

    @Data
    @AllArgsConstructor
    public static class ServerRunResult {
        /*public ServerRunResult(long requestTime, long processTime) {
            this.requestTime = requestTime;
            this.processTime = processTime;
        }*/

        public long requestTime;
        public long processTime;
    }

    @Data
    @AllArgsConstructor
    public static class RunResult {
        ServerRunResult serverResult;
        long clientRuntime;
    }
}
