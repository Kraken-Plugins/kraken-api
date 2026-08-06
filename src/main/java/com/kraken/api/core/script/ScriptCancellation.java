package com.kraken.api.core.script;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Per-script cancellation signal.
 *
 * <p>Each {@link Script} owns one token and binds it to the thread its {@code loop()} runs on. Blocking
 * helpers such as {@code SleepService} consult {@link #currentThreadCancelled()}, which resolves to the
 * token belonging to the script that owns the calling thread. Cancelling one script therefore affects
 * only that script, leaving concurrently running plugins untouched.</p>
 *
 * <p>Threads with no bound token — the client thread, plugin event handlers, anything the API does not
 * own — are never reported as cancelled.</p>
 */
public final class ScriptCancellation {

    private static final ThreadLocal<ScriptCancellation> CURRENT = new ThreadLocal<>();

    private final AtomicBoolean cancelled = new AtomicBoolean(false);

    /**
     * Requests cancellation of the script owning this token.
     */
    public void cancel() {
        cancelled.set(true);
    }

    /**
     * Reports whether this token has been cancelled.
     * @return true once {@link #cancel()} has been called.
     */
    public boolean isCancelled() {
        return cancelled.get();
    }

    /**
     * Binds this token to the calling thread for the duration of a script loop.
     */
    void bindToCurrentThread() {
        CURRENT.set(this);
    }

    /**
     * Releases the calling thread's binding.
     *
     * <p>Removes rather than nulls the entry so a pooled thread does not retain a reference to a
     * finished script.</p>
     */
    static void unbindFromCurrentThread() {
        CURRENT.remove();
    }

    /**
     * Reports whether the script owning the calling thread has been cancelled.
     *
     * @return true if a token is bound to this thread and has been cancelled; false otherwise,
     *         including when no script owns this thread.
     */
    public static boolean currentThreadCancelled() {
        ScriptCancellation token = CURRENT.get();
        return token != null && token.isCancelled();
    }

    /**
     * Cancels the script owning the calling thread, if any.
     *
     * <p>Does nothing on threads the API does not own.</p>
     */
    public static void cancelCurrentThread() {
        ScriptCancellation token = CURRENT.get();
        if (token != null) {
            token.cancel();
        }
    }
}
