package com.aujava2016;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static java.lang.Thread.sleep;

/**
 * Created by andy on 9/9/16.
 */
public class LazyTest {
    @Test
    public void SingleThreadTest() {
        Lazy<Integer> l1 = LazyFactory.createLazySingleThreaded(new Supplier<Integer>() {
            int evalCounter = 0;

            @Override
            public Integer get() {
                assert(evalCounter == 0);
                return evalCounter++;
            }
        });

        for (int i = 0; i < 10; i++) {
            assert (l1.get() == 0);
        }

        Lazy<Integer> l2 = LazyFactory.createLazySingleThreaded(new Supplier<Integer>() {
            int evalCounter = 0;
            @Override
            public Integer get() {
                assert(evalCounter == 0);
                evalCounter++;
                return null;
            }
        });
    }

    @Test
    public void MultiThreadTest() {
        Lazy<Integer> l1 = LazyFactory.createLazyMultiThreaded(new Supplier<Integer>() {
            int evalCounter = 0;

            @Override
            public Integer get() {
                try {
                    sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                assert(evalCounter == 0);
                return evalCounter++;
            }
        });

        Lazy<Integer> l2 = LazyFactory.createLazyMultiThreaded(new Supplier<Integer>() {
            int evalCounter = 0;
            @Override
            public Integer get() {
                assert(evalCounter == 0);
                evalCounter++;
                return null;
            }
        });

        final int THREADS_NUM = 10;

        List<Thread> threads = new ArrayList<>(THREADS_NUM);
        for (int i = 0; i < THREADS_NUM; i++) {
            threads.add(i, new Thread(() -> {
                assert(0 == l1.get());
                assert(null == l2.get());
            }));
        }

        threads.forEach(Thread::start);
    }

    @Test
    public void MultiThreadTestLockFree() {
        Lazy<Integer> l1 = LazyFactory.createLazyMultiThreadedLockFree(new Supplier<Integer>() {
            int evalCounter = 0;
            double sink = 0;

            private void longComputation() {
                for (int i = 0; i < 10000; i++) {
                    sink += Math.sqrt(i);
                }
            }

            @Override
            public Integer get() {
                longComputation();
                assert(evalCounter == 0);
                return evalCounter++;
            }
        });

        Lazy<Integer> l2 = LazyFactory.createLazyMultiThreadedLockFree(new Supplier<Integer>() {
            int evalCounter = 0;
            @Override
            public Integer get() {
                assert(evalCounter == 0);
                evalCounter++;
                return null;
            }
        });

        final int THREADS_NUM = 10;

        List<Thread> threads = new ArrayList<>(THREADS_NUM);
        for (int i = 0; i < THREADS_NUM; i++) {
            threads.add(i, new Thread(() -> {
                assert(0 == l1.get());
                assert(null == l2.get());
            }));
        }

        threads.forEach(Thread::start);

        boolean hasAliveThread = true;
        while (hasAliveThread) {
            hasAliveThread = false;

            for (Thread t : threads) {
                switch (t.getState()) {
                    case TERMINATED:
                    case RUNNABLE:
                    case NEW:
                        break;
                    default:
                        assert(false);
                        return;
                }
                hasAliveThread |= t.isAlive();
            }

            Thread.yield();
        }
    }
}
