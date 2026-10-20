package com.kraken.api.core.script.breakhandler;


import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Mutable break bookkeeping kept by a {@link BreakManager}: whether a break is active, its timing and reason,
 * and whether the player is logged out waiting to resume.
 */
@Getter
@Setter
public class BreakState {
    /** Whether a break is currently active. */
    private boolean onBreak = false;
    /** When the attached script started, or null if it has not started. */
    private Instant scriptStartTime;
    /** When the current break started, or null when no break has started. */
    private Instant breakStartTime;
    /** When the current break is due to end, or null when no break has been scheduled. */
    private Instant breakEndTime;
    /** When the next break is scheduled to start, or null when none is scheduled. */
    private Instant nextBreakTime;
    /** The reason the current break was started, or null when none has been recorded. */
    private String breakReason;
    /** Whether the player is logged out during a break and waiting to log back in. */
    private boolean awaitingLogin = false;

    /**
     * Clears all break state, returning every field to its initial value.
     */
    public void reset() {
        onBreak = false;
        scriptStartTime = null;
        breakStartTime = null;
        breakEndTime = null;
        nextBreakTime = null;
        breakReason = null;
        awaitingLogin = false;
    }

    /**
     * Checks if we should resume after login (break ended while logged out).
     * @return True if the script should resume after re-logging in from a break
     */
    public boolean shouldResumeAfterLogin() {
        return awaitingLogin && breakEndTime != null && Instant.now().isAfter(breakEndTime);
    }
}
