package com.aujava2016;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class LazyFactory {
    public static <T> Lazy<T> createLazySingleThreaded(Supplier<T> s) {
        return new Lazy<T>() {
            T obj = null;
            boolean initialized = false;

            @Override
            public T get() {
                if (!initialized) {
                    obj = s.get();
                    initialized = true;
                }

                return obj;
            }
        };
    }

    public static <T> Lazy<T> createLazyMultiThreaded(Supplier<T> s) {
        return new Lazy<T>() {
            T obj = null;
            boolean initialized = false;

            @Override
            public synchronized T get() {
                if (!initialized) {
                    initialized = true;
                    obj = s.get();
                }
                return obj;
            }
        };
    }

    public static <T> Lazy<T> createLazyMultiThreadedLockFree(Supplier<T> s) {
        class Proxy {
            Proxy(T value) {
                this.value = value;
            }

            T value = null;
        }

        return new Lazy<T>() {
            AtomicBoolean initialized = new AtomicBoolean();
            volatile Proxy obj = null;

            @Override
            public T get() {
                if (initialized.compareAndSet(false, true)) {
                    obj = new Proxy(s.get());
                } else {
                    while (null == obj) {
                        Thread.yield();
                    }
                }

                return obj.value;
            }
        };
    }

}