package com.kraken.api.simulation.move;

import com.kraken.api.simulation.geom.Coords;
import com.kraken.api.simulation.geom.Grid;
import com.kraken.api.simulation.geom.Los;
import com.kraken.api.simulation.world.Scratch;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Short breadth first route searches for size 1 movers over static collision, in the OSRS expansion order (west,
 * east, south, north, then the diagonals, a diagonal only when both flanking cardinals are open). They stop as soon
 * as the goal is reached, so the usual search near the player touches a few dozen tiles, and they allocate nothing.
 * Used for movers that run to a tile through other NPCs (no occupancy test) and for the player's walk toward an
 * attack target.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Routes {
    private static final int GOAL_TILE = 0;
    private static final int GOAL_ATTACK = 1;

    /**
     * First tiles (at most 2) of the route from src to dest into {@code sc.routeOut}. Cached per pass, since every
     * rollout of a pass re runs the same few searches.
     * @param sc the thread's scratch
     * @param g the arena
     * @param src the packed start tile
     * @param dest the packed goal tile
     * @param maxSteps the number of tiles wanted, at most 2
     * @return how many tiles were written; 0 when already there or unreachable
     */
    public static int toTile(Scratch sc, Grid g, int src, int dest, int maxSteps) {
        int key = (src & 0xfff) << 12 | (dest & 0xfff);
        return cached(sc, key, maxSteps, g, src, GOAL_TILE, dest, 0, 0, 0, 0, 0);
    }

    /**
     * First tiles (at most 2) of the shortest walk from src to a tile the player can attack the footprint from
     * (range, sight, melee adjacency as {@link Los}). Cached per pass like {@link #toTile}, keyed by the start, the
     * footprint, the style and the range.
     * @param sc the thread's scratch
     * @param g the arena
     * @param src the packed start tile
     * @param nx the footprint's anchor x
     * @param ny the footprint's anchor y
     * @param size the footprint's size
     * @param style the weapon's style
     * @param range the weapon's effective range
     * @param maxSteps the number of tiles wanted, at most 2
     * @return how many tiles were written; 0 when already in reach or no such tile is reachable
     */
    public static int toAttackTile(Scratch sc, Grid g, int src, int nx, int ny, int size, int style, int range,
                                   int maxSteps) {
        // Keys of these searches carry the footprint size (1 to 3) in bits 24 to 25, so they never meet the 24 bit
        // keys of toTile in the shared cache.
        int key = (src & 0xfff) | (nx | ny << 6) << 12 | (size & 3) << 24 | (style & 3) << 26
                | Math.min(15, range) << 28;
        return cached(sc, key, maxSteps, g, src, GOAL_ATTACK, -1, nx, ny, size, style, range);
    }

    private static int cached(Scratch sc, int key, int maxSteps, Grid g, int src, int goalKind, int dest,
                              int nx, int ny, int size, int style, int range) {
        long value = sc.routeCacheGet(key);
        if (value < 0) {
            int n = search(sc, g, src, 2, goalKind, dest, nx, ny, size, style, range);
            value = n | (long) (n > 0 ? sc.routeOut[0] & 0xfff : 0) << 2
                    | (long) (n > 1 ? sc.routeOut[1] & 0xfff : 0) << 14;
            sc.routeCachePut(key, value);
        }
        sc.routeOut[0] = (short) (value >>> 2 & 0xfff);
        sc.routeOut[1] = (short) (value >>> 14 & 0xfff);
        return Math.min(maxSteps, (int) (value & 3));
    }

    private static int search(Scratch sc, Grid g, int src, int maxSteps, int goalKind, int dest,
                              int nx, int ny, int size, int style, int range) {
        if (isGoal(g, src, goalKind, dest, nx, ny, size, style, range)) {
            return 0;
        }
        int[] queue = sc.bfsQueue();
        int[] visit = sc.bfsVisit();
        byte[] parent = sc.bfsParent();
        int epoch = sc.nextVisitEpoch();
        int head = 0;
        int tail = 0;
        queue[tail++] = src;
        visit[src] = epoch;
        parent[src] = -1;
        int found = -1;
        while (head < tail && found < 0) {
            int pos = queue[head++];
            int x = Coords.x(pos);
            int y = Coords.y(pos);
            for (int d = 0; d < 8; d++) {
                int ex = x + Scratch.DIR_X[d];
                int ey = y + Scratch.DIR_Y[d];
                if (ex < 0 || ey < 0 || ex >= Grid.SIZE || ey >= Grid.SIZE || g.blockedMove(ex, ey)) {
                    continue;
                }
                if (d >= 4 && (g.blockedMove(ex, y) || g.blockedMove(x, ey))) {
                    continue;
                }
                int np = ex | ey << 6;
                if (visit[np] == epoch) {
                    continue;
                }
                visit[np] = epoch;
                parent[np] = (byte) d;
                queue[tail++] = np;
                if (isGoal(g, np, goalKind, dest, nx, ny, size, style, range)) {
                    found = np;
                    break;
                }
            }
        }
        if (found < 0) {
            return 0;
        }
        int len = 0;
        for (int pos = found; pos != src; pos = back(pos, parent[pos])) {
            len++;
        }
        int n = Math.min(maxSteps, Math.min(len, sc.routeOut.length));
        int depth = len;
        for (int pos = found; pos != src; pos = back(pos, parent[pos])) {
            if (depth <= n) {
                sc.routeOut[depth - 1] = (short) pos;
            }
            depth--;
        }
        return n;
    }

    private static int back(int pos, int dir) {
        return (Coords.x(pos) - Scratch.DIR_X[dir]) | (Coords.y(pos) - Scratch.DIR_Y[dir]) << 6;
    }

    private static boolean isGoal(Grid g, int pos, int goalKind, int dest, int nx, int ny, int size,
                                  int style, int range) {
        if (goalKind == GOAL_TILE) {
            return pos == dest;
        }
        return Los.playerCanAttack(g, Coords.x(pos), Coords.y(pos), nx, ny, size, style, range);
    }
}
