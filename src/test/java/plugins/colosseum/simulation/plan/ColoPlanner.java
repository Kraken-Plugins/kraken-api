package plugins.colosseum.simulation.plan;

import plugins.colosseum.simulation.ColoCoords;
import plugins.colosseum.simulation.ColoConstants;
import plugins.colosseum.simulation.ColoFrame;
import plugins.colosseum.simulation.ColoGrid;
import plugins.colosseum.simulation.ColoLos;
import plugins.colosseum.simulation.ColoNpcType;
import plugins.colosseum.simulation.ColoScratch;
import plugins.colosseum.simulation.ColoState;
import plugins.colosseum.simulation.ColoTick;
import plugins.colosseum.simulation.LoadoutConfig;
import plugins.colosseum.simulation.PlayerCommand;

/**
 * Real-time, time-budgeted plan search over the colosseum tick engine.
 *
 * <p>The planner searches over <b>macro plans</b> - a movement destination paired with an
 * attack target - and rolls each plan out for a fixed horizon with deterministic in-rollout
 * policies handling the "forced" decisions every tick:</p>
 *
 * <ul>
 *   <li><b>Prayer policy</b>: predicts which NPCs launch an attack next tick (cooldowns,
 *   manticore orb sequences, warband metronome, post-move line-of-sight gains) and prays
 *   against the largest expected damage - exactly what a strong flicker does.</li>
 *   <li><b>Consumable policy</b>: eats/sips on HP and prayer thresholds, respecting the
 *   engine's food/combo/potion timers (food beats offence - survival is priced into the
 *   scorer, so a plan that skips a needed eat simply scores worse).</li>
 *   <li><b>Retargeting policy</b>: when the plan's target dies mid-rollout, retargets by
 *   kill-order priority and swaps to the gear set with the best expected damage.</li>
 * </ul>
 *
 * <p>Candidate destinations combine the player's immediate neighbourhood, the nearest
 * zero-exposure cover tiles from the {@link DangerMap}, and low-danger attack positions
 * with line of sight to the primary target. The best first-stage plans then get a second
 * stage of follow-up destinations ("duck behind the pillar, then swing out"), all under a
 * hard wall-clock budget - the planner returns its best-so-far at the deadline.</p>
 *
 * <p>Everything is deterministic and allocation-free after warmup: states are pooled,
 * commands are packed longs, and rollouts reuse one scratch. A full pass is typically a
 * few thousand simulated ticks, well inside a 15 ms budget.</p>
 */
public final class ColoPlanner {
    private static final int MAX_CANDIDATES = 64;

    /** Kill-order priority (research-backed): warband first (melee, mage, range), then
     *  shamans and jaguars, then the big colossi, minotaurs last. */
    private static final ColoNpcType[] KILL_PRIORITY = {
            ColoNpcType.FREMENNIK_BERSERKER,
            ColoNpcType.FREMENNIK_SEER,
            ColoNpcType.FREMENNIK_ARCHER,
            ColoNpcType.SERPENT_SHAMAN,
            ColoNpcType.JAGUAR_WARRIOR,
            ColoNpcType.MANTICORE,
            ColoNpcType.SHOCKWAVE_COLOSSUS,
            ColoNpcType.JAVELIN_COLOSSUS,
            ColoNpcType.MINOTAUR,
            ColoNpcType.MINOTAUR_RED_FLAG,
    };

    private final ColoScratch scratch = new ColoScratch();
    private final DangerMap dangerMap = new DangerMap();
    private final ColoState rollout = new ColoState();
    private final ColoState replay = new ColoState();
    private final ColoState[] stage2Roots;
    private final long[] stage2Commands;
    private final int[] stage2Targets;
    private final double[] stage2Scores;
    private final int[] stage2CandidateIndex;

