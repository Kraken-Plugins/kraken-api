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
 *
 * <p>Everything this class runs on the EDT is marked: {@link #isSynthetic()} is true while it runs, so a
 * {@code MouseListener} or {@code KeyListener} can tell Kraken's input ({@code VirtualMouse}, keyboard typing, the
 * idle reset keypress) from the player's own, because those listeners run inside the same
 * {@code dispatchEvent} call.</p>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class InputDispatch {

    private static final ThreadLocal<int[]> DEPTH = ThreadLocal.withInitial(() -> new int[1]);

    /**
     * Queues an event for delivery to its target on the event dispatch thread.
     * @param target The component that receives the event
     * @param event The event to deliver
     */
    public static void dispatch(Component target, AWTEvent event) {
        onEventThread(() -> target.dispatchEvent(event));
    }

    /**
     * Runs an action on the event dispatch thread, inline when already there and queued otherwise. Input events the
     * action dispatches are marked as synthetic while it runs.
     * @param action The action to run
     */
    public static void onEventThread(Runnable action) {
        Runnable marked = () -> runMarked(action);
        if (EventQueue.isDispatchThread()) {
            marked.run();
        } else {
            EventQueue.invokeLater(marked);
        }
    }

    /**
     * Reports whether the calling thread is delivering input that Kraken posted through this class. Call it from an
     * input listener: it is true for {@code VirtualMouse} clicks and moves, typed text and the idle reset keypress,
     * and false for the player's own mouse and keyboard.
     *
     * @return true while an event posted through this class is being delivered on this thread.
     */
    public static boolean isSynthetic() {
        return DEPTH.get()[0] > 0;
    }

    private static void runMarked(Runnable action) {
        int[] depth = DEPTH.get();
        depth[0]++;
        try {
            action.run();
        } finally {
            depth[0]--;
        }
    }
}
