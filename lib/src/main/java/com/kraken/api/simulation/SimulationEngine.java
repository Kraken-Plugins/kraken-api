package com.kraken.api.simulation;

import com.kraken.api.service.map.WorldPointService;
import com.kraken.api.simulation.tree.SimulationTree;
import com.kraken.api.simulation.tree.SimulationTreeNode;
import com.kraken.api.simulation.tree.SimulationTreeOptions;
import com.kraken.api.util.MathUtils;
import net.runelite.api.CollisionDataFlag;
import net.runelite.api.Prayer;
import net.runelite.api.coords.WorldPoint;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Core simulation engine for snapshot-driven tree generation.
 */
public final class SimulationEngine {
    private static final int BLOCKED_MOVEMENT_MASK = CollisionDataFlag.BLOCK_MOVEMENT_FULL | CollisionDataFlag.BLOCK_MOVEMENT_OBJECT;
    private static final int[][] DIRECTIONS_8 = new int[][]{
            {1, 0},
            {-1, 0},
            {0, 1},
            {0, -1},
            {1, 1},
            {1, -1},
            {-1, 1},
            {-1, -1}
    };

    /**
     * Provides extra actions while building a simulation tree.
     */
    @FunctionalInterface
    public interface ActionProvider {
        /**
         * @param state current state.
         * @param depthRemaining depth remaining.
         * @return additional actions, or null.
         */
        List<SimulationAction> provide(SimulationState state, int depthRemaining);
    }

    private static final class ExpansionCounter {
        private int nodeCount;
        private int maxDepthReached;
    }

    private static final class PathStep {
        private final int x;
        private final int y;
        private final int distance;

        private PathStep(int x, int y, int distance) {
            this.x = x;
            this.y = y;
            this.distance = distance;
        }
    }

    /**
     * Creates a root state.
     *
     * @param scenario simulation scenario.
     * @return mutable state.
     */
    public SimulationState createState(SimulationScenario scenario) {
        return SimulationState.fromScenario(scenario);
    }

    /**
     * Creates a root state.
     *
     * @param snapshot snapshot input.
     * @param npcProfilesById npc profile map.
     * @return mutable state.
     */
    public SimulationState createState(SimulationSnapshot snapshot, Map<Integer, SimulationNpcProfile> npcProfilesById) {
        return createState(new SimulationScenario(snapshot, npcProfilesById));
    }

    /**
     * Simulates one tick on a copied state.
     *
     * @param state source state.
     * @param action action to apply.
     * @return copied state after one tick.
     */
    public SimulationState simulateTickCopy(SimulationState state, SimulationAction action) {
        if (state == null) {
            throw new IllegalArgumentException("state cannot be null");
        }
        SimulationState copy = state.copy();
        simulateTick(copy, action);
        return copy;
    }

    /**
     * Simulates one tick in place.
     *
     * @param state source state.
     * @param action action to apply.
     * @return same state instance.
     */
    public SimulationState simulateTick(SimulationState state, SimulationAction action) {
        if (state == null) {
            throw new IllegalArgumentException("state cannot be null");
        }

        SimulationAction resolved = action == null ? SimulationAction.WAIT : action;
        applyPlayerAction(state, resolved);
        advanceQueuedPlayerMovement(state);
        moveNpcs(state);
        resolveNpcAttacks(state);
        state.setLastAppliedAction(resolved);
        state.incrementTick();
        return state;
    }

    /**
     * Generates an outcome tree using defaults.
     *
     * @param scenario scenario input.
     * @return generated tree.
     */
    public SimulationTree generateOutcomeTree(SimulationScenario scenario) {
        return generateOutcomeTree(scenario, SimulationTreeOptions.defaults(), null);
    }

    /**
     * Generates an outcome tree.
     *
     * @param scenario scenario input.
     * @param options tree options.
     * @param actionProvider optional action provider.
     * @return generated tree.
     */
    public SimulationTree generateOutcomeTree(
            SimulationScenario scenario,
            SimulationTreeOptions options,
            ActionProvider actionProvider
    ) {
        if (scenario == null) {
            throw new IllegalArgumentException("scenario cannot be null");
        }
        SimulationTreeOptions safeOptions = options == null ? SimulationTreeOptions.defaults() : options;

        SimulationState rootState = createState(scenario);
        SimulationTreeNode rootNode = new SimulationTreeNode(0, 0, SimulationAction.WAIT, rootState, null);
        ExpansionCounter counter = new ExpansionCounter();
        counter.nodeCount = 1;
        counter.maxDepthReached = 0;

        expandTree(rootNode, safeOptions.getTicks(), safeOptions, actionProvider, counter);
        return new SimulationTree(scenario, safeOptions, rootNode, counter.nodeCount, counter.maxDepthReached);
    }

