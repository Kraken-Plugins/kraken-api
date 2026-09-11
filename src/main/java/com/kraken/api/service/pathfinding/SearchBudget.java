package com.kraken.api.service.pathfinding;

import com.kraken.api.core.script.ScriptCancellation;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

/** Request-local monotonic deadline and graph allocation ceiling. */
final class SearchBudget {
    private final long started;
    private final long duration;
    private final int maxNodes;
    private final LongSupplier clock;

    SearchBudget(long maxMillis, int maxNodes, LongSupplier clock) {
        this.clock = clock;
        this.started = clock.getAsLong();
        this.duration = TimeUnit.MILLISECONDS.toNanos(maxMillis);
        this.maxNodes = maxNodes;
    }

    boolean exhausted() {
        return Thread.currentThread().isInterrupted() || ScriptCancellation.currentThreadCancelled()
                || clock.getAsLong() - started >= duration;
    }

    void beforeAllocation(int nodes) {
        if (nodes >= maxNodes || exhausted()) throw new Exhausted();
    }

    static final class Exhausted extends RuntimeException {
        private Exhausted() {
            super(null, null, false, false);
        }
    }
}
