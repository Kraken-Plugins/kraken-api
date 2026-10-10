package com.kraken.api.service.ui.login;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.kraken.api.Context;
import com.kraken.api.core.hooks.HooksLoader;
import com.kraken.api.service.util.ReflectionService;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Constants;
import net.runelite.api.events.GameTick;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

/**
 * Controls how long the player can stay idle before being logged out, from one client tick up to the six-hour
 * session limit the game server enforces, or never via {@link #disableIdleTimeout()}.
 *
 * <p>RuneLite's {@code Client.setIdleTimeout} clamps the timeout to 5–30 minutes, so this service writes the
 * client's idle timeout field directly through the {@code idleTimeoutField} hook. The client can only enforce
 * part of the accepted range itself, so the service picks one of three ways to honor a timeout:</p>
 * <ul>
 *     <li>5 minutes up to about 47 minutes 43 seconds: the client enforces it through its own idle logout.</li>
 *     <li>Above that: RuneLite rescales the mouse idle counter with {@code 15000 * idleTicks / timeout} in int
 *     arithmetic, which overflows, so the client check is switched off and a watchdog logs out instead.</li>
 *     <li>Below 5 minutes: the client compares keyboard idle time against a fixed 5 minutes whatever the field
 *     holds, so the watchdog enforces the shorter timeout.</li>
 * </ul>
 *
 * <p>While a timeout is set, the service re-applies it every game tick if anything else changes the value, such
 * as RuneLite's Logout Timer plugin, which calls {@code Client.setIdleTimeout} when it starts or its config changes.
 * Call {@link #reset()} to restore the timeout that was in place beforehand.</p>
 */
@Slf4j
@Singleton
public class IdleTimeoutService {

    /** Longest timeout accepted. The game server ends every session after six hours whatever the client does. */
    public static final Duration MAXIMUM_IDLE_TIMEOUT = Duration.ofHours(6);

    /**
     * Idle client ticks after which the vanilla client sends its idle logout. RuneLite rescales the mouse idle
     * counter onto this threshold but compares the keyboard idle counter against it directly.
     */
    static final int CLIENT_IDLE_THRESHOLD_TICKS = 15000;

    /**
     * Longest timeout, in client ticks, the client enforces itself. Past this many idle ticks RuneLite's
     * {@code 15000 * idleTicks / timeout} overflows an int.
     */
    static final int NATIVE_MAXIMUM_TICKS = Integer.MAX_VALUE / CLIENT_IDLE_THRESHOLD_TICKS;

    /**
     * Field value that stops the client from ever sending its idle logout. Any value above
     * {@link #NATIVE_MAXIMUM_TICKS} does that, but only this one keeps the overflowed quotient at zero; others turn
     * it negative after about 48 idle minutes, which makes the client reset its mouse idle counter and breaks the
     * watcher that reads it.
     */
    static final int DISABLED_FIELD_VALUE = Integer.MAX_VALUE;

    /** Game ticks between watchdog logout attempts, so a logout refused during combat is not retried every tick. */
    static final int LOGOUT_RETRY_GAME_TICKS = 10;

    /** Marks that the service is not managing the timeout. The field never holds 0, since the client divides by it. */
    private static final int NOT_MANAGED = 0;

    private static final int NO_WATCHDOG = -1;

    private final Client client;
    private final EventBus eventBus;
    private final ReflectionService reflectionService;
    private final Provider<Context> ctxProvider;

    // Written only on the client thread; volatile so getIdleTimeout() can read them from any thread.
    private volatile int fieldValue = NOT_MANAGED;
    private volatile int watchdogTicks = NO_WATCHDOG;

    // Client thread only.
    private Integer previousFieldValue;
    private int gameTicksUntilLogoutRetry;
    private boolean registered;

    @Inject
    public IdleTimeoutService(Client client, EventBus eventBus, ReflectionService reflectionService, Provider<Context> ctxProvider) {
        this.client = client;
        this.eventBus = eventBus;
        this.reflectionService = reflectionService;
        this.ctxProvider = ctxProvider;
    }

    /**
     * Sets how long the player can stay idle, with neither mouse nor keyboard input, before being logged out.
     *
     * @param timeout The idle timeout, from one client tick (20 ms) up to {@link #MAXIMUM_IDLE_TIMEOUT}.
     * @return {@code true} if the timeout was applied, {@code false} if the hook is unavailable or the write failed.
     * @throws IllegalArgumentException If the timeout is shorter than one client tick or longer than six hours.
     *                                  Use {@link #disableIdleTimeout()} to never log out.
     */
    public boolean setIdleTimeout(Duration timeout) {
        int ticks = toClientTicks(timeout);
        int newFieldValue = ticks <= NATIVE_MAXIMUM_TICKS ? ticks : DISABLED_FIELD_VALUE;
        int newWatchdogTicks = ticks < CLIENT_IDLE_THRESHOLD_TICKS || ticks > NATIVE_MAXIMUM_TICKS ? ticks : NO_WATCHDOG;
        return ctxProvider.get().runOnClientThread(() -> apply(newFieldValue, newWatchdogTicks), Boolean.FALSE);
    }

    /**
     * Stops the client from logging the player out for being idle. The game server still ends the session after
     * six hours.
     *
     * @return {@code true} if the client's idle logout was disabled, {@code false} if the hook is unavailable or the
     *         write failed.
     */
    public boolean disableIdleTimeout() {
        return ctxProvider.get().runOnClientThread(() -> apply(DISABLED_FIELD_VALUE, NO_WATCHDOG), Boolean.FALSE);
    }

