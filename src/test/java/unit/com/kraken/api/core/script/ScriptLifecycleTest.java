package unit.com.kraken.api.core.script;

import com.kraken.api.core.script.Script;
import net.runelite.api.events.GameTick;
import net.runelite.client.eventbus.EventBus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;

import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Covers the transitions a {@link Script} makes between stopped, running, paused and stopping.
 *
 * <p>These are the interleavings a plugin produces by toggling a config while a loop is in flight, and
 * they are where a script can quietly stop working: a stop belonging to a previous run reaching into
 * its replacement leaves the script registered and ticking with a dead worker, which looks like a
 * script that simply does nothing.</p>
 */
class ScriptLifecycleTest {

    private final java.util.List<TestScript> scripts = new java.util.ArrayList<>();

    @AfterEach
    void cleanup() {
        for (TestScript script : scripts) {
            if (script.hold != null) script.hold.countDown();
            if (script.holdStart != null) script.holdStart.countDown();
            script.stop();
            assertTrue(script.awaitStopped(TIMEOUT_MS));
        }
    }

    private TestScript newScript() {
        TestScript script = new TestScript();
        scripts.add(script);
        return script;
    }

    private static final GameTick TICK = new GameTick();

    /** How long a test waits for a worker thread to reach a point it should reach immediately. */
    private static final long TIMEOUT_MS = 5_000;

    @Test
    void restartingWhileTheOldLoopIsStillRunningLeavesTheNewRunWorking() throws Exception {
        TestScript script = newScript();
        CountDownLatch release = new CountDownLatch(1);
        script.holdLoopOn(release);

        script.start();
        script.onGameTick(TICK);
        assertTrue(script.loopEntered.await(TIMEOUT_MS, TimeUnit.MILLISECONDS), "the first loop should have started");

        // Stop and restart before the held loop returns. The stop's queued cleanup belongs to the old
        // run, so it must not shut down the worker the restart just installed.
        script.stop();
        script.start();
        release.countDown();

        script.holdLoopOn(null);
        assertTrue(script.awaitLoops(2, TIMEOUT_MS, () -> script.onGameTick(TICK)),
                "the restarted script should still run loops after the old run finished stopping");
        assertEquals(Script.State.RUNNING, script.getState());
    }

    @Test
    void aFailedStartupLeavesTheScriptStopped() {
        TestScript script = newScript();
        script.failOnStart = true;

        assertThrows(IllegalStateException.class, script::start);

        assertEquals(Script.State.STOPPED, script.getState());

        // A script left registered here would keep receiving ticks it can never serve.
        script.onGameTick(TICK);
        assertEquals(0, script.loops.get(), "a script that failed to start must not run loops");
    }

    @Test
    void resumeDoesNotStartAScriptThatWasNeverStarted() {
        TestScript script = newScript();

        script.resume();

        assertEquals(Script.State.STOPPED, script.getState());
        script.onGameTick(TICK);
        assertEquals(0, script.loops.get(), "resume is not a start");
    }

    @Test
    void resumeDoesNotReviveAStoppedScript() {
        TestScript script = newScript();
        script.start();
        script.stop();
        assertTrue(script.awaitStopped(TIMEOUT_MS));

        script.resume();

        assertEquals(Script.State.STOPPED, script.getState());
        script.onGameTick(TICK);
        assertEquals(0, script.loops.get(), "a stopped script must stay stopped");
    }

    @Test
    void pauseStopsFurtherLoopsButLeavesTheScriptStartable() {
        TestScript script = newScript();
        script.start();

        script.pause();
        assertEquals(Script.State.PAUSED, script.getState());

        script.onGameTick(TICK);
        assertEquals(0, script.loops.get(), "a paused script is not given new loops");

        script.resume();
        assertEquals(Script.State.RUNNING, script.getState());
    }

    @Test
    void stoppingDoesNotWaitOutTheLoopDelay() throws Exception {
        TestScript script = newScript();
        script.delayMs = 60_000;

        script.start();
        script.onGameTick(TICK);
        assertTrue(script.loopEntered.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));

        long start = System.currentTimeMillis();
        script.stop();

