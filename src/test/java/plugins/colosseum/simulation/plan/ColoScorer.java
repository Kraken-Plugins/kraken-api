package plugins.colosseum.simulation.plan;

import plugins.colosseum.simulation.ColoState;
import lombok.Builder;
import lombok.Getter;

/**
 * Scores a rolled-out future. Larger is better.
 *
 * <p>The score is survival-dominated: dying (or a worst-case burst that could have killed)
 * outweighs any amount of kill progress, matching the priority requirement that eating and
 * defence beat offence. Within survivable futures the scorer rewards kills and damage
 * output, and mildly punishes supply and prayer expenditure so the planner does not burn
 * resources without need.</p>
 */
@Getter
@Builder(toBuilder = true)
public final class ColoScorer {
    /** Flat penalty for futures where the expected-damage trajectory kills the player. */
    @Builder.Default
    private final double deathPenalty = 1_000_000;

    /** Penalty per hitpoint the worst-case burst floor dips below zero (could have died). */
    @Builder.Default
    private final double lethalRiskPerHp = 4_000;

    /** Reward per remaining player hitpoint at the horizon. */
    @Builder.Default
    private final double hpWeight = 6;

    /** Penalty per expected hitpoint of damage taken during the rollout. */
    @Builder.Default
    private final double damageTakenWeight = 14;

    /** Reward per NPC killed during the rollout. */
    @Builder.Default
    private final double killWeight = 900;

    /** Reward per expected hitpoint of damage dealt. */
    @Builder.Default
    private final double damageDealtWeight = 6;

    /** Penalty per supply consumed. */
    @Builder.Default
    private final double supplyWeight = 25;

    /** Penalty per prayer point below the cap at the horizon. */
    @Builder.Default
    private final double prayerWeight = 2.5;

    /** Penalty per NPC that has line of sight to the player at the horizon. */
    @Builder.Default
    private final double endExposureWeight = 30;

    /** Small reward for retaining run energy (units of 1%). */
    @Builder.Default
    private final double runEnergyWeight = 0.4;

    /**
     * @return scorer with default weights.
     */
    public static ColoScorer defaults() {
        return ColoScorer.builder().build();
    }

    /**
     * Scores a rollout end state.
     *
     * @param end state after the rollout horizon.
     * @param endLosThreats NPCs with line of sight to the player in the end state.
     * @return score, larger is better.
     */
    public double score(ColoState end, int endLosThreats) {
        double score = 0;
        if (end.playerDied()) {
            // Dying later is marginally better so gradients still exist.
            score -= deathPenalty - end.tick();
        }
        int floor = end.worstCaseHpFloor();
        if (floor < 0) {
            score -= lethalRiskPerHp * -floor;
        }
        score += end.playerHp() * hpWeight;
        score -= end.expectedDamageTaken() * damageTakenWeight;
        score += end.kills() * killWeight;
        score += end.damageDealt() * damageDealtWeight;
        score -= end.suppliesUsed() * supplyWeight;
        score -= (end.frame().getPrayerPointCap() - end.prayerPoints()) * prayerWeight;
        score -= endLosThreats * endExposureWeight;
        score += (end.runEnergyUnits() / 100.0) * runEnergyWeight;
        return score;
    }
}
