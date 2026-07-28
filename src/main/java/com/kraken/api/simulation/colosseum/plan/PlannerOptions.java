package com.kraken.api.simulation.colosseum.plan;

import lombok.Builder;
import lombok.Getter;

/**
 * Tuning knobs for {@link ColoPlanner}: time budget, rollout horizon, candidate caps and
 * the consumable policy thresholds used inside rollouts.
 */
@Getter
@Builder(toBuilder = true)
public final class PlannerOptions {
    /** Hard wall-clock budget; the planner returns its best-so-far at the deadline. */
    @Builder.Default
    private final long budgetNanos = 15_000_000L;

    /** Rollout depth in ticks for first-stage plans. */
    @Builder.Default
    private final int horizonTicks = 12;

    /** Additional rollout depth for second-stage (follow-up destination) refinement. */
    @Builder.Default
    private final int stage2HorizonTicks = 10;

    /** Number of top first-stage plans that get second-stage refinement. */
    @Builder.Default
    private final int stage2TopPlans = 4;

    /** Danger map radius around the player. */
    @Builder.Default
    private final int dangerRadius = 12;

    /** Cap on candidate movement destinations per stage. */
    @Builder.Default
    private final int maxDestinations = 24;

    /** Cap on zero-exposure "cover" tiles added as destinations. */
    @Builder.Default
    private final int maxSafeTiles = 8;

    /** Cap on low-danger attack-position tiles added as destinations. */
    @Builder.Default
    private final int maxAttackTiles = 6;

    /** Cap on candidate attack targets considered per destination. */
    @Builder.Default
    private final int maxTargets = 3;

    /** Whether movement clicks use run (walking is only better when conserving energy). */
    @Builder.Default
    private final boolean run = true;

    /** Rollout policy: eat primary food at or below this HP. */
    @Builder.Default
    private final int eatFoodAtHp = 48;

    /** Rollout policy: also eat combo food at or below this HP. */
    @Builder.Default
    private final int eatComboAtHp = 34;

    /** Rollout policy: sip a brew at or below this HP when out of food. */
    @Builder.Default
    private final int sipBrewAtHp = 55;

    /** Rollout policy: sip a restore at or below this many prayer points. */
    @Builder.Default
    private final int sipRestoreAtPrayer = 15;

    /** Scorer used to evaluate rollouts. */
    @Builder.Default
    private final ColoScorer scorer = ColoScorer.defaults();

    /**
     * @return default options tuned for a sub-frame (15 ms) decision.
     */
    public static PlannerOptions defaults() {
        return PlannerOptions.builder().build();
    }
}
