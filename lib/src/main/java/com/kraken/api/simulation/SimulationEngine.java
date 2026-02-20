package com.kraken.api.simulation;

import net.runelite.api.CollisionDataFlag;
import net.runelite.api.coords.WorldPoint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Generic Old School RuneScape simulation engine designed for high-frequency decision search.
 */
public final class SimulationEngine {
    private static final int BLOCKED_MOVEMENT_MASK = CollisionDataFlag.BLOCK_MOVEMENT_FULL | CollisionDataFlag.BLOCK_MOVEMENT_OBJECT;

    /**
     * Creates a fresh mutable simulation state from an immutable snapshot.
     *
     * @param snapshot immutable simulation input.
     * @return mutable state for stepping and branching.
     */
    public SimulationState createState(SimulationSnapshot snapshot) {
        return SimulationState.fromSnapshot(snapshot);
    }

    /**
     * Copies the provided state and simulates a single tick on the copy.
     * @param state The state to copy
     * @param playerAction The player action to simulate
     * @return SimulationState the copied simulation state
     */
    public SimulationState simulateTickCopy(SimulationState state, SimulationAction playerAction) {
        if (state == null) {
            throw new IllegalArgumentException("state cannot be null");
        }
        SimulationState copy = state.copy();
        simulateTick(copy, playerAction);
        return copy;
    }

    /**
     * Simulates one game tick in-place: apply player action, then move NPCs.
     * @param state The current state of the simulation
     * @param playerAction The player action to simulate
     * @return SimulationState the simulation state after the single tick
     */
    public SimulationState simulateTick(SimulationState state, SimulationAction playerAction) {
        if (state == null) {
            throw new IllegalArgumentException("state cannot be null");
        }

        applyPlayerAction(state, playerAction == null ? SimulationAction.WAIT : playerAction);
        moveNpcs(state);
        state.incrementTick();
        return state;
    }

    /**
     * Simulates many ticks using an ordered action list. If actions run out, WAIT is used.
     * @param state The current state of the simulation
     * @param playerActions The list of player action to simulate
     * @param ticks The number of ticks to simulate
     * @return SimulationState the simulation state after the ticks have been simulated
     */
    public SimulationState simulateTicks(SimulationState state, List<SimulationAction> playerActions, int ticks) {
        if (ticks <= 0) {
            return state;
        }

        for (int i = 0; i < ticks; i++) {
            SimulationAction action = (playerActions != null && i < playerActions.size())
                    ? playerActions.get(i)
                    : SimulationAction.WAIT;
            simulateTick(state, action);
        }
        return state;
    }

    /**
     * Returns true when a single player step/run action is currently legal.
     * @param state The state of the simulation
     * @param action The action to simulate
     * @return Boolean  true when a single player step/run action is currently legal.
     */
    public boolean canApplyPlayerAction(SimulationState state, SimulationAction action) {
        if (state == null || action == null) {
            return false;
        }

        int x = state.getPlayerX();
        int y = state.getPlayerY();
        int steps = action.isRun() ? 2 : 1;
        for (int i = 0; i < steps; i++) {
            if (!canEntityStep(state, x, y, 1, action.getDx(), action.getDy(), -1, true)) {
                return false;
            }
            x += action.getDx();
            y += action.getDy();
        }
        return true;
    }

    /**
     * Returns true when the given NPC currently has line of sight to the local simulated player.
     * @param state The state of the simulation
     * @param npcSlot The npc slot to check line of sight for
     * @return Boolean, true when the given NPC currently has line of sight to the local simulated player.
     */
    public boolean hasNpcLineOfSightToPlayer(SimulationState state, int npcSlot) {
        if (state == null || !state.isNpcActive(npcSlot)) {
            return false;
        }
        return hasLineOfSight(
                state,
                state.getNpcX(npcSlot),
                state.getNpcY(npcSlot),
                state.getNpcSize(npcSlot),
                state.getPlayerX(),
                state.getPlayerY(),
                Math.max(1, state.getNpcAttackRange(npcSlot)),
                true
        );
    }