        assertTrue(script.awaitStopped(TIMEOUT_MS), "the stop should not have waited out the delay");
        assertTrue(System.currentTimeMillis() - start < script.delayMs,
                "cancellation should have cut the delay short");
        assertEquals(Script.State.STOPPED, script.getState());
    }

    @Test
    void stoppingATwiceStartedScriptRunsTheStopHandlerOnce() {
        TestScript script = newScript();
        script.start();
        script.start();

        script.stop();

        assertTrue(script.awaitStopped(TIMEOUT_MS));
        assertEquals(1, script.stops.get());
        assertEquals(1, script.starts.get(), "a second start on a running script does nothing");
    }

    @Test
    void restartWaitsForOldCleanupAndStopCompletionIsRunSpecific() throws Exception {
        TestScript script = newScript();
        CountDownLatch release = new CountDownLatch(1);
        script.holdLoopOn(release);
        script.start();
        script.onGameTick(TICK);
        assertTrue(script.loopEntered.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        java.util.concurrent.CompletionStage<Void> stopped = script.stopAsync();
        script.start();
        assertEquals(1, script.starts.get());
        assertEquals(Script.State.STOPPING, script.getState());
        release.countDown();
        stopped.toCompletableFuture().get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertEquals(1, script.stops.get());
        script.holdLoopOn(null);
        assertTrue(script.awaitLoops(2, TIMEOUT_MS, () -> script.onGameTick(TICK)));
        assertEquals(2, script.starts.get());
        assertEquals(1, script.stopsSeenAtLastStart);
    }

    @Test
    void stopDuringStartupWaitsForHookAndDoesNotPublishRunning() throws Exception {
        TestScript script = newScript();
        script.holdStart = new CountDownLatch(1);
        java.util.concurrent.ExecutorService starter = java.util.concurrent.Executors.newSingleThreadExecutor();
        try {
            java.util.concurrent.Future<?> starting = starter.submit(script::start);
            assertTrue(script.startEntered.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
            java.util.concurrent.CompletionStage<Void> stopped = script.stopAsync();
            assertFalse(stopped.toCompletableFuture().isDone());
            script.onGameTick(TICK);
            assertEquals(0, script.loops.get());
            assertEquals(0, script.stops.get());
            script.holdStart.countDown();
            starting.get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
            stopped.toCompletableFuture().get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
            assertEquals(Script.State.STOPPED, script.getState());
            assertEquals(1, script.stops.get());
        } finally {
            script.holdStart.countDown();
            starter.shutdownNow();
        }
    }

    @Test
    void failedStartupDuringStopCompletesWithoutRunningCleanupForUninitializedRun() throws Exception {
        TestScript script = newScript();
        script.holdStart = new CountDownLatch(1);
        script.failOnStart = true;
        java.util.concurrent.ExecutorService starter = java.util.concurrent.Executors.newSingleThreadExecutor();
        try {
            java.util.concurrent.Future<?> starting = starter.submit(script::start);
            assertTrue(script.startEntered.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
            java.util.concurrent.CompletionStage<Void> stopped = script.stopAsync();
            script.holdStart.countDown();
            assertThrows(java.util.concurrent.ExecutionException.class,
                    () -> starting.get(TIMEOUT_MS, TimeUnit.MILLISECONDS));
            stopped.toCompletableFuture().get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
            assertEquals(Script.State.STOPPED, script.getState());
            assertEquals(0, script.stops.get());
        } finally {
            script.holdStart.countDown();
            starter.shutdownNow();
        }
    }

    @Test
    void pauseAllowsInFlightWorkButConcurrentTicksCannotQueueExtraLoops() throws Exception {
        TestScript script = newScript();
        CountDownLatch release = new CountDownLatch(1);
        script.holdLoopOn(release);
        script.start();
        java.util.concurrent.ExecutorService ticks = java.util.concurrent.Executors.newFixedThreadPool(8);
        try {
            java.util.List<java.util.concurrent.Future<?>> submissions = new java.util.ArrayList<>();
            for (int i = 0; i < 100; i++) submissions.add(ticks.submit(() -> script.onGameTick(TICK)));
            for (java.util.concurrent.Future<?> tick : submissions) tick.get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
            assertTrue(script.loopEntered.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
            script.pause();
            assertEquals(1, script.loops.get());
            script.stop();
            release.countDown();
            assertTrue(script.awaitStopped(TIMEOUT_MS));
            assertEquals(1, script.loops.get());
        } finally {
            release.countDown();
            ticks.shutdownNow();
        }
    }

    @Test
    void startupRegistrationFailureRollsBackAndAllowsAnotherStart() {
        TestScript script = newScript();
        org.mockito.Mockito.doThrow(new IllegalStateException("registration failed"))
                .doNothing().when(script.bus).register(script);
        assertThrows(IllegalStateException.class, script::start);
        assertEquals(Script.State.STOPPED, script.getState());
        assertTrue(script.awaitStopped(TIMEOUT_MS));
        org.mockito.Mockito.verify(script.bus).unregister(script);
        script.start();
        assertEquals(Script.State.RUNNING, script.getState());
    }

    @Test
    void stopCallbackRunsAndCompletesEvenIfCleanupThrows() {
        TestScript script = newScript();
        script.failOnStop = true;
        AtomicInteger callbacks = new AtomicInteger();
        script.start();
        script.stop(callbacks::incrementAndGet);
        assertTrue(script.awaitStopped(TIMEOUT_MS));
        assertEquals(1, callbacks.get());
        assertEquals(Script.State.STOPPED, script.getState());
    }

    /**
     * A script whose loop can be held open, made to fail on startup, or given a long delay, so a test
     * can drive it into each interleaving deliberately.
     */
    private static final class TestScript extends Script {

        private final AtomicInteger loops = new AtomicInteger();
        private final AtomicInteger starts = new AtomicInteger();
        private final AtomicInteger stops = new AtomicInteger();
        private volatile CountDownLatch loopEntered = new CountDownLatch(1);
        private volatile CountDownLatch hold;
        private volatile CountDownLatch holdStart;
        private final CountDownLatch startEntered = new CountDownLatch(1);
        private volatile int stopsSeenAtLastStart;
        private final EventBus bus = mock(EventBus.class);
        private volatile boolean failOnStop;
        private volatile boolean failOnStart;
        private volatile int delayMs;

        TestScript() {
            inject(this, "eventBus", bus);
        }

        void holdLoopOn(CountDownLatch latch) {
            this.hold = latch;
        }

        @Override
        public int loop() {
            loops.incrementAndGet();
            loopEntered.countDown();
            CountDownLatch latch = hold;
            if (latch != null) {
                try {
                    latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            return delayMs;
        }

        @Override
        public void onStart() {
            starts.incrementAndGet();
            stopsSeenAtLastStart = stops.get();
            startEntered.countDown();
            if (holdStart != null) {
                try {
                    if (!holdStart.await(TIMEOUT_MS, TimeUnit.MILLISECONDS)) throw new AssertionError("startup timed out");
                } catch (InterruptedException e) {
                    throw new AssertionError(e);
                }
            }
            if (failOnStart) {
                throw new IllegalStateException("startup failed");
            }
        }

        @Override
        public void onStop() {
            stops.incrementAndGet();
            if (failOnStop) throw new IllegalStateException("cleanup failed");
        }

        /**
         * Ticks the script until it has run at least the given number of loops.
         *
         * @param wanted    how many loops to wait for
         * @param timeoutMs how long to keep ticking, in milliseconds
         * @param tick      fires one game tick
         * @return true when the script reached that many loops in time
         */
        boolean awaitLoops(int wanted, long timeoutMs, Runnable tick) throws InterruptedException {
            long deadline = System.currentTimeMillis() + timeoutMs;
            while (System.currentTimeMillis() < deadline) {
                if (loops.get() >= wanted) {
                    return true;
                }
                tick.run();
                Thread.sleep(10);
            }
            return false;
        }
    }

    /**
     * Sets one of {@link Script}'s injected fields, which Guice would otherwise populate.
     *
     * @param target    the script to inject into
     * @param fieldName the field to set
     * @param value     the value to set it to
     */
    private static void inject(Script target, String fieldName, Object value) {
        try {
            Field field = Script.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Script no longer has a '" + fieldName + "' field", e);
        }
    }
}