    private final short[] candidateTiles = new short[MAX_CANDIDATES];
    private final double[] candidateBestScores = new double[MAX_CANDIDATES];
    private final boolean[] seenTile = new boolean[ColoCoords.MAX_DIMENSION * ColoCoords.MAX_DIMENSION];
    private final int[] targetSlots = new int[8];
    private final double[] styleDamage = new double[4];

    private ColoGrid lastGrid;

    /**
     * Creates a planner. One instance per decision thread; instances are not thread-safe.
     */
    public ColoPlanner() {
        stage2Roots = new ColoState[8];
        for (int i = 0; i < stage2Roots.length; i++) {
            stage2Roots[i] = new ColoState();
        }
        stage2Commands = new long[8];
        stage2Targets = new int[8];
        stage2Scores = new double[8];
        stage2CandidateIndex = new int[8];
    }

    /** @return the danger map from the last {@link #plan} call, for overlays. */
    public DangerMap dangerMap() {
        return dangerMap;
    }

    /**
     * Searches for the best command for the upcoming tick.
     *
     * @param root current state (not mutated).
     * @param options tuning options.
     * @return decision for this tick with visualization data.
     */
    public ColoDecision plan(ColoState root, PlannerOptions options) {
        long start = System.nanoTime();
        long deadline = start + options.getBudgetNanos();
        ColoFrame frame = root.frame();
        ColoGrid grid = frame.getGrid();
        if (grid != lastGrid) {
            scratch.invalidate();
            lastGrid = grid;
        }

        dangerMap.compute(root, options.getDangerRadius());

        int targetCount = collectTargets(root, options);
        int primaryTarget = targetCount > 0 ? targetSlots[0] : -1;
        int candidateCount = collectDestinations(root, options, primaryTarget);

        java.util.Arrays.fill(candidateBestScores, 0, candidateCount, Double.NEGATIVE_INFINITY);

        double bestScore = Double.NEGATIVE_INFINITY;
        long bestCommand = PlayerCommand.NONE;
        int bestTarget = -1;
        int bestCandidate = -1;
        int rollouts = 0;
        int stage2Count = 0;

        for (int ci = 0; ci < candidateCount; ci++) {
            short dest = candidateTiles[ci];
            int perTarget = Math.max(1, targetCount);
            for (int ti = 0; ti < perTarget; ti++) {
                int target = targetCount > 0 ? targetSlots[ti] : -1;
                long firstCommand = composeFirstCommand(root, dest, target, options);
                rollout.copyFrom(root);
                runRollout(rollout, firstCommand, target, options.getHorizonTicks(), options);
                rollouts++;
                double score = options.getScorer().score(rollout, countLosThreats(rollout));
                if (score > candidateBestScores[ci]) {
                    candidateBestScores[ci] = score;
                }
                if (score > bestScore) {
                    bestScore = score;
                    bestCommand = firstCommand;
                    bestTarget = target;
                    bestCandidate = ci;
                }
                stage2Count = offerStage2(root, options, stage2Count, firstCommand, target, ci, score);
                if (System.nanoTime() > deadline) {
                    break;
                }
            }
            if (System.nanoTime() > deadline) {
                break;
            }
        }

        // Second stage: extend the most promising plans with a follow-up destination.
        long stage2Deadline = deadline;
        for (int pi = 0; pi < stage2Count && System.nanoTime() < stage2Deadline; pi++) {
            ColoState mid = stage2Roots[pi];
            if (mid.playerDied()) {
                continue;
            }
            for (int ci = 0; ci < candidateCount && System.nanoTime() < stage2Deadline; ci++) {
                short dest = candidateTiles[ci];
                rollout.copyFrom(mid);
                long followCommand = composeFirstCommand(rollout, dest, stage2Targets[pi], options);
                runRollout(rollout, followCommand, stage2Targets[pi], options.getStage2HorizonTicks(), options);
                rollouts++;
                double score = options.getScorer().score(rollout, countLosThreats(rollout));
                int rootCandidate = stage2CandidateIndex[pi];
                if (score > candidateBestScores[rootCandidate]) {
                    candidateBestScores[rootCandidate] = score;
                }
                if (score > bestScore) {
                    bestScore = score;
                    bestCommand = stage2Commands[pi];
                    bestTarget = stage2Targets[pi];
                    bestCandidate = rootCandidate;
                }
            }
        }

        // Replay the winning plan to capture the predicted path and end summary.
        replay.copyFrom(root);
        short[] path = replayPath(replay, bestCommand, bestTarget, options);

        long elapsed = System.nanoTime() - start;
        String reasoning = buildReasoning(root, bestCommand, bestTarget, bestScore, candidateCount, targetCount);

        short[] tilesOut = new short[candidateCount];
        double[] scoresOut = new double[candidateCount];
        System.arraycopy(candidateTiles, 0, tilesOut, 0, candidateCount);
        System.arraycopy(candidateBestScores, 0, scoresOut, 0, candidateCount);

        int attackIndex = bestTarget >= 0 ? frame.getNpcRuneliteIndices()[bestTarget] : -1;
        return new ColoDecision(
                bestCommand,
                grid,
                bestScore,
                reasoning,
                rollouts,
                elapsed,
                options.getHorizonTicks(),
                bestTarget,
                attackIndex,
                PlayerCommand.gearSet(bestCommand),
                replay.playerHp(),
                replay.worstCaseHpFloor(),
                replay.expectedDamageTaken(),
                replay.kills(),
                tilesOut,
                scoresOut,
                path
        );
    }

