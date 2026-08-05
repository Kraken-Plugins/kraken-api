package plugins.api.suite;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Cooperative cancellation for a run, backed by both a flag and a thread interrupt.
 *
 * <p>Two mechanisms are needed because the work being cancelled blocks in two different ways. The
 * flag is polled between phases and inside every {@code Waiter} loop, which covers the engine's own
 * code. The interrupt covers everything below it: {@code SleepService} checks
 * {@code Thread.currentThread().isInterrupted()} in each of its wait loops and throws, and
 * {@code TaskChain} blocks in a raw {@code Thread.sleep}, so an interrupt is the only thing that can
 * unstick either of them promptly.</p>
 *
 * <p>Deliberately <b>not</b> built on {@code RunnableTask.cancel()}, even though {@code SleepService}
 * honours that flag too. It is a global static shared with every running {@code Script}, and
 * {@code RunnableTask.run()} resets it in a finally block — so cancelling a run would also kill the
 * user's script, and a script finishing at the wrong moment would silently un-cancel the run.</p>
 */
public final class CancellationToken {

    private final AtomicBoolean cancelled = new AtomicBoolean(false);
    private volatile Thread owner;

    /**
     * Binds the token to the thread that will be interrupted on cancellation.
     *
     * @param thread the worker executing the run
     */
    public void bind(Thread thread) {
        this.owner = thread;
    }

    /**
     * Reports whether cancellation has been requested.
     *
     * @return true once {@link #cancel()} has been called
     */
    public boolean isCancelled() {
        return cancelled.get();
    }

    /**
     * Requests cancellation and interrupts the bound worker.
     *
     * <p>Idempotent: only the first call interrupts, so repeated stop presses cannot interrupt a
     * thread that has already moved on to cleanup.</p>
     */
    public void cancel() {
        if (cancelled.compareAndSet(false, true)) {
            Thread thread = owner;
            if (thread != null) {
                thread.interrupt();
            }
        }
    }

    /**
     * Clears the cancelled state so the token can be reused for another run.
     */
    public void reset() {
        cancelled.set(false);
        owner = null;
    }

    /**
     * Aborts the current operation when cancellation has been requested.
     *
     * @param what what was being waited on, used in the exception message
     * @throws TestCancelledException when cancelled or when the calling thread was interrupted
     */
    public void throwIfCancelled(String what) {
        if (cancelled.get() || Thread.currentThread().isInterrupted()) {
            throw new TestCancelledException("Cancelled while " + what);
        }
    }

    /**
     * Aborts the current operation when cancellation has been requested.
     *
     * @throws TestCancelledException when cancelled or when the calling thread was interrupted
     */
    public void throwIfCancelled() {
        throwIfCancelled("running");
    }
}
