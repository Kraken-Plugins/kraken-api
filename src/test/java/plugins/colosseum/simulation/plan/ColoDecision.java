package plugins.colosseum.simulation.plan;

import plugins.colosseum.simulation.ColoCoords;
import plugins.colosseum.simulation.ColoGrid;
import plugins.colosseum.simulation.PlayerCommand;
import lombok.Getter;
import net.runelite.api.Prayer;
import net.runelite.api.coords.WorldPoint;

/**
 * The planner's output for one tick: everything the executor should do right now, plus the
 * evidence (scores, predicted path, reasoning) the debug overlay renders.
 */
@Getter
public final class ColoDecision {
    private final long command;
    private final ColoGrid grid;

    private final double score;
    private final String reasoning;
    private final int rollouts;
    private final long elapsedNanos;
    private final int horizonTicks;

    private final int attackNpcSlot;
    private final int attackNpcRuneliteIndex;
    private final int gearSetIndex;

    private final int predictedHpAtHorizon;
    private final int predictedWorstCaseFloor;
    private final int predictedDamageTaken;
    private final int predictedKills;

    private final short[] candidateTiles;
    private final double[] candidateScores;
    private final short[] plannedPath;

    ColoDecision(
            long command,
            ColoGrid grid,
            double score,
            String reasoning,
            int rollouts,
            long elapsedNanos,
            int horizonTicks,
            int attackNpcSlot,
            int attackNpcRuneliteIndex,
            int gearSetIndex,
            int predictedHpAtHorizon,
            int predictedWorstCaseFloor,
            int predictedDamageTaken,
            int predictedKills,
            short[] candidateTiles,
            double[] candidateScores,
            short[] plannedPath
    ) {
        this.command = command;
        this.grid = grid;
        this.score = score;
        this.reasoning = reasoning;
        this.rollouts = rollouts;
        this.elapsedNanos = elapsedNanos;
        this.horizonTicks = horizonTicks;
        this.attackNpcSlot = attackNpcSlot;
        this.attackNpcRuneliteIndex = attackNpcRuneliteIndex;
        this.gearSetIndex = gearSetIndex;
        this.predictedHpAtHorizon = predictedHpAtHorizon;
        this.predictedWorstCaseFloor = predictedWorstCaseFloor;
        this.predictedDamageTaken = predictedDamageTaken;
        this.predictedKills = predictedKills;
        this.candidateTiles = candidateTiles;
        this.candidateScores = candidateScores;
        this.plannedPath = plannedPath;
    }

    /** @return movement destination for this tick, or null when holding position. */
    public WorldPoint getMoveDestination() {
        short dest = PlayerCommand.moveTarget(command);
        if (!ColoCoords.isPresent(dest)) {
            return null;
        }
        return grid.toWorld(dest);
    }

    /** @return true when the movement click should be a run click. */
    public boolean isRun() {
        return PlayerCommand.run(command);
    }

    /**
     * @return overhead prayer to activate this tick, or null to keep the current overhead.
     * {@link #isPrayerOff()} distinguishes an explicit "turn overheads off".
     */
    public Prayer getPrayerToActivate() {
        switch (PlayerCommand.overhead(command)) {
            case PlayerCommand.OVERHEAD_MELEE:
                return Prayer.PROTECT_FROM_MELEE;
            case PlayerCommand.OVERHEAD_MISSILES:
                return Prayer.PROTECT_FROM_MISSILES;
            case PlayerCommand.OVERHEAD_MAGIC:
                return Prayer.PROTECT_FROM_MAGIC;
            default:
                return null;
        }
    }

    /** @return true when overhead prayers should be switched off this tick. */
    public boolean isPrayerOff() {
        return PlayerCommand.overhead(command) == PlayerCommand.OVERHEAD_OFF;
    }

    /** @return true when the executor should eat primary food this tick. */
    public boolean isEatFood() {
        return PlayerCommand.eatFood(command);
    }

    /** @return true when the executor should eat combo food this tick. */
    public boolean isEatCombo() {
        return PlayerCommand.eatCombo(command);
    }

    /** @return true when the executor should sip a brew dose this tick. */
    public boolean isSipBrew() {
        return PlayerCommand.sipBrew(command);
    }

    /** @return true when the executor should sip a restore dose this tick. */
    public boolean isSipRestore() {
        return PlayerCommand.sipRestore(command);
    }

    /** @return true when the executor should use the special attack. */
    public boolean isUseSpec() {
        return PlayerCommand.useSpec(command);
    }

    /**
     * @return true when this decision issues a NEW attack click this tick. When the player
     * is already attacking the plan's target, combat continues on its own and no click is
     * needed - getAttackNpcRuneliteIndex() still names the plan's target for
     * display purposes.
     */
    public boolean hasAttack() {
        return PlayerCommand.attackTarget(command) >= 0 && attackNpcRuneliteIndex >= 0;
    }

    /** @return candidate destination count for visualization. */
    public int candidateCount() {
        return candidateTiles == null ? 0 : candidateTiles.length;
    }

    /**
     * @param index candidate index.
     * @return candidate destination as a world point.
     */
    public WorldPoint candidateWorldPoint(int index) {
        return grid.toWorld(candidateTiles[index]);
    }

    /**
     * @param index candidate index.
     * @return candidate plan score.
     */
    public double candidateScore(int index) {
        return candidateScores[index];
    }

    /** @return predicted player path (world points) along the best plan. */
    public WorldPoint[] plannedPathWorld() {
        if (plannedPath == null) {
            return new WorldPoint[0];
        }
        WorldPoint[] points = new WorldPoint[plannedPath.length];
        for (int i = 0; i < plannedPath.length; i++) {
            points[i] = grid.toWorld(plannedPath[i]);
        }
        return points;
    }
}