    /**
     * Counts currently active NPCs that have line of sight to the player.
     *
     * @param state simulation state.
     * @return number of threatening NPCs with current LoS.
     */
    public int countNpcsWithLineOfSightToPlayer(SimulationState state) {
        if (state == null) {
            return 0;
        }

        int count = 0;
        for (int i = 0; i < state.getNpcCount(); i++) {
            if (state.isNpcActive(i) && hasNpcLineOfSightToPlayer(state, i)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Returns the line-of-sight tile set for the specified NPC using its configured attack range.
     * @param state The state of the simulation
     * @param npcSlot The npc slot to check line of sight for
     * @return List, the tiles that the NPC has line of sight to
     */
    public List<WorldPoint> getNpcLineOfSightTiles(SimulationState state, int npcSlot) {
        if (state == null || !state.isNpcActive(npcSlot)) {
            return Collections.emptyList();
        }
        return getNpcLineOfSightTiles(state, npcSlot, Math.max(1, state.getNpcAttackRange(npcSlot)));
    }

    /**
     * Returns the line-of-sight tile set for the specified NPC using an explicit range override.
     * @param state The state of the simulation
     * @param npcSlot The npc slot to check line of sight for
     * @param range The range of the NPC (how far it can see)
     * @return List The tiles that the NPC has line of sight to
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
        List<WorldPoint> visibleTiles = new ArrayList<>();
        int minX = npcX - npcRange;
        int minY = npcY - npcRange;
        int maxX = npcX + npcSize - 1 + npcRange;
        int maxY = npcY + npcSize - 1 + npcRange;

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                if (!snapshot.isWorldInBounds(x, y)) {
                    continue;
                }
                if (SimulationMath.overlaps(npcX, npcY, npcSize, x, y, 1)) {
                    continue;
                }
                if (hasLineOfSight(state, npcX, npcY, npcSize, x, y, npcRange, true)) {
                    visibleTiles.add(new WorldPoint(x, y, snapshot.getPlane()));
                }
            }
        }

        return visibleTiles;
    }

    /**
     * @param state simulation state.
     * @return true when no active NPC has current line of sight to the player.
     */
    public boolean isPlayerTileSafe(SimulationState state) {
        return countNpcsWithLineOfSightToPlayer(state) == 0;
    }

    /**
     * World-point overload for line-of-sight checks.
     *
     * @param state simulation state.
     * @param source source tile.
     * @param sourceSize source footprint size.
     * @param target target tile.
     * @param range max allowed LoS range.
     * @param sourceIsNpc true when source is an NPC footprint anchor.
     * @return true when LoS exists under range/footprint constraints.
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

    /**
     * Predicts the NPC's greedy movement path toward the simulated player without mutating the state.
     * @param state The current state of the simulation
     * @param npcSlot The NPC to predict pathing for
     * @param maxSteps The maximum number of pathing steps to predict
     * @return List of points for the NPC's greedy path toward the simulated player
     */
    public List<WorldPoint> predictNpcGreedyPathToPlayer(SimulationState state, int npcSlot, int maxSteps) {
        if (state == null || maxSteps <= 0 || !state.isNpcActive(npcSlot)) {
            return Collections.emptyList();
        }

        List<WorldPoint> path = new ArrayList<>();
        int curX = state.getNpcX(npcSlot);
        int curY = state.getNpcY(npcSlot);
        int size = state.getNpcSize(npcSlot);
        int range = Math.max(1, state.getNpcAttackRange(npcSlot));

        for (int step = 0; step < maxSteps; step++) {
            if (state.isNpcStopWhenLineOfSight(npcSlot)
                    && hasLineOfSight(state, curX, curY, size, state.getPlayerX(), state.getPlayerY(), range, true)) {
                break;
            }

            int dx = Integer.signum(state.getPlayerX() - curX);
            int dy = Integer.signum(state.getPlayerY() - curY);
            if (dx == 0 && dy == 0) {
                break;
            }

            int nextDx;
            int nextDy;
            if (canEntityStep(state, curX, curY, size, dx, dy, npcSlot, false)) {
                nextDx = dx;
                nextDy = dy;
            } else if (dx != 0 && canEntityStep(state, curX, curY, size, dx, 0, npcSlot, false)) {
                nextDx = dx;
                nextDy = 0;
            } else if (dy != 0 && canEntityStep(state, curX, curY, size, 0, dy, npcSlot, false)) {
                nextDx = 0;
                nextDy = dy;
            } else {
                break;
            }

            curX += nextDx;
            curY += nextDy;
            path.add(new WorldPoint(curX, curY, state.getSnapshot().getPlane()));
        }

        return path;
    }

    private void applyPlayerAction(SimulationState state, SimulationAction action) {
        int steps = action.isRun() ? 2 : 1;
        for (int i = 0; i < steps; i++) {
            int dx = action.getDx();
            int dy = action.getDy();
            if (dx == 0 && dy == 0) {
                return;
            }

            int currentX = state.getPlayerX();
            int currentY = state.getPlayerY();
            if (!canEntityStep(state, currentX, currentY, 1, dx, dy, -1, true)) {
                return;
            }

            state.setPlayerPosition(currentX + dx, currentY + dy);
        }
    }

    private void moveNpcs(SimulationState state) {
        for (int i = 0; i < state.getNpcCount(); i++) {
            if (!state.isNpcActive(i)) {
                continue;
            }

            int x = state.getNpcX(i);
            int y = state.getNpcY(i);
            int size = state.getNpcSize(i);
            int range = Math.max(1, state.getNpcAttackRange(i));

            if (state.isNpcStopWhenLineOfSight(i)
                    && hasLineOfSight(state, x, y, size, state.getPlayerX(), state.getPlayerY(), range, true)) {
                continue;
            }

            int dx = Integer.signum(state.getPlayerX() - x);
            int dy = Integer.signum(state.getPlayerY() - y);
            if (dx == 0 && dy == 0) {
                continue;
            }

            if (tryMoveNpc(state, i, dx, dy)) {
                continue;
            }
            if (dx != 0 && tryMoveNpc(state, i, dx, 0)) {
                continue;
            }
            if (dy != 0) {
                tryMoveNpc(state, i, 0, dy);
            }
        }
    }

    private boolean tryMoveNpc(SimulationState state, int npcSlot, int dx, int dy) {
        int x = state.getNpcX(npcSlot);
        int y = state.getNpcY(npcSlot);
        int size = state.getNpcSize(npcSlot);

        if (!canEntityStep(state, x, y, size, dx, dy, npcSlot, false)) {
            return false;
        }

        state.setNpcPosition(npcSlot, x + dx, y + dy);
        return true;
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
            for (int i = 0; i < state.getNpcCount(); i++) {
                if (!state.isNpcActive(i) || !state.isNpcCollidable(i)) {
                    continue;
                }
                if (SimulationMath.overlaps(nextX, nextY, 1, state.getNpcX(i), state.getNpcY(i), state.getNpcSize(i))) {
                    return false;
                }
            }
            return true;
        }

        if (movingNpcSlot < 0 || !state.isNpcCollidable(movingNpcSlot)) {
            return true;
        }

        if (SimulationMath.overlaps(nextX, nextY, entitySize, state.getPlayerX(), state.getPlayerY(), 1)) {
            return false;
        }

        for (int i = 0; i < state.getNpcCount(); i++) {
            if (i == movingNpcSlot || !state.isNpcActive(i) || !state.isNpcCollidable(i)) {
                continue;
            }
            if (SimulationMath.overlaps(nextX, nextY, entitySize, state.getNpcX(i), state.getNpcY(i), state.getNpcSize(i))) {
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

        if (dx > 0 && dy > 0) { // NE
            return (sourceFlags & CollisionDataFlag.BLOCK_MOVEMENT_EAST) == 0
                    && (sourceFlags & CollisionDataFlag.BLOCK_MOVEMENT_NORTH) == 0
                    && (targetFlags & CollisionDataFlag.BLOCK_MOVEMENT_WEST) == 0
                    && (targetFlags & CollisionDataFlag.BLOCK_MOVEMENT_SOUTH) == 0;
        }
        if (dx < 0 && dy > 0) { // NW
            return (sourceFlags & CollisionDataFlag.BLOCK_MOVEMENT_WEST) == 0
                    && (sourceFlags & CollisionDataFlag.BLOCK_MOVEMENT_NORTH) == 0
                    && (targetFlags & CollisionDataFlag.BLOCK_MOVEMENT_EAST) == 0
                    && (targetFlags & CollisionDataFlag.BLOCK_MOVEMENT_SOUTH) == 0;
        }
        if (dx > 0 && dy < 0) { // SE
            return (sourceFlags & CollisionDataFlag.BLOCK_MOVEMENT_EAST) == 0
                    && (sourceFlags & CollisionDataFlag.BLOCK_MOVEMENT_SOUTH) == 0
                    && (targetFlags & CollisionDataFlag.BLOCK_MOVEMENT_WEST) == 0
                    && (targetFlags & CollisionDataFlag.BLOCK_MOVEMENT_NORTH) == 0;
        }
        if (dx < 0 && dy < 0) { // SW
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

        if (SimulationMath.overlaps(sourceX, sourceY, sourceSize, targetX, targetY, 1)) {
            return false;
        }

        if (range == 1) {
            return isMeleeReachable(sourceX, sourceY, sourceSize, targetX, targetY);
        }

        int lineSourceX = sourceX;
        int lineSourceY = sourceY;
        if (sourceIsNpc && sourceSize > 1) {
            lineSourceX = SimulationMath.clamp(targetX, sourceX, sourceX + sourceSize - 1);
            lineSourceY = SimulationMath.clamp(targetY, sourceY, sourceY + sourceSize - 1);
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

    private boolean isBlockedTile(int flags) {
        return (flags & BLOCKED_MOVEMENT_MASK) != 0;
    }

    private boolean isInScene(int[][] flags, int sceneX, int sceneY) {
        return sceneX >= 0
                && sceneY >= 0
                && sceneX < flags.length
                && sceneY < flags[sceneX].length;
    }
}
