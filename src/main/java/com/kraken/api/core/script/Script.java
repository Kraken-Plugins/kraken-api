package com.kraken.api.core.script;

import com.google.inject.Inject;
import com.kraken.api.core.KrakenThreads;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.events.GameTick;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * A game-tick driven worker with serialized lifecycle transitions. Each run owns its worker and
 * cancellation signal. A start requested during stopping is deferred until the old loop and cleanup
 * have returned, so subclass hooks cannot clean up a replacement run's resources.
 *
 * <p>Pause prevents new iterations; an iteration already submitted may finish its actions. Stop is
 * cooperative: blocking helpers observe cancellation, but arbitrary user code must return on its own.
 * Never wait for stop completion on the client thread or inside this script's hooks or loop.</p>
 */
@Slf4j
public abstract class Script implements Scriptable {

    /** Lifecycle states; STARTING and STOPPING do not accept ticks. */
    public enum State { STOPPED, STARTING, RUNNING, PAUSED, STOPPING }

    private static final class Run {
        private final ExecutorService executor = KrakenThreads.newExecutor("script");
        private final ScriptCancellation cancellation = new ScriptCancellation();
        private final CountDownLatch cancelled = new CountDownLatch(1);
        private final CompletableFuture<Void> stopped = new CompletableFuture<>();
        // Guarded by lifecycleLock, as are currentRun, state and restartRequested.
        private Future<?> future;
        private boolean starting = true;
        private Runnable callback;

        private void cancel() {
            cancellation.cancel();
            cancelled.countDown();
        }
    }

    @Inject
    private EventBus eventBus;

    private final Object lifecycleLock = new Object();
    private State state = State.STOPPED;
    private Run currentRun;
    private boolean restartRequested;

    /** @return the current lifecycle state */
    public final State getState() {
        synchronized (lifecycleLock) {
            return state;
        }
    }

