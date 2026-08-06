package com.kraken.api.service.util;

import com.kraken.api.Context;
import com.kraken.api.core.Services;
import com.kraken.api.core.script.ScriptCancellation;
import com.kraken.api.core.script.ScriptStoppedException;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Player;
import net.runelite.client.RuneLite;

import javax.inject.Singleton;
import java.util.concurrent.Callable;
import java.util.function.BooleanSupplier;

/**
 * Blocking wait helpers for script threads.
 *
 * <p><strong>None of these methods may be called on the client thread.</strong> Every entry point
 * asserts this and throws {@link IllegalStateException} if the rule is broken. The client thread is
 * the only thread that advances the game tick counter and repaints the scene, so a wait issued from
 * it can never observe the state change it is waiting for — the wait would spin forever and take the
 * game client down with it. Failing loudly at the call site is the only safe behaviour.</p>
 *
 * <p>Timeouts are always milliseconds. The one exception is {@link #sleepUntilTicks}, which is named
 * differently precisely so that a tick budget can never be mistaken for a millisecond budget.</p>
 */
@Slf4j
@Singleton
public class SleepService {

    private static Context ctx() {
        return Services.context();
    }

    /**
     * Default budget for the wait helpers that do not take an explicit timeout.
     */
    private static final long DEFAULT_TIMEOUT_MS = 15_000L;

    /**
     * Interval between condition re-checks for the helpers that do not take an explicit one.
     */
    private static final int DEFAULT_POLL_INTERVAL_MS = 100;

    /**
     * Rejects any attempt to block the client thread.
     *
     * <p>Called at the top of every public entry point rather than only at the innermost sleep so that
     * the resulting stack trace names the helper the caller actually invoked.</p>
     *
     * @throws IllegalStateException when invoked on the client thread.
     */
    private static void assertOffClientThread() {
        if (ctx().getClient().isClientThread()) {
            throw new IllegalStateException("SleepService may not be called on the client thread — "
                    + "the client thread cannot advance the game state it would be waiting for. "
                    + "Move this call onto a script thread, or schedule the follow-up work with "
                    + "Context.runOnClientThread(...) instead of waiting for it.");
        }
    }

    /**
     * Throws if the script that owns this thread has been cancelled or the thread was interrupted.
     *
     * <p>Cancellation is read from the token bound to the calling thread, so a stopping script only
     * interrupts its own waits.</p>
     */
    private static void assertNotCancelled() {
        if (Thread.currentThread().isInterrupted() || ScriptCancellation.currentThreadCancelled()) {
            throw new ScriptStoppedException("Script stopped during sleep");
        }
    }

    /**
     * Waits until the specified condition is true, up to {@value #DEFAULT_TIMEOUT_MS} milliseconds.
     * @param condition the condition to be met
     * @return true if the condition was met, false if the timeout was reached
     */
    public static boolean sleepUntil(BooleanSupplier condition) {
        return sleepUntil(condition, DEFAULT_TIMEOUT_MS);
    }

    /**
     * Sleeps until the specified condition is true or the timeout is reached.
     * @param condition the condition to be met
     * @param timeoutMs the maximum time to wait, in milliseconds
     * @return true if the condition was met, false if the timeout was reached
     */
    public static boolean sleepUntil(BooleanSupplier condition, long timeoutMs) {
        assertOffClientThread();

        long start = System.currentTimeMillis();
        while (!condition.getAsBoolean()) {
            if (System.currentTimeMillis() - start > timeoutMs) {
                return false;
            }
            assertNotCancelled();
            sleep(DEFAULT_POLL_INTERVAL_MS);
        }
        return true;
    }

