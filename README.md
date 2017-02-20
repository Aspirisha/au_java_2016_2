## Server Architecture Benchmarker
Allows to compare different server architectures under different conditions.

##Build
1. ```git clone https://github.com/Aspirisha/au_java_2016_2.git cssort```
2. ```cd cssort```
3. ```git checkout cssort```
4. ```gradle build```

##Run
To run benchmarks, you need to:
* start server (either on the local machine or not)
    - ```cd $CSSORT_DIR/server/build/libs```
    - ```java -jar server-all-1.0-SNAPSHOT.jar -a 0```
* start profiler
    - ```cd $CSSORT_DIR/profiler/build/libs```
    - ```java -jar profiler-all-1.0-SNAPSHOT.jar```

When started, profiler shows GUI which is by no means awfully fu*king ugly. Nevertheless, it allows to tune different parameters (choose server architecture, select varying argument etc), and finally to run the benchmark.

Server listens to hardcoded ports 1235 (clients) and 1236 (profiler).
This can be changed in the *Settings.java* file.

##Other Info
Directory **stats** contains gathered statistics. Yet, benchmarks were held locally (server was running on the same machine as profiler) so precision might be not perfect. 