    /**
     * Generates legal candidate actions for a node.
     *
     * @param state state input.
     * @param depthRemaining depth remaining.
     * @param options tree options.
     * @param actionProvider optional provider.
     * @return legal action list.
     */
    public List<SimulationAction> generateCandidateActions(
            SimulationState state,
            int depthRemaining,
            SimulationTreeOptions options,
            ActionProvider actionProvider
    ) {
        if (state == null) {
            return Collections.singletonList(SimulationAction.WAIT);
        }

        SimulationTreeOptions safeOptions = options == null ? SimulationTreeOptions.defaults() : options;
        LinkedHashSet<SimulationAction> collected = new LinkedHashSet<>();
        collected.addAll(generateMovementActions(state, depthRemaining, safeOptions));
        collected.add(SimulationAction.WAIT);

        if (actionProvider != null) {
            List<SimulationAction> extra = actionProvider.provide(state, depthRemaining);
            if (extra != null) {
                for (SimulationAction action : extra) {
                    if (action != null) {
                        collected.add(action);
                    }
                }
            }
        }

        List<SimulationAction> legal = new ArrayList<>();
        for (SimulationAction action : collected) {
            if (canApplyPlayerAction(state, action)) {
                legal.add(action);
            }
            if (legal.size() >= safeOptions.getMaxActionsPerNode()) {
                break;
            }
        }

        if (legal.isEmpty()) {
            return Collections.singletonList(SimulationAction.WAIT);
        }
        return legal;
    }

    /**
     * Checks whether an action is currently legal.
     *
     * @param state state.
     * @param action action.
     * @return true when legal.
     */
    public boolean canApplyPlayerAction(SimulationState state, SimulationAction action) {
        if (state == null || action == null) {
            return false;
        }

        switch (action.getType()) {
            case WAIT:
                return true;
            case MOVE:
                if (action.getMovementDestination() == null) {
                    return false;
                }
                int targetX = WorldPointService.getPackedX(action.getTargetPackedPoint());
                int targetY = WorldPointService.getPackedY(action.getTargetPackedPoint());
                if (!state.getSnapshot().isWorldInBounds(targetX, targetY)) {
                    return false;
                }
                return canPlayerReach(
                        state,
                        state.getPlayerX(),
                        state.getPlayerY(),
                        targetX,
                        targetY,
                        resolvePlayerReachBudget(state, targetX, targetY)
                );
            case SWITCH_PRAYER:
                return action.getPrayer() != null && action.getPrayer() != state.getActiveProtectionPrayer();
            case EQUIP_ITEM:
                return action.getItemId() != null
                        && action.getItemId() >= 0
                        && state.hasInventoryItem(action.getItemId())
                        && !state.isItemEquipped(action.getItemId());
            case NPC_INTERACT:
                return action.getTargetNpcIndex() != null
                        && action.getTargetNpcIndex() >= 0
                        && state.findNpcSlotByIndex(action.getTargetNpcIndex()) >= 0;
            case CAST_SPELL:
                return action.getSpell() != null;
            case CUSTOM:
            default:
                return true;
        }
    }