    /**
     * Sleeps until the specified condition is true or the given number of game ticks have elapsed.
     *
     * <p>Named separately from {@link #sleepUntil} so a tick budget can never be passed where a
     * millisecond budget was intended.</p>
     *
     * @param condition the condition to be met
     * @param ticks the maximum time to wait, in game ticks
     * @return true if the condition was met, false if the tick budget was exhausted
     */
    public static boolean sleepUntilTicks(BooleanSupplier condition, int ticks) {
        assertOffClientThread();

        int end = ctx().getClient().getTickCount() + ticks;
        while (!condition.getAsBoolean()) {
            if (ctx().getClient().getTickCount() >= end) {
                return false;
            }
            assertNotCancelled();
            sleep(DEFAULT_POLL_INTERVAL_MS);
        }
        return true;
    }

    /**
     * Sleeps until the local player's animation is idle, up to {@value #DEFAULT_TIMEOUT_MS} milliseconds.
     * @return true if the player went idle, false if the timeout was reached
     */
    public static boolean sleepUntilIdle() {
        assertOffClientThread();
        return sleepUntil(() -> ctx().players().local().isIdle());
    }

    /**
     * Sleeps until the local player reaches the specified world tile, up to
     * {@value #DEFAULT_TIMEOUT_MS} milliseconds.
     * @param worldX the x-coordinate of the target tile
     * @param worldY the y-coordinate of the target tile
     * @return true if the tile was reached, false if the timeout was reached
     */
    public static boolean sleepUntilTile(int worldX, int worldY) {
        assertOffClientThread();
        return sleepUntil(() -> {
            Player player = ctx().players().local().raw();
            if (player == null) {
                return false;
            }
            return player.getWorldLocation().getX() == worldX
                    && player.getWorldLocation().getY() == worldY;
        });
    }

    /**
     * Sleeps for a specified amount of time in milliseconds. This sleep is interruptible by script cancellation.
     * @param duration the duration to sleep in milliseconds
     */
    public static void sleep(int duration) {
        sleep((long) duration);
    }