    private int offerStage2(
            ColoState root,
            PlannerOptions options,
            int stage2Count,
            long firstCommand,
            int target,
            int candidateIndex,
            double score
    ) {
        int capacity = Math.min(options.getStage2TopPlans(), stage2Roots.length);
        if (capacity <= 0) {
            return stage2Count;
        }
        int slot;
        if (stage2Count < capacity) {
            slot = stage2Count++;
        } else {
            slot = 0;
            for (int i = 1; i < stage2Count; i++) {
                if (stage2Scores[i] < stage2Scores[slot]) {
                    slot = i;
                }
            }
            if (stage2Scores[slot] >= score) {
                return stage2Count;
            }
        }
        // Save the mid-rollout end state as the second-stage root.
        stage2Roots[slot].copyFrom(rollout);
        stage2Commands[slot] = firstCommand;
        stage2Targets[slot] = target;
        stage2Scores[slot] = score;
        stage2CandidateIndex[slot] = candidateIndex;
        return stage2Count;
    }

    private void runRollout(ColoState state, long firstCommand, int target, int horizon, PlannerOptions options) {
        int currentTarget = target;
        short plannedDest = PlayerCommand.moveTarget(firstCommand);
        for (int t = 0; t < horizon; t++) {
            long cmd = t == 0 ? firstCommand : policyCommand(state, currentTarget, plannedDest, options);
            ColoTick.advance(state, cmd, scratch);
            if (state.playerDied()) {
                return;
            }
            if (currentTarget >= 0 && !state.npcActive(currentTarget)) {
                currentTarget = nextTargetByPriority(state);
            }
        }
    }

    private short[] replayPath(ColoState state, long firstCommand, int target, PlannerOptions options) {
        short[] path = new short[options.getHorizonTicks()];
        int currentTarget = target;
        short plannedDest = PlayerCommand.moveTarget(firstCommand);
        int length = 0;
        for (int t = 0; t < options.getHorizonTicks(); t++) {
            long cmd = t == 0 ? firstCommand : policyCommand(state, currentTarget, plannedDest, options);
            ColoTick.advance(state, cmd, scratch);
            path[length++] = state.playerPos();
            if (state.playerDied()) {
                break;
            }
            if (currentTarget >= 0 && !state.npcActive(currentTarget)) {
                currentTarget = nextTargetByPriority(state);
            }
        }
        short[] trimmed = new short[length];
        System.arraycopy(path, 0, trimmed, 0, length);
        return trimmed;
    }

