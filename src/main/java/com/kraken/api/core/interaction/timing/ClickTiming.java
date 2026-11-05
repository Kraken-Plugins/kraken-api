package com.kraken.api.core.interaction.timing;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Value;

/**
 * When a scheduled click should leave inside the current game tick, in milliseconds after the tick started.
 *
 * <p>A click sent before the server's cutoff (roughly {@code 600 - round trip - processing time} ms into the tick)
 * applies on the next tick, exactly like a click sent the instant the tick started. ClickScheduler sends the
 * click at getOffsetMs() when no other click is too close to it, and sends it no later than the last client
 * cycle before getDeadlineMs() whatever else is happening.
 */
@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ClickTiming {

    /**
     * Marks a deadline that follows the scheduler's cutoff, ClickScheduler#getCutoffMs().
     */
    public static final int SCHEDULER_CUTOFF = -1;

    private static final ClickTiming IMMEDIATE = new ClickTiming(true, 0, SCHEDULER_CUTOFF);

    /**
     * True when the click is sent within the call that schedules it, the way every interaction worked before
     * scheduling existed.
     */
    boolean immediate;

    /**
     * Milliseconds after the start of the tick at which the click should leave.
     */
    int offsetMs;

    /**
     * Milliseconds after the start of the tick by which the click must have left, or {@link #SCHEDULER_CUTOFF}.
     */
    int deadlineMs;

    /**
     * A click sent within the call that schedules it.
     *
     * @return the immediate timing.
     */
    public static ClickTiming immediate() {
        return IMMEDIATE;
    }

    /**
     * A click sent {@code offsetMs} after the tick started, and before the scheduler's cutoff.
     *
     * @param offsetMs milliseconds after the start of the tick, 0 or more.
     * @return the timing.
     */
    public static ClickTiming at(int offsetMs) {
        return at(offsetMs, SCHEDULER_CUTOFF);
    }

    /**
     * A click sent {@code offsetMs} after the tick started, and no later than {@code deadlineMs}.
     *
     * @param offsetMs   milliseconds after the start of the tick, 0 or more.
     * @param deadlineMs milliseconds after the start of the tick by which the click must leave, or
     *                   {@link #SCHEDULER_CUTOFF}.
     * @return the timing.
     */
    public static ClickTiming at(int offsetMs, int deadlineMs) {
        if (offsetMs < 0) {
            throw new IllegalArgumentException("offsetMs must not be negative: " + offsetMs);
        }
        if (deadlineMs < 0 && deadlineMs != SCHEDULER_CUTOFF) {
            throw new IllegalArgumentException("deadlineMs must not be negative: " + deadlineMs);
        }
        return new ClickTiming(false, offsetMs, deadlineMs);
    }
}
