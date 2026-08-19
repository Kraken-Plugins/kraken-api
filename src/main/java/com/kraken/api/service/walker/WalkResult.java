package com.kraken.api.service.walker;

import lombok.Getter;
import net.runelite.api.coords.WorldPoint;

/**
 * The result of a walk, carrying enough detail to explain a failure without reading the log.
 */
@Getter
public final class WalkResult {

    private final WalkOutcome outcome;
    private final String reason;
    private final WorldPoint finalLocation;
    private final int rounds;

    private WalkResult(WalkOutcome outcome, String reason, WorldPoint finalLocation, int rounds) {
        this.outcome = outcome;
        this.reason = reason;
        this.finalLocation = finalLocation;
        this.rounds = rounds;
    }

    /**
     * Builds a successful result.
     *
     * @param finalLocation where the player ended up
     * @param rounds how many plan and walk rounds it took
     * @return the result
     */
    public static WalkResult success(WorldPoint finalLocation, int rounds) {
        return new WalkResult(WalkOutcome.SUCCESS, "arrived", finalLocation, rounds);
    }

    /**
     * Builds a failed result.
     *
     * @param outcome why the walk ended
     * @param reason readable detail, shown to whoever has to work out what went wrong
     * @param finalLocation where the player ended up
     * @param rounds how many plan and walk rounds it took
     * @return the result
     */
    public static WalkResult failure(WalkOutcome outcome, String reason, WorldPoint finalLocation, int rounds) {
        return new WalkResult(outcome, reason, finalLocation, rounds);
    }

    /**
     * Reports whether the walk arrived.
     *
     * @return true when the player reached the destination
     */
    public boolean isSuccess() {
        return outcome == WalkOutcome.SUCCESS;
    }

    @Override
    public String toString() {
        return "WalkResult(" + outcome + ": " + reason + " at " + finalLocation + " after " + rounds + " rounds)";
    }
}
