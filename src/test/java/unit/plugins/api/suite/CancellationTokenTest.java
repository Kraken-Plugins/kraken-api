package unit.plugins.api.suite;

import org.junit.jupiter.api.Test;
import plugins.api.suite.CancellationToken;
import plugins.api.suite.TestCancelledException;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers cancellation. This is what makes a Stop button actually stop something: the previous harness
 * called {@code CompletableFuture.cancel(true)}, which cannot interrupt a running task, so tests kept
 * going after being "cancelled".
 */
class CancellationTokenTest {

    @Test
    void aFreshTokenIsNotCancelled() {
        CancellationToken token = new CancellationToken();

        assertFalse(token.isCancelled());
        assertDoesNotThrow(() -> token.throwIfCancelled("working"));
    }

    @Test
    void cancellingRaisesTheFlagAndThrows() {
        CancellationToken token = new CancellationToken();
        token.cancel();

        assertTrue(token.isCancelled());
        assertThrows(TestCancelledException.class, () -> token.throwIfCancelled("working"));
    }

    @Test
    void theExceptionSaysWhatWasBeingWaitedOn() {
        CancellationToken token = new CancellationToken();
        token.cancel();

        TestCancelledException thrown = assertThrows(TestCancelledException.class,
                () -> token.throwIfCancelled("opening the bank"));

        assertTrue(thrown.getMessage().contains("opening the bank"));
    }

    @Test
    void cancellingInterruptsTheBoundWorker() throws Exception {
        // The whole point: a worker blocked in a sleep has to actually wake up.
        CancellationToken token = new CancellationToken();
        CountDownLatch started = new CountDownLatch(1);
        AtomicBoolean interrupted = new AtomicBoolean(false);

        Thread worker = new Thread(() -> {
            token.bind(Thread.currentThread());
            started.countDown();
            try {
                Thread.sleep(30_000);
            } catch (InterruptedException e) {
                interrupted.set(true);
            }
        });

        worker.setDaemon(true);
        worker.start();
        assertTrue(started.await(5, TimeUnit.SECONDS), "worker should have started");

        token.cancel();
        worker.join(5_000);

        assertFalse(worker.isAlive(), "worker should have woken up and finished");
        assertTrue(interrupted.get(), "worker should have been interrupted out of its sleep");
    }

    @Test
    void cancellingTwiceOnlyInterruptsOnce() throws Exception {
        // A second stop press must not interrupt a thread that has already moved on to cleanup, or the
        // cleanup itself gets torn down.
        CancellationToken token = new CancellationToken();
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicBoolean interruptedDuringCleanup = new AtomicBoolean(false);

        Thread worker = new Thread(() -> {
            token.bind(Thread.currentThread());
            started.countDown();
            try {
                release.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            // Simulated cleanup: clear the interrupt then see whether a second cancel sets it again.
            Thread.interrupted();
            token.cancel();
            interruptedDuringCleanup.set(Thread.currentThread().isInterrupted());
        });

        worker.setDaemon(true);
        worker.start();
        assertTrue(started.await(5, TimeUnit.SECONDS));

        token.cancel();
        release.countDown();
        worker.join(5_000);

        assertFalse(interruptedDuringCleanup.get(),
                "a repeat cancel must not re-interrupt the worker during cleanup");
    }

    @Test
    void anInterruptedThreadIsTreatedAsCancelled() {
        // SleepService and TaskChain both surface cancellation as an interrupt, so the token has to
        // recognise one even when its own flag was never set.
        CancellationToken token = new CancellationToken();
        Thread.currentThread().interrupt();

        try {
            assertThrows(TestCancelledException.class, () -> token.throwIfCancelled("working"));
        } finally {
            // Clear the flag so it cannot leak into another test on this thread.
            Thread.interrupted();
        }
    }

    @Test
    void resetMakesATokenReusable() {
        CancellationToken token = new CancellationToken();
        token.cancel();
        token.reset();

        assertFalse(token.isCancelled());
        assertDoesNotThrow(() -> token.throwIfCancelled("working"));
    }

    @Test
    void cancellingWithNoBoundWorkerIsSafe() {
        CancellationToken token = new CancellationToken();

        assertDoesNotThrow(token::cancel);
        assertTrue(token.isCancelled());
    }
}
