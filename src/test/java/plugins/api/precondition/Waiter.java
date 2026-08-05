package plugins.api.precondition;

import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import plugins.api.suite.CancellationToken;

import java.util.function.BooleanSupplier;

/**
 * The single blocking wait primitive used by the precondition engine.
 *
 * <p>Engine code must never call {@code SleepService.sleepUntil} directly. That method is overloaded
 * as both {@code (Supplier<Boolean>, int)} meaning <em>ticks</em> and {@code (BooleanSupplier, int)}
 * meaning <em>milliseconds</em>, and which one a call binds to depends on the inferred type of the
 * lambda. Assigning a condition to a {@code Supplier<Boolean>} variable silently converts a five
 * second timeout into a five thousand tick one — fifty minutes. One wait primitive with unambiguous
 * millisecond semantics removes that whole class of bug, and carries the cancellation poll for free.
 *
 * <p>Everything here blocks and must be called off the client thread.</p>
 */
@Slf4j
@Singleton
public class Waiter {

    /** How often conditions are re-evaluated. Comfortably below one game tick. */
    private static final long POLL_INTERVAL_MS = 100;

    /**
     * Waits for a condition to become true.
     *
     * @param condition evaluated on the calling thread; must be cheap and side effect free
     * @param timeoutMs how long to wait, in milliseconds
     * @param token polled every iteration so a cancelled run stops promptly
     * @param what description of what is being waited for, used in the timeout log line
     * @return true if the condition became true within the timeout, false if it timed out
     * @throws plugins.api.suite.TestCancelledException if the run is cancelled while waiting
     */
    public boolean until(BooleanSupplier condition, long timeoutMs, CancellationToken token, String what) {
        long deadline = System.currentTimeMillis() + timeoutMs;

        while (true) {
            token.throwIfCancelled("waiting for " + what);

            if (condition.getAsBoolean()) {
                return true;
            }

            if (System.currentTimeMillis() >= deadline) {
                log.warn("Timed out after {}ms waiting for {}", timeoutMs, what);
                return false;
            }

            sleep(POLL_INTERVAL_MS, token);
        }
    }

    /**
     * Waits for a condition to become false.
     *
     * @param condition evaluated on the calling thread; must be cheap and side effect free
     * @param timeoutMs how long to wait, in milliseconds
     * @param token polled every iteration so a cancelled run stops promptly
     * @param what description of what is being waited for, used in the timeout log line
     * @return true if the condition became false within the timeout, false if it timed out
     * @throws plugins.api.suite.TestCancelledException if the run is cancelled while waiting
     */
    public boolean whileTrue(BooleanSupplier condition, long timeoutMs, CancellationToken token, String what) {
        return until(() -> !condition.getAsBoolean(), timeoutMs, token, what);
    }

    /**
     * Sleeps for a fixed duration, remaining responsive to cancellation.
     *
     * @param millis how long to sleep
     * @param token polled so a cancelled run stops promptly
     * @throws plugins.api.suite.TestCancelledException if the run is cancelled while sleeping
     */
    public void sleep(long millis, CancellationToken token) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            // Restore the flag so anything further up the stack still sees the interrupt, then convert
            // it into the control signal the runner understands.
            Thread.currentThread().interrupt();
            token.throwIfCancelled("sleeping");
        }
    }
}