    /**
     * Waits for the run observed at entry to stop, including its cleanup callback. A later restart
     * does not change the run being awaited.
     * @param timeoutMs maximum wait in milliseconds
     * @return true on completion, false on timeout or interruption
     */
    public final boolean awaitStopped(long timeoutMs) {
        CompletableFuture<Void> completion;
        synchronized (lifecycleLock) {
            completion = currentRun == null ? CompletableFuture.completedFuture(null) : currentRun.stopped;
        }
        try {
            completion.get(timeoutMs, TimeUnit.MILLISECONDS);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (java.util.concurrent.ExecutionException | java.util.concurrent.TimeoutException e) {
            return false;
        }
    }

    /** @return milliseconds to wait after this iteration; non-positive values wait for the next tick */
    public abstract int loop();

    /** Initializes a run before it accepts ticks. A deferred restart invokes this on the old worker. */
    public void onStart() {}

    /** Cleans up a successfully initialized run after its last iteration returns. */
    public void onStop() {}

    /**
     * Starts once, or requests one restart after an outstanding stop finishes. Startup runs on the
     * caller unless deferred. A startup exception rolls back registration and is rethrown.
     */
    public final void start() {
        Run run;
        synchronized (lifecycleLock) {
            if (state == State.STOPPING) {
                restartRequested = true;
                return;
            }
            if (state != State.STOPPED) return;
            run = beginRun();
        }
        startRun(run);
    }

    /** Publishes state and event registration together while holding lifecycleLock. */
    private Run beginRun() {
        Run run = new Run();
        currentRun = run;
        state = State.STARTING;
        try {
            eventBus.register(this);
        } catch (RuntimeException | Error e) {
            try {
                eventBus.unregister(this);
            } finally {
                currentRun = null;
                state = State.STOPPED;
                run.executor.shutdown();
                run.stopped.complete(null);
            }
            throw e;
        }
        return run;
    }

    private void startRun(Run run) {
        try {
            onStart();
        } catch (RuntimeException | Error e) {
            synchronized (lifecycleLock) {
                eventBus.unregister(this);
                run.cancel();
                state = State.STOPPING;
                run.starting = false;
            }
            finishStop(run, false);
            throw e;
        }
        synchronized (lifecycleLock) {
            run.starting = false;
            if (state == State.STARTING) {
                state = State.RUNNING;
                log.info("[{}] Script started", getClass().getName());
                return;
            }
        }
        // stop() during startup must wait for onStart() to return before cleanup.
        finishStop(run, true);
    }

    /** Pauses future iterations while allowing an already submitted iteration to finish. */
    public final void pause() {
        synchronized (lifecycleLock) {
            if (state == State.RUNNING) {
                state = State.PAUSED;
                log.info("[{}] Script paused", getClass().getName());
            }
        }
    }

    /** Resumes a paused run; never starts an unregistered or stopped script. */
    public final void resume() {
        synchronized (lifecycleLock) {
            if (state == State.PAUSED) {
                state = State.RUNNING;
                log.info("[{}] Script resumed", getClass().getName());
            }
        }
    }

    /**
     * Submits at most one iteration, including its delay, for the current run.
     * @param event the game tick event
     */
    @Subscribe
    public final void onGameTick(GameTick event) {
        synchronized (lifecycleLock) {
            if (state != State.RUNNING) return;
            Run run = currentRun;
            if (run.future != null && !run.future.isDone()) return;
            run.future = run.executor.submit(() -> runLoop(run));
        }
    }

    private void runLoop(Run run) {
        run.cancellation.bindToCurrentThread();
        try {
            if (run.cancellation.isCancelled()) return;
            int delay = loop();
            if (delay > 0) run.cancelled.await(delay, TimeUnit.MILLISECONDS);
        } catch (ScriptStoppedException e) {
            log.debug("Script loop cancelled");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("Error in script", e);
        } finally {
            ScriptCancellation.unbindFromCurrentThread();
        }
    }

    /**
     * Requests cooperative stop and invokes the callback after cleanup. Repeated stop calls do not
     * install additional callbacks; a stop while stopping cancels any deferred restart.
     * @param callback optional callback run after onStop
     */
    public void stop(Runnable callback) {
        requestStop(callback);
    }

    /** Requests cooperative stop without waiting for an in-flight iteration. */
    public void stop() {
        stop(null);
    }

    /**
     * Requests stop and captures completion for that specific run, even if it is later restarted.
     * @return a completion stage signalled after the loop, cleanup and stop callback return
     */
    public final CompletionStage<Void> stopAsync() {
        return requestStop(null).minimalCompletionStage();
    }

    private CompletableFuture<Void> requestStop(Runnable callback) {
        Run run;
        synchronized (lifecycleLock) {
            restartRequested = false;
            if (currentRun == null) return CompletableFuture.completedFuture(null);
            run = currentRun;
            if (state == State.STOPPING) return run.stopped;
            state = State.STOPPING;
            eventBus.unregister(this);
            run.callback = callback;
            run.cancel();
            if (run.starting) return run.stopped;
            if (run.future != null && !run.future.isDone()) {
                run.executor.submit(() -> finishStop(run, true));
                run.executor.shutdown();
                return run.stopped;
            }
        }
        finishStop(run, true);
        return run.stopped;
    }

    private void finishStop(Run run, boolean initialized) {
        try {
            if (initialized) onStop();
        } catch (Throwable e) {
            log.error("Stop handler failed", e);
        } finally {
            try {
                if (run.callback != null) run.callback.run();
            } catch (Throwable e) {
                log.error("Stop callback failed", e);
            } finally {
                Run replacement = null;
                synchronized (lifecycleLock) {
                    run.executor.shutdown();
                    if (currentRun == run) {
                        currentRun = null;
                        state = State.STOPPED;
                        if (restartRequested) {
                            restartRequested = false;
                            try {
                                replacement = beginRun();
                            } catch (RuntimeException | Error e) {
                                log.error("[{}] Restart registration failed", getClass().getName(), e);
                            }
                        }
                    }
                }
                log.info("[{}] Script stopped", getClass().getName());
                run.stopped.complete(null);
                if (replacement != null) startRun(replacement);
            }
        }
    }
}
