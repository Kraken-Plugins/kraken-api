package com.kraken.api.core.script;

import com.google.inject.Inject;
import com.kraken.api.core.KrakenThreads;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.events.GameTick;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A long-running automation loop driven off the client's game ticks.
 *
 * <h2>Lifecycle</h2>
 *
 * <p>A script moves through {@link State}, and every transition is a single atomic step, so two
 * threads racing to start or stop one produce a single winner rather than a half-applied change.</p>
 *
 * <p>Each start creates a {@link Run} owning that execution's worker thread, cancellation token and
 * in-flight loop. A stop finalises the run it was given and nothing else, so restarting a script whose
 * previous loop has not yet returned is safe: the outgoing run releases its own worker while the
 * incoming one keeps hers.</p>
 *
 * <p>{@link #pause()} stops new loops being submitted; a loop already in flight runs to completion,
 * including whatever game actions it has left to perform. Use {@link #stop()} when in-flight work must
 * unwind instead, and {@link #awaitStopped(long)} to wait for that to finish.</p>
 */
@Slf4j
public abstract class Script implements Scriptable {

    /**
     * Where a script is in its lifecycle.
     */
    public enum State {

        /** Not running. No worker, no event bus registration. */
        STOPPED,

        /** {@link #onStart()} is running. Ticks are not yet dispatched to {@link #loop()}. */
        STARTING,

        /** Ticks are dispatched to {@link #loop()}. */
        RUNNING,

        /** Started, but ticks are not dispatched. A loop in flight when the pause landed still finishes. */
        PAUSED,

        /** A stop has been requested and is waiting for the in-flight loop and {@link #onStop()}. */
        STOPPING
    }

    /**
     * One execution of the script: its worker, its cancellation token, the loop currently in flight,
     * and the signals that it has been cancelled and has finished stopping.
     *
     * <p>Everything an execution owns lives here, so a callback left over from a stopped run can only
     * finalise its own state. A restart installs a new run; the old one shuts down the worker it
     * created, never the replacement's.</p>
     */
    private static final class Run {

        private final ExecutorService executor = KrakenThreads.newExecutor("script");
        private final ScriptCancellation cancellation = new ScriptCancellation();
        private final CountDownLatch cancelled = new CountDownLatch(1);
        private final CountDownLatch stopped = new CountDownLatch(1);
        private volatile Future<?> future;

        /**
         * Signals every wait belonging to this run that it should unwind.
         */
        void cancel() {
            cancellation.cancel();
            cancelled.countDown();
        }

        /**
         * Waits out the delay a loop asked for, returning as soon as the run is cancelled instead of
         * holding a stop open for the rest of it.
         *
         * @param delayMs how long the loop asked to sleep, in milliseconds
         * @return true when the wait ended early because the run was cancelled
         * @throws InterruptedException if the worker thread is interrupted while waiting
         */
        boolean awaitCancellation(long delayMs) throws InterruptedException {
            return cancelled.await(delayMs, TimeUnit.MILLISECONDS);
        }
    }

    @Inject
    private EventBus eventBus;

    private final AtomicReference<State> state = new AtomicReference<>(State.STOPPED);
    private final AtomicReference<Run> currentRun = new AtomicReference<>();
    private final String name;

    public Script() {
        this.name = this.getClass().getName();
    }

    /**
     * Where this script currently is in its lifecycle.
     *
     * @return the current state, never null
     */
    public final State getState() {
        return state.get();
    }

    /**
     * Waits for a stop to finish, i.e. for the in-flight loop to return and {@link #onStop()} and any
     * stop callback to have run.
     *
     * @param timeoutMs how long to wait, in milliseconds
     * @return true when the script has finished stopping, false on timeout or if the wait was interrupted
     */
    public final boolean awaitStopped(long timeoutMs) {
        Run run = currentRun.get();
        if (run == null) {
            return state.get() == State.STOPPED;
        }

        try {
            return run.stopped.await(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * Executes a specific loop logic and returns an integer result based on the implementation.
     * <p>
     * This abstract method needs to be implemented by subclasses to define the specific
     * behavior of the loop.
     * </p>
     *
     * @return an integer value representing the amount of time to sleep in milliseconds. Since this is called every game
     *  tick, any value {@literal  <=} 600 will execute on the next game tick.
     *
     * <p><strong>Example Usage:</strong></p>
     * <pre>{
     * public class CustomScript extends Script {
     *     {@literal @Override}
     *     public int loop() {
     *         // Do something to automate game
     *         return 100;
     *     }
     * }
     * }</pre>
     */
    public abstract int loop();

    /**
     * Optional: Called when the script starts.
     */
    public void onStart() {}

    /**
     * Optional: Called when the script stops.
     */
    public void onStop() {}

    /**
     * Starts the script execution, initializing the necessary parts and marking the script as running.
     * <p>
     * This method transitions the script into a "running" state by performing the following steps:
     * </p>
     * <ul>
     *   <li>Claims the {@link State#STARTING} state; if the script is already starting, running or
     *       paused, the method returns immediately.</li>
     *   <li>Creates the run that owns this execution's worker thread and cancellation token.</li>
     *   <li>Registers the script instance to the {@code eventBus} for event handling.</li>
     *   <li>Invokes the {@link #onStart()} method to allow subclasses to define custom startup logic.</li>
     *   <li>Moves to {@link State#RUNNING}, from which game ticks reach {@link #loop()}.</li>
     * </ul>
     *
     * <h3>Thread-Safety</h3>
     * <p>
     * The state claim is atomic, so concurrent callers produce one start rather than two. Starting a
     * script whose previous run is still stopping is allowed and gives the new run its own worker; the
     * outgoing run cannot then interfere with it.
     * </p>
     *
     * <h3>Behavior</h3>
     * <ul>
     *   <li>If the script is already starting, running or paused, no further actions are performed.</li>
     *   <li>Otherwise, the script is initialized, event handling is enabled, and startup logic is executed.</li>
     *   <li>If {@link #onStart()} throws, the script is unregistered and left {@link State#STOPPED}
     *       rather than registered and marked running, and the exception is rethrown.</li>
     * </ul>
     *
     * <h3>Example Usage</h3>
     * <p>Used during the initialization process of a script:</p>
     * <pre>
     * <code>
     * // Called during the plugin's start-up phase to launch the script
     * {@literal @Override}
     * protected void startUp() {
     *     context.register();
     *     context.initializePackets();
     *     exampleScript.start(); // Start the script
     *
     *     overlayManager.add(overlay);
     * }
     * </code>
     * </pre>
     */
    public final void start() {
        State previous;
        do {
            previous = state.get();
            if (previous == State.STARTING || previous == State.RUNNING || previous == State.PAUSED) {
                return;
            }
        } while (!state.compareAndSet(previous, State.STARTING));

        // A restart while the previous run is still unwinding gets its own worker and token, so a
        // cancellation or a stop callback belonging to that run cannot reach into this one.
        Run run = new Run();
        currentRun.set(run);
        eventBus.register(this);
        log.info("[{}] script started", this.name);

        try {
            onStart();
        } catch (RuntimeException | Error e) {
            // A failed startup must not leave a registered script the event bus would keep ticking.
            eventBus.unregister(this);
            run.executor.shutdownNow();
            currentRun.compareAndSet(run, null);
            state.compareAndSet(State.STARTING, State.STOPPED);
            run.stopped.countDown();
            throw e;
        }

        // A stop that landed during onStart() has already claimed STOPPING, so this leaves it alone.
        state.compareAndSet(State.STARTING, State.RUNNING);
    }

    /**
     * Pauses the execution of the script.
     * <p>
     * This method moves a {@link State#RUNNING} script to {@link State#PAUSED}, which stops game ticks
     * reaching {@link #loop()}. A loop already in flight is left alone and runs to completion, including
     * any game actions it has still to perform, so a pause is not a way to make the script stop touching
     * the game immediately — {@link #stop()} is. If the script is not running, this has no effect.
     * </p>
     *
     * <h3>Behavior</h3>
     * <ul>
     *   <li>If the script is {@link State#RUNNING}, it moves to {@link State#PAUSED} and logs the pause.</li>
     *   <li>If the script is in any other state, the method performs no actions.</li>
     * </ul>
     *
     * <h3>Thread-Safety</h3>
     * <p>
     * Ensure thread-safe access to the script's state when invoking this method to prevent race conditions.
     * </p>
     *
     * <h3>Example Usage</h3>
     * <p>Used when a configuration change should trigger the script to pause:</p>
     * <pre>
     * <code>
     * {@literal @Subscribe}
     * private void onConfigChanged(final ConfigChanged event) {
     *     if (event.getGroup().equals("testapi") and event.getKey().equalsIgnoreCase("pauseScript")) {
     *         if (config.pauseScript()) {
     *             exampleScript.pause();
     *         } else {
     *             exampleScript.resume();
     *         }
     *     }
     * }
     * </code>
     * </pre>
     */
    public final void pause() {
        if (state.compareAndSet(State.RUNNING, State.PAUSED)) {
            log.info("[{}] script paused", this.name);
        }
    }

    /**
     * Resumes the execution of the script if it is currently paused.
     * <p>
     * This method moves a {@link State#PAUSED} script back to {@link State#RUNNING}, from which game
     * ticks reach {@link #loop()} again. Only a paused script can resume: a script that was never
     * started, or one that has stopped, is untouched, so a resume can never mark an unregistered script
     * running.
     * </p>
     * <h3>Behavior</h3>
     * <ul>
     * <li>If the script is {@link State#PAUSED}, it moves to {@link State#RUNNING} and logs the resumption.</li>
     * <li>In any other state, including {@link State#STOPPED}, the method performs no actions.</li>
     * </ul>
     * <h3>Thread-Safety</h3>
     * <p>Ensure thread-safe access to the script's state before calling this method.</p>
     *
     * <h3>Example Usage:</h3>
     * <pre>
     * <code>
     * {@literal @Subscribe}
     * private void onConfigChanged(final ConfigChanged event) {
     *     if (event.getGroup().equals("testapi") and event.getKey().equalsIgnoreCase("pauseScript")) {
     *         if (config.pauseScript()) {
     *             exampleScript.pause();
     *         } else {
     *             exampleScript.resume();
     *         }
     *     }
     * }
     * </code>
     * </pre>
     */
    public final void resume() {
        if (state.compareAndSet(State.PAUSED, State.RUNNING)) {
            log.info("[{}] script resumed", this.name);
        }
    }

    /**
     * Handles actions to be executed on each game tick event while the script is running.
     * <p>
     * This method is triggered by the {@code GameTick} event, which occurs at consistent 0.6s intervals in the game.
     * It coordinates the execution of the script's main logic by invoking the {@link #loop()} method on a separate thread.
     * </p>
     *
     * <h3>Key Behavior:</h3>
     * <ul>
     *     <li>Ensures the script is {@link State#RUNNING} before proceeding; in any other state the method returns immediately.</li>
     *     <li>Skips execution if the current run's previous {@code loop()} call is still in progress.</li>
     *     <li>Submits the {@code loop()} logic to the current run's worker for asynchronous execution.</li>
     *     <li>If a delay is set by the {@code loop()} method, the worker waits out that delay, returning early if the run is stopped.</li>
     *     <li>Treats {@link ScriptStoppedException} as normal termination and logs it at debug level rather than as an error.</li>
     *     <li>Logs any other exception thrown during loop execution as an error.</li>
     *     <li>Binds this script's {@link ScriptCancellation} token to the worker thread for the duration of the loop, and releases it afterward.</li>
     * </ul>
     *
     * <h3>Threading Model:</h3>
     * <p>
     * The main game logic defined in {@link #loop()} is executed asynchronously to avoid blocking the main game thread.
     * This separation allows for the use of thread sleeps and other blocking operations within the loop's logic.
     * </p>
     *
     * @param event an instance of {@code GameTick} representing a single tick of the game clock.
     *             It triggers all logic tied to the game's periodic updates.
     *
     * <h3>Example Usage:</h3>
     * <pre>
     * <code>
     * public class ExampleScript extends Script {
     *
     *     {@literal @Override}
     *     public int loop() {
     *         log.info("Executing game logic...");
     *         // Perform actions such as pathfinding or combat
     *         return 1000; // Delay in milliseconds before the next execution
     *     }
     * }
     * </code>
     * </pre>
     */
    @Subscribe
    public final void onGameTick(GameTick event) {
        if (state.get() != State.RUNNING) return;

        final Run run = currentRun.get();
        if (run == null) return;

        // If we are sleeping as part of loop() skip calling loop again this game tick.
        Future<?> inFlight = run.future;
        if (inFlight != null && !inFlight.isDone()) return;

        try {
            run.future = run.executor.submit(() -> runLoop(run));
        } catch (RejectedExecutionException e) {
            log.debug("[{}] Loop not submitted, this run is shutting down", this.name);
        }
    }

    /**
     * Runs one iteration of {@link #loop()} on a run's worker and waits out the delay it asked for.
     *
     * @param run the run this iteration belongs to
     */
    private void runLoop(Run run) {
        run.cancellation.bindToCurrentThread();
        try {
            int delay = loop();
            if (delay > 0 && run.awaitCancellation(delay)) {
                log.debug("[{}] Loop delay cut short by a stop", this.name);
            }
        } catch (ScriptStoppedException e) {
            log.debug("[{}] Script loop cancelled", this.name);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("[{}] Error in script:", this.name, e);
        } finally {
            ScriptCancellation.unbindFromCurrentThread();
        }
    }

    /**
     * Stops the current execution loop and performs the necessary cleanup.
     * This method safely shuts down the process, unregisters the instance
     * from the event bus, and triggers the provided callback after successful termination.
     *
     * <p>Behavior:</p>
     * <ul>
     *     <li>Claims {@link State#STOPPING}; a script already stopped or stopping is left alone.</li>
     *     <li>Unregisters the instance from the event bus.</li>
     *     <li>Cancels the run's token, which unwinds its blocking helpers and cuts short a loop delay.</li>
     *     <li>Waits for the in-flight loop to return before invoking {@link #onStop()} and the {@code callback}.</li>
     *     <li>Shuts down that run's worker only, so a script restarted mid-stop keeps its new one.</li>
     * </ul>
     *
     * <p>Returns as soon as the stop is under way; use {@link #awaitStopped(long)} to wait for it to
     * finish.</p>
     *
     * @param callback A {@code Runnable} that will execute after the stop operation is complete;
     *                 can be {@code null} if no action is required after stopping.
     */
    public void stop(Runnable callback) {
        State previous;
        do {
            previous = state.get();
            if (previous == State.STOPPED || previous == State.STOPPING) {
                return;
            }
        } while (!state.compareAndSet(previous, State.STOPPING));

        eventBus.unregister(this);

        final Run run = currentRun.get();
        if (run == null) {
            state.compareAndSet(State.STOPPING, State.STOPPED);
            if (callback != null) callback.run();
            return;
        }

        // Unblocks this run's blocking helpers and cuts short a loop delay that is already under way.
        run.cancel();

        Future<?> pending = run.future;
        if (pending == null || pending.isDone()) {
            finishStop(run, callback);
            return;
        }

        log.info("[{}] Stopping script...", this.name);

        // Queued behind the in-flight loop on this run's single worker, so it runs once that loop
        // returns. shutdown() lets the queue drain rather than discarding it.
        try {
            run.executor.submit(() -> finishStop(run, callback));
            run.executor.shutdown();
        } catch (RejectedExecutionException e) {
            finishStop(run, callback);
        }
    }

    /**
     * Runs the stop callbacks and releases one run's worker.
     *
     * @param run      The run being finalised. Only its own worker is shut down.
     * @param callback Optional caller-supplied hook to run once the script has stopped.
     */
    private void finishStop(Run run, Runnable callback) {
        log.info("[{}] Script stopped", this.name);
        try {
            onStop();
            if (callback != null) callback.run();
        } catch (Exception e) {
            log.error("[{}] Stop handler failed: ", this.name, e);
        } finally {
            run.executor.shutdown();
            // Only the run that is still the current one may declare the script stopped. A run that a
            // restart replaced finalises itself and leaves the incoming run's state and worker alone.
            if (currentRun.compareAndSet(run, null)) {
                state.compareAndSet(State.STOPPING, State.STOPPED);
            }
            run.stopped.countDown();
        }
    }

    /**
     * Gracefully stops a running asynchronous loop.
     */
    public void stop() {
        stop(null);
    }
}