    /**
     * Sleeps for a specified amount of time in milliseconds. This sleep is interruptible by script cancellation.
     * @param time the duration to sleep in milliseconds
     */
    public static void sleep(long time) {
        assertOffClientThread();

        if (time <= 0) {
            return;
        }
        long end = System.currentTimeMillis() + time;
        while (System.currentTimeMillis() < end) {
            assertNotCancelled();
            try {
                long remaining = end - System.currentTimeMillis();
                if (remaining > 0) {
                    Thread.sleep(Math.min(remaining, 50)); // Check for cancellation every 50ms
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Sleep interrupted", e);
            }
        }
    }

    /**
     * Sleeps for a random duration between start and end milliseconds.
     * @param start the minimum sleep time
     * @param end the maximum sleep time
     */
    public static void sleep(long start, long end) {
        assertOffClientThread();

        if (start < 0 || end < 0 || start > end) {
            throw new IllegalArgumentException("Invalid sleep range: " + start + " to " + end);
        }
        sleep(RandomService.between((int) start, (int) end));
    }

    /**
     * Sleeps for a random duration between start and end milliseconds.
     * @param start the minimum sleep time
     * @param end the maximum sleep time
     */
    public static void sleep(int start, int end) {
        assertOffClientThread();
        sleep(RandomService.between(start, end));
    }

    /**
     * Sleeps for a duration based on a Gaussian distribution.
     * @param mean the mean sleep time
     * @param stddev the standard deviation of the sleep time
     */
    public static void sleepGaussian(int mean, int stddev) {
        assertOffClientThread();
        sleep(RandomService.randomGaussian(mean, stddev));
    }

    /**
     * Repeatedly calls a method until it returns a non-null value, or a timeout is reached.
     * @param method the method to call
     * @param timeoutMillis the maximum time to wait in milliseconds
     * @param sleepMillis the time to sleep between calls
     * @param <T> the return type of the method
     * @return the non-null value, or null if the timeout was reached
     */
    @SneakyThrows
    public static <T> T sleepUntilNotNull(Callable<T> method, int timeoutMillis, int sleepMillis) {
        assertOffClientThread();

        boolean done;
        T methodResponse;
        final long endTime = System.currentTimeMillis() + timeoutMillis;
        do {
            assertNotCancelled();
            methodResponse = method.call();
            done = methodResponse != null;
            if (!done) {
                sleep(sleepMillis);
            }
        } while (!done && System.currentTimeMillis() < endTime);
        return methodResponse;
    }

    /**
     * Repeatedly calls a method until it returns a non-null value, or a timeout is reached.
     * Sleeps 100ms between calls.
     * @param method the method to call
     * @param timeoutMillis the maximum time to wait in milliseconds
     * @param <T> the return type of the method
     * @return the non-null value, or null if the timeout was reached
     */
    public static <T> T sleepUntilNotNull(Callable<T> method, int timeoutMillis) {
        return sleepUntilNotNull(method, timeoutMillis, DEFAULT_POLL_INTERVAL_MS);
    }

    /**
     * Sleeps the current thread for one game tick.
     */
    public static void tick() {
        assertOffClientThread();
        sleepFor(1);
    }

    /**
     * Sleeps the current thread by the specified number of game ticks.
     * @param ticks ticks
     */
    public static void sleepFor(int ticks) {
        assertOffClientThread();

        int tick = ctx().getClient().getTickCount() + ticks;
        int start = ctx().getClient().getTickCount();
        while (ctx().getClient().getTickCount() < tick && ctx().getClient().getTickCount() >= start) {
            assertNotCancelled();
            sleep(20);
        }
    }

    /**
     * Sleeps for a random number of ticks between the provided values.
     * @param minTicks minimum ticks to sleep
     * @param maxTicks maximum ticks to sleep
     */
    public static void sleepFor(int minTicks, int maxTicks) {
        assertOffClientThread();
        sleepFor(RandomService.between(minTicks, maxTicks));
    }

    /**
     * Waits until the specified condition is true, re-checking at a caller-supplied interval, until a
     * timeout is reached.
     *
     * <p>Equivalent to {@link #sleepUntil(BooleanSupplier, long)} apart from the configurable poll
     * interval; prefer {@code sleepUntil} unless you specifically need to poll faster or slower than
     * the {@value #DEFAULT_POLL_INTERVAL_MS}ms default.</p>
     *
     * @param awaitedCondition the condition to be met
     * @param pollIntervalMs the time to sleep between checks in milliseconds
     * @param timeoutMs the maximum time to wait in milliseconds
     * @return true if the condition was met, false if the timeout was reached
     */
    public static boolean sleepUntilTrue(BooleanSupplier awaitedCondition, int pollIntervalMs, long timeoutMs) {
        assertOffClientThread();

        long startTime = System.currentTimeMillis();
        do {
            assertNotCancelled();
            if (awaitedCondition.getAsBoolean()) {
                return true;
            }
            sleep(pollIntervalMs);
        } while (System.currentTimeMillis() - startTime < timeoutMs);
        return false;
    }

    /**
     * Waits until the specified condition is true, or a timeout is reached.
     *
     * <p>An alias for {@link #sleepUntil(BooleanSupplier, long)} kept for existing callers. Both take
     * a millisecond budget and behave identically; prefer {@code sleepUntil} in new code.</p>
     *
     * @param awaitedCondition the condition to be met
     * @param timeoutMs the maximum time to wait in milliseconds
     * @return true if the condition was met, false if the timeout was reached
     */
    public static boolean sleepUntilTrue(BooleanSupplier awaitedCondition, long timeoutMs) {
        return sleepUntil(awaitedCondition, timeoutMs);
    }

    /**
     * Sleeps while the specified condition is true or until the timeout is reached.
     * @param condition the condition to be met
     * @param timeoutMs the maximum time to sleep in milliseconds
     * @return true if the condition became false, false if the timeout was reached
     */
    public static boolean sleepWhile(BooleanSupplier condition, long timeoutMs) {
        assertOffClientThread();

        long start = System.currentTimeMillis();
        while (condition.getAsBoolean()) {
            if (System.currentTimeMillis() - start > timeoutMs) {
                return false;
            }
            assertNotCancelled();
            sleep(DEFAULT_POLL_INTERVAL_MS);
        }
        return true;
    }
}