    // ------------------------------------------------------------------
    // Candidate generation
    // ------------------------------------------------------------------

    private int collectDestinations(ColoState root, PlannerOptions options, int primaryTarget) {
        ColoGrid grid = root.frame().getGrid();
        java.util.Arrays.fill(seenTile, false);
        int count = 0;
        short playerPos = root.playerPos();
        int px = ColoCoords.x(playerPos);
        int py = ColoCoords.y(playerPos);

        count = addCandidate(count, playerPos);
        for (int d = 0; d < 8; d++) {
            int nx = px + ColoScratch.DIR_X[d];
            int ny = py + ColoScratch.DIR_Y[d];
            if (grid.inBounds(nx, ny) && ColoScratch.playerCanStep(grid, px, py, ColoScratch.DIR_X[d], ColoScratch.DIR_Y[d])) {
                count = addCandidate(count, ColoCoords.pack(nx, ny));
            }
        }

        byte[] walkDistance = scratch.playerField(grid, playerPos);

        // Nearest zero-exposure cover tiles.
        count = addBestTiles(
                root, options, count, walkDistance,
                options.getMaxSafeTiles(),
                tile -> dangerMap.losCount(tile) == 0,
                (tile, dist) -> dist
        );

        // Low-danger tiles that keep line of sight (and range) on the primary target.
        if (primaryTarget >= 0) {
            LoadoutConfig.GearSet gear = bestGearFor(root, primaryTarget);
            short targetAnchor = root.npcPos(primaryTarget);
            int targetSize = root.frame().size(primaryTarget);
            count = addBestTiles(
                    root, options, count, walkDistance,
                    options.getMaxAttackTiles(),
                    tile -> ColoLos.tileHasLosToFootprint(grid, tile, gear.getAttackRange(), targetAnchor, targetSize),
                    (tile, dist) -> dangerMap.expectedDamagePerTickX100(tile) * 4 + dist
            );
        }

        return Math.min(count, options.getMaxDestinations());
    }

    private interface TileFilter {
        boolean accept(short tile);
    }

    private interface TileCost {
        int cost(short tile, int walkDistance);
    }

    private int addBestTiles(
            ColoState root,
            PlannerOptions options,
            int count,
            byte[] walkDistance,
            int maxTiles,
            TileFilter filter,
            TileCost cost
    ) {
        if (maxTiles <= 0 || count >= MAX_CANDIDATES) {
            return count;
        }
        ColoGrid grid = root.frame().getGrid();
        int radius = options.getDangerRadius();
        int px = ColoCoords.x(root.playerPos());
        int py = ColoCoords.y(root.playerPos());

        // Fixed-size selection of the lowest-cost qualifying tiles.
        short[] bestTiles = new short[maxTiles];
        int[] bestCosts = new int[maxTiles];
        int found = 0;
        for (int y = Math.max(0, py - radius); y <= Math.min(grid.height() - 1, py + radius); y++) {
            for (int x = Math.max(0, px - radius); x <= Math.min(grid.width() - 1, px + radius); x++) {
                if (grid.isMoveBlocked(x, y)) {
                    continue;
                }
                int idx = (y << 6) | x;
                int dist = walkDistance[idx] & 0xFF;
                if (dist >= ColoScratch.UNREACHABLE) {
                    continue;
                }
                short tile = ColoCoords.pack(x, y);
                if (!filter.accept(tile)) {
                    continue;
                }
                int tileCost = cost.cost(tile, dist);
                if (found < maxTiles) {
                    bestTiles[found] = tile;
                    bestCosts[found] = tileCost;
                    found++;
                } else {
                    int worst = 0;
                    for (int i = 1; i < maxTiles; i++) {
                        if (bestCosts[i] > bestCosts[worst]) {
                            worst = i;
                        }
                    }
                    if (bestCosts[worst] > tileCost) {
                        bestTiles[worst] = tile;
                        bestCosts[worst] = tileCost;
                    }
                }
            }
        }
        for (int i = 0; i < found && count < MAX_CANDIDATES; i++) {
            count = addCandidate(count, bestTiles[i]);
        }
        return count;
    }

