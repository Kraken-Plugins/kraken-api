package com.kraken.api.input;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.EventQueue;

/**
 * Posts synthetic AWT input events to the event dispatch thread.
 *
 * <p>{@link Component#dispatchEvent} runs the component's listeners synchronously on the calling thread,
 * so calling it from a script worker or the client thread hands the client's mouse and key handlers to a
 * thread they do not expect. Every event is queued on the EDT instead; the queue is FIFO, so a sequence
 * posted from one thread is delivered in order. Posting never blocks, which keeps a caller on the client
 * thread from deadlocking against an EDT that is itself waiting on the client thread. Timing between
 * events (typing cadence, movement steps) stays on the calling worker, never on the EDT.</p>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class InputDispatch {

    /**
     * Queues an event for delivery to its target on the event dispatch thread.
     * @param target The component that receives the event
     * @param event The event to deliver
     */
    public static void dispatch(Component target, AWTEvent event) {
        onEventThread(() -> target.dispatchEvent(event));
    }

    /**
     * Runs an action on the event dispatch thread, inline when already there and queued otherwise.
     * @param action The action to run
     */
    public static void onEventThread(Runnable action) {
        if (EventQueue.isDispatchThread()) {
            action.run();
        } else {
            EventQueue.invokeLater(action);
        }
    }
}