    /**
     * Restores the idle timeout that was in place before this service first changed it and stops re-applying the
     * value. The restore goes through RuneLite's own setter, so it lands within RuneLite's 5–30 minute range.
     *
     * @return {@code true} if the previous timeout was restored or nothing had been changed, {@code false} if the
     *         client thread could not run the restore.
     */
    public boolean reset() {
        return ctxProvider.get().runOnClientThread(this::restore, Boolean.FALSE);
    }

    /**
     * Returns the idle timeout currently in effect, whether set through this service or by RuneLite.
     *
     * @return The idle timeout, or empty if the player is never logged out for being idle.
     */
    public Optional<Duration> getIdleTimeout() {
        int watchdog = watchdogTicks;
        if (watchdog != NO_WATCHDOG) {
            return Optional.of(toDuration(watchdog));
        }

        int clientTicks = client.getIdleTimeout();
        return clientTicks > NATIVE_MAXIMUM_TICKS ? Optional.empty() : Optional.of(toDuration(clientTicks));
    }

    /**
     * Re-applies the managed timeout if something else replaced it, and logs out once the player has been idle for
     * a timeout the client cannot enforce itself. Runs on the client thread.
     *
     * @param event The game tick event.
     */
    @Subscribe
    public void onGameTick(GameTick event) {
        int managedValue = fieldValue;
        if (managedValue == NOT_MANAGED) {
            return;
        }

        int currentValue = client.getIdleTimeout();
        if (currentValue != managedValue) {
            log.debug("Idle timeout changed to {} ticks outside IdleTimeoutService, restoring {} ticks", currentValue, managedValue);
            writeField(managedValue);
        }

        int watchdog = watchdogTicks;
        if (watchdog == NO_WATCHDOG) {
            return;
        }

        int idleTicks = Math.min(client.getMouseIdleTicks(), client.getKeyboardIdleTicks());
        if (idleTicks < watchdog) {
            gameTicksUntilLogoutRetry = 0;
            return;
        }

        if (gameTicksUntilLogoutRetry > 0) {
            gameTicksUntilLogoutRetry--;
            return;
        }

        log.info("Idle for {} client ticks, past the {} tick idle timeout; logging out", idleTicks, watchdog);
        gameTicksUntilLogoutRetry = LOGOUT_RETRY_GAME_TICKS;
        ctxProvider.get().players().local().logout();
    }

    /**
     * Converts a timeout into whole client ticks, rejecting values the client cannot hold.
     *
     * @param timeout The requested timeout.
     * @return The timeout in client ticks, at least 1.
     * @throws IllegalArgumentException If the timeout is shorter than one client tick or longer than six hours.
     */
    static int toClientTicks(Duration timeout) {
        Objects.requireNonNull(timeout, "timeout");
        if (timeout.compareTo(MAXIMUM_IDLE_TIMEOUT) > 0) {
            throw new IllegalArgumentException("Idle timeout " + timeout + " exceeds the server's " + MAXIMUM_IDLE_TIMEOUT + " session limit");
        }

        long ticks = timeout.toMillis() / Constants.CLIENT_TICK_LENGTH;
        if (ticks < 1) {
            throw new IllegalArgumentException("Idle timeout " + timeout + " is shorter than one client tick ("
                    + Constants.CLIENT_TICK_LENGTH + " ms); use disableIdleTimeout() to never log out");
        }
        return (int) ticks;
    }

    private static Duration toDuration(int clientTicks) {
        return Duration.ofMillis((long) clientTicks * Constants.CLIENT_TICK_LENGTH);
    }

    /**
     * Writes the field and records it as the managed value, remembering the client's timeout from before the first
     * change so {@link #reset()} can restore it. Registers for game ticks on first use. Runs on the client thread.
     *
     * @param newFieldValue The value to write into the client's idle timeout field.
     * @param newWatchdogTicks The idle ticks after which the watchdog logs out, or {@link #NO_WATCHDOG}.
     * @return {@code true} if the field was written.
     */
    private boolean apply(int newFieldValue, int newWatchdogTicks) {
        int valueBeforeWrite = client.getIdleTimeout();
        if (!writeField(newFieldValue)) {
            return false;
        }

        if (previousFieldValue == null) {
            previousFieldValue = valueBeforeWrite;
        }
        fieldValue = newFieldValue;
        watchdogTicks = newWatchdogTicks;
        gameTicksUntilLogoutRetry = 0;

        if (!registered) {
            eventBus.register(this);
            registered = true;
        }
        return true;
    }

    /**
     * Stops managing the timeout and restores the value from before the first change. Runs on the client thread.
     *
     * @return Always {@code true}.
     */
    private boolean restore() {
        if (fieldValue == NOT_MANAGED) {
            return true;
        }

        if (registered) {
            eventBus.unregister(this);
            registered = false;
        }

        int restoredValue = previousFieldValue;
        fieldValue = NOT_MANAGED;
        watchdogTicks = NO_WATCHDOG;
        previousFieldValue = null;
        client.setIdleTimeout(restoredValue);
        return true;
    }

    private boolean writeField(int value) {
        String fieldName = HooksLoader.getReflectionHooks().getIdleTimeoutField();
        if (fieldName == null) {
            log.warn("hooks.json has no reflectionHooks.idleTimeoutField, so the idle timeout cannot be changed. "
                    + "Regenerate the hooks with kraken-updater.");
            return false;
        }
        return reflectionService.setFieldValue(client.getClass().getName(), fieldName, null, value);
    }
}
