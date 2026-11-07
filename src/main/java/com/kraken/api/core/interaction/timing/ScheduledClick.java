package com.kraken.api.core.interaction.timing;

import lombok.AccessLevel;
import lombok.Getter;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * A click handed to {@link ClickScheduler}, and how far it has got. Callers keep it to cancel the click or to tell
 * whether it has been sent yet.
 */
public class ScheduledClick {

    /**
     * Where a scheduled click is in its life.
     */
    public enum State {
        /** Waiting for its send time, a gap after the previous click, or its deadline. */
        PENDING,
        /** Its action is running on the client thread. */
        SENDING,
        /** Its action ran and sent a click. */
        SENT,
        /** Its action ran and decided no click was needed, for example because the game was already in the wanted state. */
        SKIPPED,
        /** It was cancelled before it was sent. */
        CANCELLED
    }

    /**
     * The object that scheduled the click, usually a plugin helper. {@link ClickScheduler#cancel(Object)} cancels
     * every pending click of one owner.
     */
    @Getter
    private final Object owner;

    /**
     * When the click should leave, from the scheduler's clock.
     */
    @Getter
    private final long sendAtMillis;

    /**
     * When the click must have left by, from the scheduler's clock.
     */
    @Getter
    private final long deadlineMillis;

    /**
     * When the click was sent or skipped, from the scheduler's clock, or 0 while it has not run.
     */
    @Getter
    private volatile long sentAtMillis;

    @Getter(AccessLevel.PACKAGE)
    private final int baseGapMs;

    @Getter(AccessLevel.PACKAGE)
    private final Supplier<ClickLocation> location;

    @Getter(AccessLevel.PACKAGE)
    private final BooleanSupplier action;

    @Getter(AccessLevel.PACKAGE)
    private final long sequence;

    private final AtomicReference<State> state = new AtomicReference<>(State.PENDING);

    ScheduledClick(Object owner, long sendAtMillis, long deadlineMillis, int baseGapMs, Supplier<ClickLocation> location,
                   BooleanSupplier action, long sequence) {
        this.owner = owner;
        this.sendAtMillis = sendAtMillis;
        this.deadlineMillis = deadlineMillis;
        this.baseGapMs = baseGapMs;
        this.location = location;
        this.action = action;
        this.sequence = sequence;
    }

    /**
     * @return where the click is in its life.
     */
    public State getState() {
        return state.get();
    }

    /**
     * @return true while the click has not been sent, skipped or cancelled.
     */
    public boolean isPending() {
        State current = state.get();
        return current == State.PENDING || current == State.SENDING;
    }

    /**
     * @return true once the click's action has sent a click.
     */
    public boolean isSent() {
        return state.get() == State.SENT;
    }

    /**
     * Cancels the click if it has not started sending.
     *
     * @return true if the click was pending and is now cancelled.
     */
    public boolean cancel() {
        return state.compareAndSet(State.PENDING, State.CANCELLED);
    }

    /**
     * Claims the click for sending, so a concurrent {@link #cancel()} can no longer stop it.
     *
     * @return true if the click was pending and is now sending.
     */
    boolean claim() {
        return state.compareAndSet(State.PENDING, State.SENDING);
    }

    /**
     * Records the outcome of the click's action.
     *
     * @param clicked true if the action sent a click.
     * @param now     the scheduler's clock when the action ran.
     */
    void finish(boolean clicked, long now) {
        sentAtMillis = now;
        state.set(clicked ? State.SENT : State.SKIPPED);
    }
}