    private int addCandidate(int count, short tile) {
        int idx = tile & 0xFFF;
        if (count >= MAX_CANDIDATES || seenTile[idx]) {
            return count;
        }
        seenTile[idx] = true;
        candidateTiles[count] = tile;
        return count + 1;
    }

    private int collectTargets(ColoState root, PlannerOptions options) {
        int count = 0;
        int current = root.attackTargetSlot();
        if (current >= 0 && root.npcActive(current)) {
            targetSlots[count++] = current;
        }
        for (ColoNpcType type : KILL_PRIORITY) {
            if (count >= Math.min(options.getMaxTargets(), targetSlots.length)) {
                break;
            }
            int bestSlot = -1;
            int bestDistance = Integer.MAX_VALUE;
            for (int slot = 0; slot < root.frame().getNpcSlotCount(); slot++) {
                if (!root.npcActive(slot) || root.frame().type(slot) != type || slot == current) {
                    continue;
                }
                int distance = ColoCoords.chebyshev(root.npcPos(slot), root.playerPos());
                if (distance < bestDistance) {
                    bestDistance = distance;
                    bestSlot = slot;
                }
            }
            if (bestSlot >= 0) {
                targetSlots[count++] = bestSlot;
            }
        }
        return count;
    }

    private int nextTargetByPriority(ColoState state) {
        for (ColoNpcType type : KILL_PRIORITY) {
            for (int slot = 0; slot < state.frame().getNpcSlotCount(); slot++) {
                if (state.npcActive(slot) && state.frame().type(slot) == type) {
                    return slot;
                }
            }
        }
        return -1;
    }

    // ------------------------------------------------------------------
    // Rollout policies
    // ------------------------------------------------------------------

    private long composeFirstCommand(ColoState root, short dest, int target, PlannerOptions options) {
        long cmd = basePolicy(root, target, options);
        boolean moving = ColoCoords.isPresent(dest) && dest != root.playerPos();
        if (moving) {
            // A ground click owns the tick; attacking resumes via the rollout policy once
            // in position (or as ready-weapon kite shots along the way).
            cmd = PlayerCommand.withMove(cmd, dest, options.isRun());
        } else {
            if (ColoCoords.isPresent(root.moveTarget())) {
                cmd = PlayerCommand.withStop(cmd);
            }
            if (target >= 0 && root.attackTargetSlot() != target) {
                cmd = PlayerCommand.withAttack(cmd, target);
            }
        }
        return cmd;
    }

    private long policyCommand(ColoState state, int target, short plannedDest, PlannerOptions options) {
        long cmd = basePolicy(state, target, options);
        if (target < 0 || !state.npcActive(target)) {
            // No target: just make sure the planned destination is still being pursued.
            if (!ColoCoords.isPresent(state.moveTarget())
                    && ColoCoords.isPresent(plannedDest)
                    && state.playerPos() != plannedDest) {
                cmd = PlayerCommand.withMove(cmd, plannedDest, options.isRun());
            }
            return cmd;
        }

        LoadoutConfig.GearSet gear = state.frame().getLoadout().gearSet(state.gearSet());
        boolean inRangeNow = ColoLos.tileHasLosToFootprint(
                state.frame().getGrid(),
                state.playerPos(),
                gear.getAttackRange(),
                state.npcPos(target),
                state.frame().size(target)
        );
        boolean traveling = ColoCoords.isPresent(state.moveTarget());
        boolean engaged = state.attackTargetSlot() == target;
        boolean weaponReady = state.attackDelay() <= 0;
        boolean atPlannedDest = !ColoCoords.isPresent(plannedDest) || state.playerPos() == plannedDest;

        if (traveling) {
            // Kite shot: interrupt movement only when the weapon is ready and the target is
            // hittable from the current tile; the next policy tick resumes the path.
            if (weaponReady && inRangeNow && !engaged) {
                cmd = PlayerCommand.withAttack(cmd, target);
            }
        } else if (!engaged) {
            if (inRangeNow && (weaponReady || atPlannedDest)) {
                cmd = PlayerCommand.withAttack(cmd, target);
            } else if (!atPlannedDest) {
                cmd = PlayerCommand.withMove(cmd, plannedDest, options.isRun());
            }
        } else if (!atPlannedDest && state.attackDelay() > 1) {
            // Engaged but off the planned tile with the weapon on cooldown: spend the dead
            // ticks walking to the planned position (classic attack-move weaving).
            cmd = PlayerCommand.withMove(cmd, plannedDest, options.isRun());
        }
        return cmd;
    }

