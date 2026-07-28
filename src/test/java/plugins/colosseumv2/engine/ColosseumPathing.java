package plugins.colosseumv2.engine;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Colosseum-specific movement and line of sight rules, mirroring the validated behavior of
 * {@link com.kraken.api.simulation.colosim.Simulation} but operating on standard scene
 * coordinates (y increasing north, southwest footprint anchors).
 *
 * <p>Key differences from the generic {@code ActorService} helpers:
 * <ul>
 *   <li>Melee attackers can only hit from cardinal adjacency — a diagonal tile is never
 *   attackable, which is half of the "corner trap" fix.</li>
 *   <li>Movement legality accounts for other NPC footprints, so an NPC wedged behind another
 *   NPC on a pillar corner is correctly predicted as stuck (the other half of the fix).</li>
 * </ul>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ColosseumPathing {

    /**
     * Axis-aligned overlap test between two southwest-anchored square footprints.
     */
    public static boolean overlaps(int x1, int y1, int size1, int x2, int y2, int size2) {
        return !(x1 > x2 + size2 - 1
                || x1 + size1 - 1 < x2
                || y1 > y2 + size2 - 1
                || y1 + size1 - 1 < y2);
    }

    /**
     * Returns the tile within a footprint closest to a target tile.
     */
    public static int[] closestFootprintTile(int swX, int swY, int size, int targetX, int targetY) {
        int x = Math.max(swX, Math.min(swX + size - 1, targetX));
        int y = Math.max(swY, Math.min(swY + size - 1, targetY));
        return new int[]{x, y};
    }

    /**
     * Chebyshev distance from the closest footprint tile to the target tile.
     */
    public static int distanceTo(int swX, int swY, int size, int targetX, int targetY) {
        int[] closest = closestFootprintTile(swX, swY, size, targetX, targetY);
        return Math.max(Math.abs(closest[0] - targetX), Math.abs(closest[1] - targetY));
    }

    /**
     * Whether a target tile is melee reachable from a footprint: orthogonally adjacent to the
     * footprint edge. Diagonal corners are NOT melee reachable — this is the rule that makes a
     * corner-trapped NPC unable to attack.
     */
    public static boolean isMeleeAdjacent(int swX, int swY, int size, int targetX, int targetY) {
        int dx = targetX - swX;
        int dy = targetY - swY;
        return (dx >= 0 && dx < size && (dy == -1 || dy == size))
                || (dy >= 0 && dy < size && (dx == -1 || dx == size));
    }

    /**
     * Grid raycast between two tiles using the OSRS sight-line algorithm. Only full LoS
     * blockers are considered, matching the colosseum arena's collision layout.
     *
     * @return True when the sight line is unobstructed. The same tile always has line of sight.
     */
    public static boolean hasSightLine(CollisionMap map, int x1, int y1, int x2, int y2) {
        if (x1 == x2 && y1 == y2) {
            return true;
        }

        int dx = x2 - x1;
        int dy = y2 - y1;
        int dxAbs = Math.abs(dx);
        int dyAbs = Math.abs(dy);

        if (dxAbs > dyAbs) {
            int x = x1;
            int yBig = (y1 << 16) + 0x8000;
            int slope = (dy << 16) / dxAbs;
            if (dy < 0) {
                yBig--;
            }
            int direction = dx < 0 ? -1 : 1;

            while (x != x2) {
                x += direction;
                int y = yBig >>> 16;
                if (map.isLosBlocked(x, y)) {
                    return false;
                }
                yBig += slope;
                int nextY = yBig >>> 16;
                if (nextY != y && map.isLosBlocked(x, nextY)) {
                    return false;
                }
            }
        } else {
            int y = y1;
            int xBig = (x1 << 16) + 0x8000;
            int slope = (dx << 16) / dyAbs;
            if (dx < 0) {
                xBig--;
            }
            int direction = dy < 0 ? -1 : 1;

            while (y != y2) {
                y += direction;
                int x = xBig >>> 16;
                if (map.isLosBlocked(x, y)) {
                    return false;
                }
                xBig += slope;
                int nextX = xBig >>> 16;
                if (nextX != x && map.isLosBlocked(nextX, y)) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Whether an attacker footprint can currently attack a 1x1 target.
     *
     * <p>Melee attackers require cardinal adjacency. Ranged attackers require the target to be
     * within attack range of the closest footprint tile and an unobstructed sight line. A
     * target underneath the attacker's footprint can never be attacked.
     *
     * @param map       Collision map.
     * @param swX       Attacker southwest x.
     * @param swY       Attacker southwest y.
     * @param size      Attacker footprint size.
     * @param range     Attack range (1 for melee).
     * @param meleeOnly Whether the attacker is melee-only.
     * @param targetX   Target x.
     * @param targetY   Target y.
     * @return True when an attack is possible this tick.
     */
    public static boolean canAttack(
            CollisionMap map,
            int swX,
            int swY,
            int size,
            int range,
            boolean meleeOnly,
            int targetX,
            int targetY
    ) {
        if (overlaps(swX, swY, size, targetX, targetY, 1)) {
            return false;
        }

        if (meleeOnly || range <= 1) {
            return isMeleeAdjacent(swX, swY, size, targetX, targetY);
        }

        if (distanceTo(swX, swY, size, targetX, targetY) > range) {
            return false;
        }

        int[] closest = closestFootprintTile(swX, swY, size, targetX, targetY);
        // Cast from the target toward the NPC's closest tile, mirroring the colosim check.
        return hasSightLine(map, targetX, targetY, closest[0], closest[1]);
    }

    /**
     * Computes a single movement step toward a target, mirroring colosseum NPC movement:
     * diagonal step first (with both cardinal sub-steps required to be legal for 1x1 NPCs),
     * then the horizontal step, then the vertical step. When the diagonal step would land on
     * the target's tile, the vertical component is dropped first.
     *
     * @param map       Collision map.
     * @param swX       Mover southwest x.
     * @param swY       Mover southwest y.
     * @param size      Mover footprint size.
     * @param targetX   Target x (the player).
     * @param targetY   Target y (the player).
     * @param occupancy Footprints of other NPCs as {swX, swY, size} triples. The mover itself
     *                  must not be included.
     * @return The new {x, y} position, or {@code null} when the mover cannot move (stuck).
     */
    public static int[] computeStep(
            CollisionMap map,
            int swX,
            int swY,
            int size,
            int targetX,
            int targetY,
            List<int[]> occupancy
    ) {
        int stepX = Integer.signum(targetX - swX);
        int stepY = Integer.signum(targetY - swY);
        if (stepX == 0 && stepY == 0) {
            return null;
        }

        int candX = swX + stepX;
        int candY = swY + stepY;

        // Mirrors colosim: when the combined step would land on the player, drop the y component.
        if (overlaps(candX, candY, size, targetX, targetY, 1)) {
            candY = swY;
        }

        boolean diagonalLegal = isLegalPosition(map, candX, candY, size, occupancy)
                && (size > 1 || (isLegalPosition(map, candX, swY, size, occupancy)
                && isLegalPosition(map, swX, candY, size, occupancy)));

        if (diagonalLegal) {
            if (candX != swX || candY != swY) {
                return new int[]{candX, candY};
            }
            return null;
        }

        if (candX != swX && isLegalPosition(map, candX, swY, size, occupancy)) {
            return new int[]{candX, swY};
        }

        if (candY != swY && isLegalPosition(map, swX, candY, size, occupancy)) {
            return new int[]{swX, candY};
        }

        return null;
    }

    /**
     * Whether a footprint may occupy a position: all tiles walkable and no overlap with any
     * occupied footprint.
     */
    public static boolean isLegalPosition(CollisionMap map, int swX, int swY, int size, List<int[]> occupancy) {
        for (int x = swX; x < swX + size; x++) {
            for (int y = swY; y < swY + size; y++) {
                if (map.isMovementBlocked(x, y)) {
                    return false;
                }
            }
        }

        if (occupancy != null) {
            for (int[] other : occupancy) {
                if (overlaps(swX, swY, size, other[0], other[1], other[2])) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * A movement/attack forecast for one NPC produced by {@link #forwardSimulate}.
     */
    public static final class Forecast {
        /** Predicted positions after each future step (scene coords of the SW tile). */
        public final List<int[]> path = new ArrayList<>();

        /** First future tick with line of sight / melee adjacency to the player, or -1. */
        public int firstLosTick = -1;

        /** First future tick where an attack is possible (LoS + cooldown ready), or -1. */
        public int firstAttackTick = -1;

        /** True when the NPC could not move at some point before reaching the player. */
        public boolean stuck;
    }

    /**
     * Mutable mover state for the forward simulation.
     */
    public static final class Mover {
        public final int key;
        public int x;
        public int y;
        public final int size;
        public final int range;
        public final boolean meleeOnly;
        public final int readyTick;

        public Mover(int key, int x, int y, int size, int range, boolean meleeOnly, int readyTick) {
            this.key = key;
            this.x = x;
            this.y = y;
            this.size = size;
            this.range = range;
            this.meleeOnly = meleeOnly;
            this.readyTick = readyTick;
        }
    }

    /**
     * Steps all movers forward together, in order, against a stationary player — the same
     * progressive update the game server applies. An NPC holding line of sight does not move;
     * everything else advances one step per tick while respecting pillars and the current
     * positions of every other NPC. This is what makes corner traps resolve correctly: a
     * blocked NPC simply never produces an attack tick.
     *
     * @param map             Collision map.
     * @param movers          Movers ordered by NPC index (server processing order).
     * @param staticOccupancy Immobile blocking footprints (e.g. the healing totem), may be null.
     * @param playerX         Player scene x (assumed stationary).
     * @param playerY         Player scene y.
     * @param currentTick     The current game tick; forecasts are absolute ticks after it.
     * @param horizon         Number of future ticks to simulate.
     * @param stopOnLos       When true, movers stop advancing once they gain line of sight; when
     *                        false they keep pathing toward the player (debug visualization mode).
     * @return Forecast per mover key.
     */
    public static java.util.Map<Integer, Forecast> forwardSimulate(
            CollisionMap map,
            List<Mover> movers,
            List<int[]> staticOccupancy,
            int playerX,
            int playerY,
            int currentTick,
            int horizon,
            boolean stopOnLos
    ) {
        java.util.Map<Integer, Forecast> forecasts = new java.util.LinkedHashMap<>();
        for (Mover mover : movers) {
            forecasts.put(mover.key, new Forecast());
        }

        for (int offset = 1; offset <= horizon; offset++) {
            int futureTick = currentTick + offset;

            for (Mover mover : movers) {
                Forecast forecast = forecasts.get(mover.key);
                boolean inSight = canAttack(map, mover.x, mover.y, mover.size, mover.range, mover.meleeOnly, playerX, playerY);

                if (inSight) {
                    if (forecast.firstLosTick == -1) {
                        forecast.firstLosTick = futureTick;
                    }
                    if (forecast.firstAttackTick == -1 && futureTick >= mover.readyTick) {
                        forecast.firstAttackTick = futureTick;
                    }
                    if (stopOnLos) {
                        continue;
                    }
                }

                List<int[]> occupancy = new ArrayList<>();
                for (Mover other : movers) {
                    if (other != mover) {
                        occupancy.add(new int[]{other.x, other.y, other.size});
                    }
                }
                if (staticOccupancy != null) {
                    occupancy.addAll(staticOccupancy);
                }

                int[] next = computeStep(map, mover.x, mover.y, mover.size, playerX, playerY, occupancy);
                if (next == null) {
                    if (!inSight) {
                        forecast.stuck = true;
                    }
                    continue;
                }

                mover.x = next[0];
                mover.y = next[1];
                forecast.path.add(next);
                forecast.stuck = false;
            }
        }

        return forecasts;
    }

    /**
     * Computes the set of tiles from which the player would be attackable by this NPC at its
     * current position — the debug visualization of "line of sight".
     *
     * @param map      Collision map.
     * @param swX      NPC southwest x.
     * @param swY      NPC southwest y.
     * @param size     NPC footprint size.
     * @param range    Attack range.
     * @param meleeOnly Whether the NPC is melee only.
     * @param cap      Maximum radius to scan.
     * @return Scene tiles within range and sight of the NPC.
     */
    public static List<int[]> visibleTiles(
            CollisionMap map,
            int swX,
            int swY,
            int size,
            int range,
            boolean meleeOnly,
            int cap
    ) {
        List<int[]> tiles = new ArrayList<>();
        int radius = Math.min(range, cap) + size;

        for (int x = swX - radius; x <= swX + size - 1 + radius; x++) {
            for (int y = swY - radius; y <= swY + size - 1 + radius; y++) {
                if (map.isMovementBlocked(x, y) && map.isLosBlocked(x, y)) {
                    continue;
                }
                if (canAttack(map, swX, swY, size, range, meleeOnly, x, y)) {
                    tiles.add(new int[]{x, y});
                }
            }
        }

        return tiles;
    }
}
