package com.kraken.api.core;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Factory for the background threads this library owns.
 *
 * <p>Every thread produced here is a daemon thread with a descriptive name. Daemon status matters:
 * the JVM will not wait on these threads at shutdown, so a client exit is never held open by API work
 * still in flight.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class KrakenThreads {

    private static final AtomicInteger POOL_COUNT = new AtomicInteger();

    /**
     * Creates a thread factory producing named daemon threads.
     *
     * @param name Short subsystem name used as the thread name prefix (e.g. "camera").
     * @return a {@link ThreadFactory} whose threads are daemons named {@code kraken-<name>-<n>}.
     */
    public static ThreadFactory daemonFactory(String name) {
        final String prefix = "kraken-" + name + "-";
        final AtomicInteger threadCount = new AtomicInteger();
        return runnable -> {
            Thread thread = new Thread(runnable, prefix + threadCount.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
    }

    /**
     * Creates a single-threaded scheduler backed by a named daemon thread.
     *
     * @param name Short subsystem name used as the thread name prefix.
     * @return a new single-threaded {@link ScheduledExecutorService}. The caller owns it and is
     *         responsible for shutting it down.
     */
    public static ScheduledExecutorService newScheduler(String name) {
        return Executors.newSingleThreadScheduledExecutor(daemonFactory(name));
    }

    /**
     * Creates a single-threaded executor backed by a named daemon thread.
     *
     * @param name Short subsystem name used as the thread name prefix.
     * @return a new single-threaded executor. The caller owns it and is responsible for shutting it down.
     */
    public static java.util.concurrent.ExecutorService newExecutor(String name) {
        return Executors.newSingleThreadExecutor(daemonFactory(name + "-" + POOL_COUNT.incrementAndGet()));
    }
}