    private long basePolicy(ColoState state, int target, PlannerOptions options) {
        long cmd = PlayerCommand.NONE;

        int overheadCode = policyOverhead(state);
        if (overheadCode != PlayerCommand.OVERHEAD_KEEP) {
            cmd = PlayerCommand.withOverhead(cmd, overheadCode);
        }

        boolean food = false;
        boolean combo = false;
        boolean brew = false;
        boolean restore = false;
        if (state.playerHp() <= options.getEatFoodAtHp() && state.foodCount() > 0) {
            food = true;
        }
        if (state.playerHp() <= options.getEatComboAtHp() && state.comboCount() > 0) {
            combo = true;
        }
        if (!food && state.playerHp() <= options.getSipBrewAtHp() && state.brewDoses() > 0) {
            brew = true;
        }
        if (!brew && state.prayerPoints() <= options.getSipRestoreAtPrayer() && state.restoreDoses() > 0) {
            restore = true;
        }
        if (food || combo || brew || restore) {
            cmd = PlayerCommand.withConsumables(cmd, food, combo, brew, restore);
        }

        if (target >= 0 && state.npcActive(target)) {
            int bestSet = bestGearIndexFor(state, target);
            if (bestSet >= 0 && bestSet != state.gearSet()) {
                cmd = PlayerCommand.withGearSet(cmd, bestSet);
            }
        }
        return cmd;
    }