    /**
     * Counts active NPC line-of-sight threats.
     *
     * @param state state.
     * @return count.
     */
    public int countNpcsWithLineOfSightToPlayer(SimulationState state) {
        if (state == null) {
            return 0;
        }
        int count = 0;
        for (int slot = 0; slot < state.getNpcCount(); slot++) {
            if (state.isNpcActive(slot) && hasNpcLineOfSightToPlayer(state, slot)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Counts active NPCs able to attack now.
     *
     * @param state state.
     * @return count.
     */
    public int countNpcsAbleToAttackPlayer(SimulationState state) {
        if (state == null) {
            return 0;
        }
        int count = 0;
        for (int slot = 0; slot < state.getNpcCount(); slot++) {
            if (state.isNpcActive(slot) && canNpcAttackPlayerNow(state, slot)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Counts active threats not covered by active prayer.
     *
     * @param state state.
     * @return count.
     */
    public int countUnprotectedNpcThreats(SimulationState state) {
        if (state == null) {
            return 0;
        }
        int count = 0;
        for (int slot = 0; slot < state.getNpcCount(); slot++) {
            if (!state.isNpcActive(slot) || !canNpcAttackPlayerNow(state, slot)) {
                continue;
            }
            if (!isNpcAttackProtected(state, slot)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Returns the best overhead prayer based on soonest incoming hit.
     *
     * @param state state.
     * @return prayer or null.
     */
    public Prayer recommendProtectionPrayer(SimulationState state) {
        if (state == null) {
            return null;
        }
        int bestTicks = Integer.MAX_VALUE;
        int bestMaxHit = Integer.MIN_VALUE;
        Prayer recommended = null;
        for (int slot = 0; slot < state.getNpcCount(); slot++) {
            if (!state.isNpcActive(slot) || !canNpcAttackPlayerNow(state, slot)) {
                continue;
            }
            Prayer prayer = state.getNpcProfile(slot).getAttackStyle().toProtectionPrayer();
            if (prayer == null) {
                continue;
            }
            int cooldown = state.getNpcAttackCooldown(slot);
            int maxHit = state.getNpcProfile(slot).getMaxHit();
            if (cooldown < bestTicks || (cooldown == bestTicks && maxHit > bestMaxHit)) {
                bestTicks = cooldown;
                bestMaxHit = maxHit;
                recommended = prayer;
            }
        }
        return recommended;
    }

    /**
     * @param state state.
     * @return true when no npc currently has line of sight.
     */
    public boolean isPlayerTileSafe(SimulationState state) {
        return countNpcsWithLineOfSightToPlayer(state) == 0;
    }

    /**
     * Checks whether an npc has line of sight to the player.
     *
     * @param state state.
     * @param npcSlot npc slot.
     * @return true when line of sight exists.
     */
    public boolean hasNpcLineOfSightToPlayer(SimulationState state, int npcSlot) {
        if (state == null || !state.isNpcActive(npcSlot)) {
            return false;
        }
        SimulationNpcProfile profile = state.getNpcProfile(npcSlot);
        return hasLineOfSight(
                state,
                state.getNpcX(npcSlot),
                state.getNpcY(npcSlot),
                state.getNpcSize(npcSlot),
                state.getPlayerX(),
                state.getPlayerY(),
                Math.max(1, profile.getAttackRange()),
                true
        );
    }

    /**
     * Returns npc-visible tiles using configured range.
     *
     * @param state state.
     * @param npcSlot npc slot.
     * @return tiles.
     */
    public List<WorldPoint> getNpcLineOfSightTiles(SimulationState state, int npcSlot) {
        if (state == null || !state.isNpcActive(npcSlot)) {
            return Collections.emptyList();
        }
        return getNpcLineOfSightTiles(state, npcSlot, Math.max(1, state.getNpcProfile(npcSlot).getAttackRange()));
    }

    /**
     * Returns npc-visible tiles using explicit range.
     *
     * @param state state.
     * @param npcSlot npc slot.
     * @param range range.
     * @return tiles.
     */
    public List<WorldPoint> getNpcLineOfSightTiles(SimulationState state, int npcSlot, int range) {
        if (state == null || !state.isNpcActive(npcSlot) || range <= 0) {
            return Collections.emptyList();
        }
        int npcX = state.getNpcX(npcSlot);
        int npcY = state.getNpcY(npcSlot);
        int npcSize = state.getNpcSize(npcSlot);
        int npcRange = Math.max(1, range);
        SimulationSnapshot snapshot = state.getSnapshot();
        List<WorldPoint> visible = new ArrayList<>();
        int minX = npcX - npcRange;
        int minY = npcY - npcRange;
        int maxX = npcX + npcSize - 1 + npcRange;
        int maxY = npcY + npcSize - 1 + npcRange;
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                if (!snapshot.isWorldInBounds(x, y)) {
                    continue;
                }
                if (MathUtils.overlaps(npcX, npcY, npcSize, x, y, 1)) {
                    continue;
                }
                if (hasLineOfSight(state, npcX, npcY, npcSize, x, y, npcRange, true)) {
                    visible.add(new WorldPoint(x, y, snapshot.getPlane()));
                }
            }
        }
        return visible;
    }

    /**
     * Predicts npc pathing toward the player.
     *
     * @param state state.
     * @param npcSlot npc slot.
     * @param maxSteps max steps.
     * @return path.
     */
    public List<WorldPoint> predictNpcGreedyPathToPlayer(SimulationState state, int npcSlot, int maxSteps) {
        if (state == null || maxSteps <= 0 || !state.isNpcActive(npcSlot)) {
            return Collections.emptyList();
        }
        List<WorldPoint> path = new ArrayList<>();
        int curX = state.getNpcX(npcSlot);
        int curY = state.getNpcY(npcSlot);
        int size = state.getNpcSize(npcSlot);
        SimulationNpcProfile profile = state.getNpcProfile(npcSlot);
        int range = Math.max(1, profile.getAttackRange());
        for (int step = 0; step < maxSteps; step++) {
            if (hasLineOfSight(state, curX, curY, size, state.getPlayerX(), state.getPlayerY(), range, true)) {
                break;
            }
            int[] next = profile.isIntelligentPathing()
                    ? computeNpcBfsStep(state, npcSlot, curX, curY, profile, 18)
                    : computeNpcGreedyStep(state, npcSlot, curX, curY);
            if (next == null) {
                break;
            }
            curX = next[0];
            curY = next[1];
            path.add(new WorldPoint(curX, curY, state.getSnapshot().getPlane()));
        }
        return path;
    }

    /**
     * World point line-of-sight overload.
     *
     * @param state state.
     * @param source source.
     * @param sourceSize source size.
     * @param target target.
     * @param range range.
     * @param sourceIsNpc true if source is npc footprint.
     * @return true when line of sight exists.
     */
    public boolean hasLineOfSight(
            SimulationState state,
            WorldPoint source,
            int sourceSize,
            WorldPoint target,
            int range,
            boolean sourceIsNpc
    ) {
        if (state == null || source == null || target == null) {
            return false;
        }
        if (source.getPlane() != state.getSnapshot().getPlane() || target.getPlane() != state.getSnapshot().getPlane()) {
            return false;
        }
        return hasLineOfSight(state, source.getX(), source.getY(), sourceSize, target.getX(), target.getY(), range, sourceIsNpc);
    }

    private void expandTree(
            SimulationTreeNode node,
            int depthRemaining,
            SimulationTreeOptions options,
            ActionProvider actionProvider,
            ExpansionCounter counter
    ) {
        if (depthRemaining <= 0 || counter.nodeCount >= options.getMaxNodes()) {
            return;
        }
        List<SimulationAction> actions = generateCandidateActions(node.getState(), depthRemaining, options, actionProvider);
        for (SimulationAction action : actions) {
            if (counter.nodeCount >= options.getMaxNodes()) {
                break;
            }
            SimulationState childState = simulateTickCopy(node.getState(), action);
            SimulationTreeNode child = new SimulationTreeNode(counter.nodeCount, node.getDepth() + 1, action, childState, node);
            counter.nodeCount++;
            counter.maxDepthReached = Math.max(counter.maxDepthReached, child.getDepth());
            node.addChild(child);
            expandTree(child, depthRemaining - 1, options, actionProvider, counter);
        }
    }

    private List<SimulationAction> generateMovementActions(SimulationState state, int depthRemaining, SimulationTreeOptions options) {
        if (!options.isIncludeWalkActions() && !options.isIncludeRunActions()) {
            return Collections.emptyList();
        }

        int maxSteps;
        int radiusLimit;
        if (options.getMovementMode() == SimulationMovementMode.REACHABLE) {
            int stride = options.isIncludeRunActions() ? 2 : 1;
            maxSteps = Math.max(1, depthRemaining) * stride;
            radiusLimit = Integer.MAX_VALUE;
        } else {
            maxSteps = Math.max(1, options.getMovementRadius() * 2);
            radiusLimit = options.getMovementRadius();
        }

        List<Integer> packedTargets = collectReachablePlayerTargets(
                state,
                state.getPlayerX(),
                state.getPlayerY(),
                maxSteps,
                radiusLimit,
                options.getMaxMovementTargets()
        );

        List<SimulationAction> movementActions = new ArrayList<>();
        for (int packed : packedTargets) {
            if (options.isIncludeWalkActions()) {
                movementActions.add(SimulationAction.moveToPacked(packed, false));
            }
            if (options.isIncludeRunActions()) {
                movementActions.add(SimulationAction.moveToPacked(packed, true));
            }
        }
        return movementActions;
    }

    private List<Integer> collectReachablePlayerTargets(
            SimulationState state,
            int originX,
            int originY,
            int maxSteps,
            int radiusLimit,
            int targetCap
    ) {
        if (maxSteps <= 0 || targetCap <= 0) {
            return Collections.emptyList();
        }
        ArrayDeque<PathStep> queue = new ArrayDeque<>();
        Set<Integer> visited = new LinkedHashSet<>();
        List<Integer> results = new ArrayList<>();
        int plane = state.getSnapshot().getPlane();
        int originPacked = WorldPointService.pack(originX, originY, plane);
        queue.add(new PathStep(originX, originY, 0));
        visited.add(originPacked);

        while (!queue.isEmpty() && results.size() < targetCap) {
            PathStep current = queue.poll();
            if (current.distance >= maxSteps) {
                continue;
            }
            for (int[] direction :  DIRECTIONS_8) {
                int nx = current.x + direction[0];
                int ny = current.y + direction[1];
                if (!state.getSnapshot().isWorldInBounds(nx, ny)) {
                    continue;
                }
                if (radiusLimit != Integer.MAX_VALUE
                        &&  MathUtils.chebyshevDistance(originX, originY, nx, ny) > radiusLimit) {
                    continue;
                }
                if (!canEntityStep(state, current.x, current.y, 1, direction[0], direction[1], -1, true)) {
                    continue;
                }
                int packed = WorldPointService.pack(nx, ny, plane);
                if (!visited.add(packed)) {
                    continue;
                }
                queue.add(new PathStep(nx, ny, current.distance + 1));
                if (packed != originPacked) {
                    results.add(packed);
                    if (results.size() >= targetCap) {
                        break;
                    }
                }
            }
        }
        return results;
    }

    private void applyPlayerAction(SimulationState state, SimulationAction action) {
        if (action == null) {
            return;
        }
        switch (action.getType()) {
            case WAIT:
                break;
            case MOVE:
                if (action.getMovementDestination() != null) {
                    state.queueMovement(action.getTargetPackedPoint(), action.isRun());
                }
                break;
            case SWITCH_PRAYER:
                state.setActiveProtectionPrayer(action.getPrayer());
                break;
            case EQUIP_ITEM:
                if (action.getItemId() != null) {
                    state.equipItemFromInventory(action.getItemId());
                }
                break;
            case NPC_INTERACT:
            case CAST_SPELL:
            case CUSTOM:
            default:
                break;
        }
    }

    private void advanceQueuedPlayerMovement(SimulationState state) {
        if (!state.hasQueuedMovement()) {
            return;
        }
        int targetPacked = state.getQueuedMoveTargetPackedPoint();
        int targetX = WorldPointService.getPackedX(targetPacked);
        int targetY = WorldPointService.getPackedY(targetPacked);
        int playerX = state.getPlayerX();
        int playerY = state.getPlayerY();

        if (playerX == targetX && playerY == targetY) {
            state.clearQueuedMovement();
            return;
        }

        List<Integer> path = findShortestPathForEntity(
                state,
                playerX,
                playerY,
                1,
                targetX,
                targetY,
                -1,
                true,
                resolvePlayerReachBudget(state, targetX, targetY)
        );
        if (path.size() <= 1) {
            state.clearQueuedMovement();
            return;
        }

        int maxStride = state.isQueuedMovementRun() ? 2 : 1;
        int currentX = playerX;
        int currentY = playerY;
        int moved = 0;
        for (int i = 1; i < path.size() && moved < maxStride; i++) {
            int packed = path.get(i);
            int nextX = WorldPointService.getPackedX(packed);
            int nextY = WorldPointService.getPackedY(packed);
            int dx = nextX - currentX;
            int dy = nextY - currentY;
            if (!canEntityStep(state, currentX, currentY, 1, dx, dy, -1, true)) {
                break;
            }
            state.setPlayerPackedPoint(WorldPointService.pack(nextX, nextY, state.getSnapshot().getPlane()));
            currentX = nextX;
            currentY = nextY;
            moved++;
        }
        if (state.getPlayerX() == targetX && state.getPlayerY() == targetY) {
            state.clearQueuedMovement();
        }
    }

    private void moveNpcs(SimulationState state) {
        for (int slot = 0; slot < state.getNpcCount(); slot++) {
            if (!state.isNpcActive(slot)) {
                continue;
            }
            if (canNpcAttackPlayerNow(state, slot)) {
                continue;
            }

            int npcX = state.getNpcX(slot);
            int npcY = state.getNpcY(slot);
            SimulationNpcProfile profile = state.getNpcProfile(slot);
            int[] next = profile.isIntelligentPathing()
                    ? computeNpcBfsStep(state, slot, npcX, npcY, profile, 24)
                    : computeNpcGreedyStep(state, slot, npcX, npcY);
            if (next == null) {
                continue;
            }
            state.setNpcPackedPoint(slot, WorldPointService.pack(next[0], next[1], state.getSnapshot().getPlane()));
        }
    }

    private int[] computeNpcGreedyStep(SimulationState state, int npcSlot, int npcX, int npcY) {
        int dx = Integer.signum(state.getPlayerX() - npcX);
        int dy = Integer.signum(state.getPlayerY() - npcY);
        if (dx == 0 && dy == 0) {
            return null;
        }
        int size = state.getNpcSize(npcSlot);
        if (canEntityStep(state, npcX, npcY, size, dx, dy, npcSlot, false)) {
            return new int[]{npcX + dx, npcY + dy};
        }
        if (dx != 0 && canEntityStep(state, npcX, npcY, size, dx, 0, npcSlot, false)) {
            return new int[]{npcX + dx, npcY};
        }
        if (dy != 0 && canEntityStep(state, npcX, npcY, size, 0, dy, npcSlot, false)) {
            return new int[]{npcX, npcY + dy};
        }
        return null;
    }

    private int[] computeNpcBfsStep(
            SimulationState state,
            int npcSlot,
            int startX,
            int startY,
            SimulationNpcProfile profile,
            int maxSearchSteps
    ) {
        int size = state.getNpcSize(npcSlot);
        if (canNpcAttackPlayerFrom(state, startX, startY, size, profile)) {
            return null;
        }

        int plane = state.getSnapshot().getPlane();
        ArrayDeque<PathStep> queue = new ArrayDeque<>();
        queue.add(new PathStep(startX, startY, 0));
        Map<Integer, Integer> parent = new HashMap<>();
        int startPacked = WorldPointService.pack(startX, startY, plane);
        parent.put(startPacked, startPacked);
        int foundPacked = -1;

        while (!queue.isEmpty()) {
            PathStep current = queue.poll();
            if (current.distance >= maxSearchSteps) {
                continue;
            }
            for (int[] direction :  DIRECTIONS_8) {
                int nx = current.x + direction[0];
                int ny = current.y + direction[1];
                if (!state.getSnapshot().isWorldInBounds(nx, ny)) {
                    continue;
                }
                if (!canEntityStep(state, current.x, current.y, size, direction[0], direction[1], npcSlot, false)) {
                    continue;
                }
                int packed = WorldPointService.pack(nx, ny, plane);
                if (parent.containsKey(packed)) {
                    continue;
                }
                parent.put(packed, WorldPointService.pack(current.x, current.y, plane));
                if (canNpcAttackPlayerFrom(state, nx, ny, size, profile)) {
                    foundPacked = packed;
                    break;
                }
                queue.add(new PathStep(nx, ny, current.distance + 1));
            }
            if (foundPacked >= 0) {
                break;
            }
        }

        if (foundPacked < 0) {
            return computeNpcGreedyStep(state, npcSlot, startX, startY);
        }

        int cursor = foundPacked;
        int parentPacked = parent.get(cursor);
        while (parentPacked != startPacked && cursor != parentPacked) {
            cursor = parentPacked;
            parentPacked = parent.get(cursor);
        }
        return new int[]{WorldPointService.getPackedX(cursor), WorldPointService.getPackedY(cursor)};
    }

    private void resolveNpcAttacks(SimulationState state) {
        for (int slot = 0; slot < state.getNpcCount(); slot++) {
            if (!state.isNpcActive(slot)) {
                continue;
            }
            int cooldown = state.getNpcAttackCooldown(slot);
            if (cooldown > 0) {
                state.setNpcAttackCooldown(slot, cooldown - 1);
                continue;
            }
            if (!canNpcAttackPlayerNow(state, slot)) {
                continue;
            }
            int damage = Math.max(0, state.getNpcProfile(slot).getMaxHit());
            if (damage > 0 && !isNpcAttackProtected(state, slot)) {
                state.damagePlayer(damage);
            }
            state.setNpcAttackCooldown(slot, Math.max(1, state.getNpcProfile(slot).getAttackSpeed()) - 1);
        }
    }

    private boolean canNpcAttackPlayerNow(SimulationState state, int npcSlot) {
        if (state == null || !state.isNpcActive(npcSlot)) {
            return false;
        }
        return canNpcAttackPlayerFrom(
                state,
                state.getNpcX(npcSlot),
                state.getNpcY(npcSlot),
                state.getNpcSize(npcSlot),
                state.getNpcProfile(npcSlot)
        );
    }

    private boolean canNpcAttackPlayerFrom(
            SimulationState state,
            int npcX,
            int npcY,
            int npcSize,
            SimulationNpcProfile profile
    ) {
        return hasLineOfSight(
                state,
                npcX,
                npcY,
                npcSize,
                state.getPlayerX(),
                state.getPlayerY(),
                Math.max(1, profile.getAttackRange()),
                true
        );
    }

    private boolean isNpcAttackProtected(SimulationState state, int npcSlot) {
        Prayer activePrayer = state.getActiveProtectionPrayer();
        if (activePrayer == null) {
            return false;
        }
        Prayer neededPrayer = state.getNpcProfile(npcSlot).getAttackStyle().toProtectionPrayer();
        return neededPrayer != null && neededPrayer == activePrayer;
    }

    private boolean canPlayerReach(SimulationState state, int startX, int startY, int targetX, int targetY, int maxSteps) {
        List<Integer> path = findShortestPathForEntity(state, startX, startY, 1, targetX, targetY, -1, true, maxSteps);
        return path.size() > 1 || (startX == targetX && startY == targetY);
    }

    private List<Integer> findShortestPathForEntity(
            SimulationState state,
            int startX,
            int startY,
            int entitySize,
            int targetX,
            int targetY,
            int movingNpcSlot,
            boolean movingPlayer,
            int maxSteps
    ) {
        if (startX == targetX && startY == targetY) {
            return Collections.singletonList(WorldPointService.pack(startX, startY, state.getSnapshot().getPlane()));
        }
        if (maxSteps <= 0) {
            return Collections.emptyList();
        }
        int plane = state.getSnapshot().getPlane();
        ArrayDeque<PathStep> queue = new ArrayDeque<>();
        queue.add(new PathStep(startX, startY, 0));
        Map<Integer, Integer> parent = new HashMap<>();
        int startPacked = WorldPointService.pack(startX, startY, plane);
        int targetPacked = WorldPointService.pack(targetX, targetY, plane);
        parent.put(startPacked, startPacked);

        while (!queue.isEmpty()) {
            PathStep current = queue.poll();
            if (current.distance >= maxSteps) {
                continue;
            }
            for (int[] direction :  DIRECTIONS_8) {
                int nx = current.x + direction[0];
                int ny = current.y + direction[1];
                if (!state.getSnapshot().isWorldInBounds(nx, ny)) {
                    continue;
                }
                if (!canEntityStep(state, current.x, current.y, entitySize, direction[0], direction[1], movingNpcSlot, movingPlayer)) {
                    continue;
                }
                int packed = WorldPointService.pack(nx, ny, plane);
                if (parent.containsKey(packed)) {
                    continue;
                }
                parent.put(packed, WorldPointService.pack(current.x, current.y, plane));
                if (packed == targetPacked) {
                    return rebuildPath(parent, startPacked, targetPacked);
                }
                queue.add(new PathStep(nx, ny, current.distance + 1));
            }
        }
        return Collections.emptyList();
    }

    private List<Integer> rebuildPath(Map<Integer, Integer> parent, int startPacked, int targetPacked) {
        ArrayList<Integer> reversed = new ArrayList<>();
        int cursor = targetPacked;
        while (true) {
            reversed.add(cursor);
            if (cursor == startPacked) {
                break;
            }
            Integer next = parent.get(cursor);
            if (next == null || next == cursor) {
                break;
            }
            cursor = next;
        }
        Collections.reverse(reversed);
        return reversed;
    }

    private int resolvePlayerReachBudget(SimulationState state, int targetX, int targetY) {
        int distance =  MathUtils.chebyshevDistance(state.getPlayerX(), state.getPlayerY(), targetX, targetY);
        return Math.max(4, distance + 24);
    }

    private boolean canEntityStep(
            SimulationState state,
            int currentX,
            int currentY,
            int entitySize,
            int dx,
            int dy,
            int movingNpcSlot,
            boolean movingPlayer
    ) {
        if (dx == 0 && dy == 0) {
            return true;
        }
        if (!canTraverseCollision(state.getSnapshot(), currentX, currentY, entitySize, dx, dy)) {
            return false;
        }
        int nextX = currentX + dx;
        int nextY = currentY + dy;

        if (movingPlayer) {
            for (int slot = 0; slot < state.getNpcCount(); slot++) {
                if (!state.isNpcActive(slot)) {
                    continue;
                }
                if ( MathUtils.overlaps(nextX, nextY, 1, state.getNpcX(slot), state.getNpcY(slot), state.getNpcSize(slot))) {
                    return false;
                }
            }
            return true;
        }

        if ( MathUtils.overlaps(nextX, nextY, entitySize, state.getPlayerX(), state.getPlayerY(), 1)) {
            return false;
        }
        for (int slot = 0; slot < state.getNpcCount(); slot++) {
            if (slot == movingNpcSlot || !state.isNpcActive(slot)) {
                continue;
            }
            if ( MathUtils.overlaps(nextX, nextY, entitySize, state.getNpcX(slot), state.getNpcY(slot), state.getNpcSize(slot))) {
                return false;
            }
        }
        return true;
    }

    private boolean canTraverseCollision(SimulationSnapshot snapshot, int currentX, int currentY, int size, int dx, int dy) {
        int[][] flags = snapshot.collisionFlagsUnsafe();
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                int tileX = currentX + x;
                int tileY = currentY + y;
                int sceneX = tileX - snapshot.getBaseX();
                int sceneY = tileY - snapshot.getBaseY();
                if (!isWalkable(flags, sceneX, sceneY, dx, dy)) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isWalkable(int[][] flags, int sceneX, int sceneY, int dx, int dy) {
        if (!isInScene(flags, sceneX, sceneY)) {
            return false;
        }
        int targetX = sceneX + dx;
        int targetY = sceneY + dy;
        if (!isInScene(flags, targetX, targetY)) {
            return false;
        }
        int sourceFlags = flags[sceneX][sceneY];
        int targetFlags = flags[targetX][targetY];
        if (isBlockedTile(targetFlags)) {
            return false;
        }

        if (dx == 0 || dy == 0) {
            if (dx > 0) {
                return (sourceFlags & CollisionDataFlag.BLOCK_MOVEMENT_EAST) == 0
                        && (targetFlags & CollisionDataFlag.BLOCK_MOVEMENT_WEST) == 0;
            }
            if (dx < 0) {
                return (sourceFlags & CollisionDataFlag.BLOCK_MOVEMENT_WEST) == 0
                        && (targetFlags & CollisionDataFlag.BLOCK_MOVEMENT_EAST) == 0;
            }
            if (dy > 0) {
                return (sourceFlags & CollisionDataFlag.BLOCK_MOVEMENT_NORTH) == 0
                        && (targetFlags & CollisionDataFlag.BLOCK_MOVEMENT_SOUTH) == 0;
            }
            return (sourceFlags & CollisionDataFlag.BLOCK_MOVEMENT_SOUTH) == 0
                    && (targetFlags & CollisionDataFlag.BLOCK_MOVEMENT_NORTH) == 0;
        }

        int xNeighborFlags = flags[sceneX + dx][sceneY];
        int yNeighborFlags = flags[sceneX][sceneY + dy];
        if (isBlockedTile(xNeighborFlags) || isBlockedTile(yNeighborFlags)) {
            return false;
        }

        if (dx > 0 && dy > 0) {
            return (sourceFlags & CollisionDataFlag.BLOCK_MOVEMENT_EAST) == 0
                    && (sourceFlags & CollisionDataFlag.BLOCK_MOVEMENT_NORTH) == 0
                    && (targetFlags & CollisionDataFlag.BLOCK_MOVEMENT_WEST) == 0
                    && (targetFlags & CollisionDataFlag.BLOCK_MOVEMENT_SOUTH) == 0;
        }
        if (dx < 0 && dy > 0) {
            return (sourceFlags & CollisionDataFlag.BLOCK_MOVEMENT_WEST) == 0
                    && (sourceFlags & CollisionDataFlag.BLOCK_MOVEMENT_NORTH) == 0
                    && (targetFlags & CollisionDataFlag.BLOCK_MOVEMENT_EAST) == 0
                    && (targetFlags & CollisionDataFlag.BLOCK_MOVEMENT_SOUTH) == 0;
        }
        if (dx > 0 && dy < 0) {
            return (sourceFlags & CollisionDataFlag.BLOCK_MOVEMENT_EAST) == 0
                    && (sourceFlags & CollisionDataFlag.BLOCK_MOVEMENT_SOUTH) == 0
                    && (targetFlags & CollisionDataFlag.BLOCK_MOVEMENT_WEST) == 0
                    && (targetFlags & CollisionDataFlag.BLOCK_MOVEMENT_NORTH) == 0;
        }
        if (dx < 0 && dy < 0) {
            return (sourceFlags & CollisionDataFlag.BLOCK_MOVEMENT_WEST) == 0
                    && (sourceFlags & CollisionDataFlag.BLOCK_MOVEMENT_SOUTH) == 0
                    && (targetFlags & CollisionDataFlag.BLOCK_MOVEMENT_EAST) == 0
                    && (targetFlags & CollisionDataFlag.BLOCK_MOVEMENT_NORTH) == 0;
        }
        return false;
    }

    private boolean hasLineOfSight(
            SimulationState state,
            int sourceX,
            int sourceY,
            int sourceSize,
            int targetX,
            int targetY,
            int range,
            boolean sourceIsNpc
    ) {
        if (sourceSize <= 0 || range <= 0) {
            return false;
        }
        if ( MathUtils.overlaps(sourceX, sourceY, sourceSize, targetX, targetY, 1)) {
            return false;
        }
        if (range == 1) {
            return isMeleeReachable(sourceX, sourceY, sourceSize, targetX, targetY);
        }

        int lineSourceX = sourceX;
        int lineSourceY = sourceY;
        if (sourceIsNpc && sourceSize > 1) {
            lineSourceX =  MathUtils.clamp(targetX, sourceX, sourceX + sourceSize - 1);
            lineSourceY =  MathUtils.clamp(targetY, sourceY, sourceY + sourceSize - 1);
        }

        int dx = targetX - lineSourceX;
        int dy = targetY - lineSourceY;
        int dxAbs = Math.abs(dx);
        int dyAbs = Math.abs(dy);
        if (dxAbs > range || dyAbs > range) {
            return false;
        }

        SimulationSnapshot snapshot = state.getSnapshot();
        int sourceSceneX = lineSourceX - snapshot.getBaseX();
        int sourceSceneY = lineSourceY - snapshot.getBaseY();
        int targetSceneX = targetX - snapshot.getBaseX();
        int targetSceneY = targetY - snapshot.getBaseY();
        int[][] flags = snapshot.collisionFlagsUnsafe();
        if (!isInScene(flags, sourceSceneX, sourceSceneY) || !isInScene(flags, targetSceneX, targetSceneY)) {
            return false;
        }
        if (sourceSceneX == targetSceneX && sourceSceneY == targetSceneY) {
            return true;
        }

        int xFlags = CollisionDataFlag.BLOCK_LINE_OF_SIGHT_FULL
                | (dx < 0 ? CollisionDataFlag.BLOCK_LINE_OF_SIGHT_EAST : CollisionDataFlag.BLOCK_LINE_OF_SIGHT_WEST);
        int yFlags = CollisionDataFlag.BLOCK_LINE_OF_SIGHT_FULL
                | (dy < 0 ? CollisionDataFlag.BLOCK_LINE_OF_SIGHT_NORTH : CollisionDataFlag.BLOCK_LINE_OF_SIGHT_SOUTH);

        if (dxAbs > dyAbs) {
            int x = sourceSceneX;
            int yBig = sourceSceneY << 16;
            int slope = (dy << 16) / dxAbs;
            yBig += 0x8000;
            if (dy < 0) {
                yBig--;
            }
            int xDirection = dx < 0 ? -1 : 1;
            while (x != targetSceneX) {
                x += xDirection;
                int y = yBig >>> 16;
                if (!isInScene(flags, x, y) || (flags[x][y] & xFlags) != 0) {
                    return false;
                }
                yBig += slope;
                int nextY = yBig >>> 16;
                if (nextY != y && (!isInScene(flags, x, nextY) || (flags[x][nextY] & yFlags) != 0)) {
                    return false;
                }
            }
            return true;
        }

        int y = sourceSceneY;
        int xBig = sourceSceneX << 16;
        int slope = (dx << 16) / dyAbs;
        xBig += 0x8000;
        if (dx < 0) {
            xBig--;
        }
        int yDirection = dy < 0 ? -1 : 1;
        while (y != targetSceneY) {
            y += yDirection;
            int x = xBig >>> 16;
            if (!isInScene(flags, x, y) || (flags[x][y] & yFlags) != 0) {
                return false;
            }
            xBig += slope;
            int nextX = xBig >>> 16;
            if (nextX != x && (!isInScene(flags, nextX, y) || (flags[nextX][y] & xFlags) != 0)) {
                return false;
            }
        }
        return true;
    }

    private boolean isMeleeReachable(int sourceX, int sourceY, int sourceSize, int targetX, int targetY) {
        int dx = targetX - sourceX;
        int dy = targetY - sourceY;
        return (dx < sourceSize && dx >= 0 && (dy == sourceSize || dy == -1))
                || (dy < sourceSize && dy >= 0 && (dx == -1 || dx == sourceSize));
    }

    private boolean isLikelyConsumableAction(String action) {
        if (action == null) {
            return false;
        }
        String normalized = action.trim().toLowerCase();
        return "eat".equals(normalized) || "drink".equals(normalized) || "consume".equals(normalized);
    }

    private boolean isBlockedTile(int flags) {
        return (flags & BLOCKED_MOVEMENT_MASK) != 0;
    }

    private boolean isInScene(int[][] flags, int sceneX, int sceneY) {
        return sceneX >= 0 && sceneY >= 0 && sceneX < flags.length && sceneY < flags[sceneX].length;
    }
}
