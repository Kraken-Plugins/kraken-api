package unit.com.kraken.api.core.script;

import com.kraken.api.core.script.Script;
import net.runelite.api.events.GameTick;
import net.runelite.client.eventbus.EventBus;
import org.junit.jupiter.api.Test;

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

    private static final GameTick TICK = new GameTick();

    /** How long a test waits for a worker thread to reach a point it should reach immediately. */
    private static final long TIMEOUT_MS = 5_000;

    @Test
    void restartingWhileTheOldLoopIsStillRunningLeavesTheNewRunWorking() throws Exception {
        TestScript script = new TestScript();
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
        TestScript script = new TestScript();
        script.failOnStart = true;

        assertThrows(IllegalStateException.class, script::start);

        assertEquals(Script.State.STOPPED, script.getState());

        // A script left registered here would keep receiving ticks it can never serve.
        script.onGameTick(TICK);
        assertEquals(0, script.loops.get(), "a script that failed to start must not run loops");
    }

    @Test
    void resumeDoesNotStartAScriptThatWasNeverStarted() {
        TestScript script = new TestScript();

        script.resume();

        assertEquals(Script.State.STOPPED, script.getState());
        script.onGameTick(TICK);
        assertEquals(0, script.loops.get(), "resume is not a start");
    }

    @Test
    void resumeDoesNotReviveAStoppedScript() {
        TestScript script = new TestScript();
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
        TestScript script = new TestScript();
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
        TestScript script = new TestScript();
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
        TestScript script = new TestScript();
        script.start();
        script.start();

        script.stop();

        assertTrue(script.awaitStopped(TIMEOUT_MS));
        assertEquals(1, script.stops.get());
        assertEquals(1, script.starts.get(), "a second start on a running script does nothing");
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
        private volatile boolean failOnStart;
        private volatile int delayMs;

        TestScript() {
            inject(this, "eventBus", mock(EventBus.class));
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
            if (failOnStart) {
                throw new IllegalStateException("startup failed");
            }
        }

        @Override
        public void onStop() {
            stops.incrementAndGet();
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
