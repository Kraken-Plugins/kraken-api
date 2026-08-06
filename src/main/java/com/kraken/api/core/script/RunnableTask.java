package com.kraken.api.core.script;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * A {@link Runnable} wrapper that participates in the script cancellation model.
 *
 * <p>Cancellation is scoped to the calling thread via {@link ScriptCancellation}, so cancelling from
 * inside a script loop affects that script alone and leaves other plugins running.</p>
 */
@Slf4j
public class RunnableTask implements Runnable {

    private final Runnable runnable;

    public RunnableTask(@NonNull Runnable runnable) {
        super();
        this.runnable = runnable;
    }

    /**
     * Requests cancellation of the script that owns the calling thread.
     *
     * <p>Has no effect on threads the API does not own, such as the client thread or a plugin event
     * handler.</p>
     */
    public static void cancel() {
        ScriptCancellation.cancelCurrentThread();
    }

    /**
     * Reports whether the script owning the calling thread has been cancelled.
     * @return true when the calling thread's script has been cancelled.
     */
    public static boolean isCanceled() {
        return ScriptCancellation.currentThreadCancelled();
    }

    @Override
    public void run() {
        if (runnable == null) {
            return;
        }
        try {
            runnable.run();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Runnable Task threw an exception: ", e);
        }
    }
}