    /**
     * Predicts the style that deals the most expected damage on the upcoming tick and
     * returns the overhead command blocking it (keeping the current overhead when no
     * launch is predicted).
     */
    private int policyOverhead(ColoState state) {
        java.util.Arrays.fill(styleDamage, 0);
        ColoFrame frame = state.frame();
        double factor = frame.getLoadout().getNpcExpectedDamageFactor();
        int upcomingTick = state.tick();
        boolean canAttack = !frame.isWaveStartGates() || upcomingTick >= ColoConstants.WAVE_START_ATTACK_GATE_TICKS;

        if (canAttack) {
            for (int slot = 0; slot < frame.getNpcSlotCount(); slot++) {
                if (!state.npcActive(slot)) {
                    continue;
                }
                ColoNpcType type = frame.type(slot);
                boolean losNow = ColoTick.npcHasLosToPlayer(state, slot);
                boolean losAfterMove = losNow;
                if (!losNow) {
                    short next = ColoTick.predictNextNpcPos(state, scratch, slot);
                    if (ColoCoords.isPresent(next)) {
                        losAfterMove = ColoLos.footprintHasLosTo(
                                frame.getGrid(), next, type.getSize(), type.getAttackRange(), state.playerPos());
                    }
                }
                if (!losAfterMove) {
                    continue;
                }
                switch (type.getSpecialKind()) {
                    case MANTICORE: {
                        int orbsRemaining = state.manticoreOrbsRemaining(slot);
                        int orbIndex = -1;
                        if (orbsRemaining > 0) {
                            orbIndex = ColoConstants.MANTICORE_ORB_COUNT - orbsRemaining;
                        } else if (state.npcChargingStarted(slot) && state.npcCooldown(slot) <= 1 && losNow) {
                            orbIndex = 0;
                        }
                        if (orbIndex >= 0) {
                            accumulateOrbDamage(state, slot, orbIndex, factor);
                        }
                        break;
                    }
                    case WARBAND:
                        if (upcomingTick % ColoConstants.WARBAND_CYCLE_TICKS == frame.getWarbandCyclePhase() && losNow) {
                            addStyleDamage(styleCodeOf(type), type.getMaxHit() * factor);
                        }
                        break;
                    case MINOTAUR_HEAL:
                        // The melee hit lands one tick after the attack; it is covered by
                        // the pending-hit scan below once launched.
                        if (state.npcCooldown(slot) <= 1 && losAfterMove) {
                            addStyleDamage(ColoTick.STYLE_MELEE, type.getMaxHit() * factor);
                        }
                        break;
                    default:
                        if (state.npcCooldown(slot) <= 1) {
                            double damage = type.getMaxHit() * factor;
                            if (type == ColoNpcType.JAGUAR_WARRIOR) {
                                damage *= ColoTick.JAGUAR_HITS_PER_ATTACK;
                            }
                            addStyleDamage(styleCodeOf(type), damage);
                        }
                        break;
                }
            }
        }

        // Unresolved (prayer-checked-on-landing) hits that land next tick.
        for (int i = 0; i < state.pendingHitCount(); i++) {
            long hit = state.pendingHit(i);
            if (ColoTick.hitTargetsNpc(hit) || ColoTick.hitLandTick(hit) != upcomingTick) {
                continue;
            }
            int style = ColoTick.hitStyle(hit);
            if (style != ColoTick.STYLE_TYPELESS) {
                addStyleDamage(style, ColoTick.hitExpectedDamage(hit));
            }
        }

        int bestStyle = 0;
        double bestDamage = 0;
        for (int style = 1; style <= 3; style++) {
            if (styleDamage[style] > bestDamage) {
                bestDamage = styleDamage[style];
                bestStyle = style;
            }
        }
        if (bestStyle == 0) {
            return PlayerCommand.OVERHEAD_KEEP;
        }
        if (state.overhead() == bestStyle) {
            return PlayerCommand.OVERHEAD_KEEP;
        }
        switch (bestStyle) {
            case ColoTick.STYLE_MELEE:
                return PlayerCommand.OVERHEAD_MELEE;
            case ColoTick.STYLE_RANGED:
                return PlayerCommand.OVERHEAD_MISSILES;
            default:
                return PlayerCommand.OVERHEAD_MAGIC;
        }
    }

    private void accumulateOrbDamage(ColoState state, int slot, int orbIndex, double factor) {
        int pattern = state.manticorePattern(slot);
        if (pattern != ColoTick.PATTERN_UNKNOWN) {
            int style = ColoTick.ORB_STYLES[pattern][orbIndex];
            double damage;
            switch (style) {
                case ColoTick.STYLE_RANGED:
                    damage = ColoTick.MANTICORE_MAX_HIT_RANGED * factor;
                    break;
                case ColoTick.STYLE_MAGIC:
                    damage = ColoTick.MANTICORE_MAX_HIT_MAGIC * factor;
                    break;
                default:
                    damage = ColoTick.MANTICORE_MAX_HIT_MELEE * factor;
                    break;
            }
            addStyleDamage(style, damage);
            return;
        }
        boolean lastOrbKnownMelee = state.frame().getMantimayhemTier() < 3 && orbIndex == 2;
        if (lastOrbKnownMelee) {
            addStyleDamage(ColoTick.STYLE_MELEE, ColoTick.MANTICORE_MAX_HIT_MELEE * factor);
        } else {
            addStyleDamage(ColoTick.STYLE_RANGED, ColoTick.MANTICORE_MAX_HIT_RANGED * factor / 2);
            addStyleDamage(ColoTick.STYLE_MAGIC, ColoTick.MANTICORE_MAX_HIT_MAGIC * factor / 2);
        }
    }

    private void addStyleDamage(int style, double damage) {
        if (style >= 1 && style <= 3) {
            styleDamage[style] += damage;
        }
    }

    private static int styleCodeOf(ColoNpcType type) {
        switch (type.getAttackStyle()) {
            case MELEE:
                return ColoTick.STYLE_MELEE;
            case RANGED:
                return ColoTick.STYLE_RANGED;
            case MAGIC:
                return ColoTick.STYLE_MAGIC;
            default:
                return ColoTick.STYLE_TYPELESS;
        }
    }

    private LoadoutConfig.GearSet bestGearFor(ColoState state, int slot) {
        return state.frame().getLoadout().gearSet(bestGearIndexFor(state, slot));
    }

    private int bestGearIndexFor(ColoState state, int slot) {
        LoadoutConfig loadout = state.frame().getLoadout();
        ColoNpcType type = state.frame().type(slot);
        int best = 0;
        double bestDamage = -1;
        for (int i = 0; i < loadout.gearSetCount(); i++) {
            double damage = loadout.gearSet(i).expectedDamageVs(type)
                    * Math.max(1, 6 - loadout.gearSet(i).getAttackSpeedTicks());
            if (damage > bestDamage) {
                bestDamage = damage;
                best = i;
            }
        }
        return best;
    }

    private int countLosThreats(ColoState state) {
        int count = 0;
        for (int slot = 0; slot < state.frame().getNpcSlotCount(); slot++) {
            if (state.npcActive(slot) && ColoTick.npcHasLosToPlayer(state, slot)) {
                count++;
            }
        }
        return count;
    }

    private String buildReasoning(
            ColoState root,
            long command,
            int target,
            double score,
            int candidateCount,
            int targetCount
    ) {
        StringBuilder sb = new StringBuilder(160);
        short dest = PlayerCommand.moveTarget(command);
        if (ColoCoords.isPresent(dest)) {
            int losThere = dangerMap.losCount(dest);
            sb.append("Move to (").append(ColoCoords.x(dest)).append(',').append(ColoCoords.y(dest)).append(')');
            sb.append(losThere == 0 ? " [cover]" : " [" + losThere + " NPC LoS]");
        } else {
            sb.append("Hold position");
        }
        switch (PlayerCommand.overhead(command)) {
            case PlayerCommand.OVERHEAD_MELEE:
                sb.append("; pray Melee");
                break;
            case PlayerCommand.OVERHEAD_MISSILES:
                sb.append("; pray Missiles");
                break;
            case PlayerCommand.OVERHEAD_MAGIC:
                sb.append("; pray Magic");
                break;
            case PlayerCommand.OVERHEAD_OFF:
                sb.append("; prayers off");
                break;
            default:
                break;
        }
        if (PlayerCommand.eatFood(command) || PlayerCommand.eatCombo(command)) {
            sb.append("; eat");
        }
        if (PlayerCommand.sipBrew(command)) {
            sb.append("; brew");
        }
        if (PlayerCommand.sipRestore(command)) {
            sb.append("; restore");
        }
        if (target >= 0) {
            sb.append("; attack ").append(root.frame().type(target).getDisplayName());
            int set = PlayerCommand.gearSet(command);
            if (set >= 0) {
                sb.append(" with ").append(root.frame().getLoadout().gearSet(set).getName());
            }
        }
        sb.append(" | score ").append(String.format(java.util.Locale.ROOT, "%.0f", score));
        sb.append(" (").append(candidateCount).append(" tiles x ").append(Math.max(1, targetCount)).append(" targets)");
        return sb.toString();
    }
}
