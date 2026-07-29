package com.kraken.api.simulation.colosseum;

/**
 * Reusable working memory for the tick engine and planner: BFS queues, distance fields and
 * per-tick transient flags. One instance per planning thread; nothing here survives as
 * state, so the search loop stays allocation-free after warmup.
 *
 * <p>Two kinds of distance fields are cached:</p>
 * <ul>
 *   <li><b>Player path fields</b> - BFS distance from a movement destination over
 *   player-legal steps. Keyed by destination; a small LRU keeps the fields for candidate
 *   destinations shared across every rollout of one planning pass.</li>
 *   <li><b>NPC approach fields</b> - BFS distance to the nearest legal melee-attack anchor
 *   against the player, per footprint size. Used by route-finding NPCs (Fremennik warband,
 *   Red Flag minotaurs). Keyed by player position and size.</li>
 * </ul>
 */
public final class ColoScratch {
    private static final int FIELD_SIZE = ColoCoords.MAX_DIMENSION * ColoCoords.MAX_DIMENSION;
    private static final int PLAYER_FIELD_CACHE = 48;
    /** Field value for unreachable tiles. */
    public static final int UNREACHABLE = 255;

    /** Per-tick transient: which NPC slots moved during the current NPC phase. */
    final boolean[] npcMoved = new boolean[ColoConstants.MAX_NPCS];

    /**
     * Optional hook invoked whenever an NPC launches an attack during {@link ColoTick#advance};
     * used by tests and debug tooling (null and zero-cost otherwise).
     */
    public ColoTick.AttackListener attackListener;

    private final int[] bfsQueue = new int[FIELD_SIZE];

    private final short[] playerFieldKeys = new short[PLAYER_FIELD_CACHE];
    private final byte[][] playerFields = new byte[PLAYER_FIELD_CACHE][];
    private final long[] playerFieldStamp = new long[PLAYER_FIELD_CACHE];
    private long stampCounter;

    private final short[] approachKeys = new short[ColoGrid.MAX_NPC_SIZE + 1];
    private final byte[][] approachFields = new byte[ColoGrid.MAX_NPC_SIZE + 1][];

    /**
     * Creates scratch memory.
     */
    public ColoScratch() {
        java.util.Arrays.fill(playerFieldKeys, ColoCoords.NONE);
        java.util.Arrays.fill(approachKeys, ColoCoords.NONE);
    }

    /**
     * Invalidates cached fields; call when the grid changes (new capture).
     */
    public void invalidate() {
        java.util.Arrays.fill(playerFieldKeys, ColoCoords.NONE);
        java.util.Arrays.fill(approachKeys, ColoCoords.NONE);
    }

    /**
     * Returns the BFS distance field toward a player movement destination.
     *
     * @param grid collision grid.
     * @param dest packed destination tile.
     * @return distance field indexed by {@code y << 6 | x}; {@link #UNREACHABLE} where no path.
     */
    public byte[] playerField(ColoGrid grid, short dest) {
        int slot = -1;
        for (int i = 0; i < PLAYER_FIELD_CACHE; i++) {
            if (playerFieldKeys[i] == dest) {
                playerFieldStamp[i] = ++stampCounter;
                return playerFields[i];
            }
            if (slot < 0 && playerFieldKeys[i] == ColoCoords.NONE) {
                slot = i;
            }
        }
        if (slot < 0) {
            long oldest = Long.MAX_VALUE;
            for (int i = 0; i < PLAYER_FIELD_CACHE; i++) {
                if (playerFieldStamp[i] < oldest) {
                    oldest = playerFieldStamp[i];
                    slot = i;
                }
            }
        }
        if (playerFields[slot] == null) {
            playerFields[slot] = new byte[FIELD_SIZE];
        }
        byte[] field = playerFields[slot];
        computePlayerField(grid, dest, field);
        playerFieldKeys[slot] = dest;
        playerFieldStamp[slot] = ++stampCounter;
        return field;
    }

    /**
     * Returns the BFS approach field for a route-finding NPC of the given size hunting the
     * player: distance from any anchor tile to the nearest legal melee-attack anchor.
     *
     * @param grid collision grid.
     * @param playerPos packed player position.
     * @param size npc footprint size.
     * @return distance field indexed by anchor {@code y << 6 | x}.
     */
    public byte[] approachField(ColoGrid grid, short playerPos, int size) {
        if (approachKeys[size] == playerPos && approachFields[size] != null) {
            return approachFields[size];
        }
        if (approachFields[size] == null) {
            approachFields[size] = new byte[FIELD_SIZE];
        }
        byte[] field = approachFields[size];
        computeApproachField(grid, playerPos, size, field);
        approachKeys[size] = playerPos;
        return field;
    }

    /**
     * Player single-step legality: destination tile must be free; diagonal steps additionally
     * require both flanking cardinal tiles to be free (standard player movement rule).
     *
     * @param grid collision grid.
     * @param x from x.
     * @param y from y.
     * @param dx step x, -1..1.
     * @param dy step y, -1..1.
     * @return true when the player may take the step.
     */
    public static boolean playerCanStep(ColoGrid grid, int x, int y, int dx, int dy) {
        if (grid.isMoveBlocked(x + dx, y + dy)) {
            return false;
        }
        if (dx != 0 && dy != 0) {
            return !grid.isMoveBlocked(x + dx, y) && !grid.isMoveBlocked(x, y + dy);
        }
        return true;
    }

    /**
     * NPC anchor single-step legality against static collision (dynamic entity blocking is
     * checked separately at step-execution time). Size-1 NPCs may not cut corners; larger
     * NPCs may step diagonally whenever the destination footprint fits, matching the
     * validated community simulator.
     *
     * @param grid collision grid.
     * @param x anchor x.
     * @param y anchor y.
     * @param dx step x.
     * @param dy step y.
     * @param size footprint size.
     * @return true when the step is statically legal.
     */
    public static boolean npcCanStepStatic(ColoGrid grid, int x, int y, int dx, int dy, int size) {
        if (!grid.isFootprintFree(x + dx, y + dy, size)) {
            return false;
        }
        if (size == 1 && dx != 0 && dy != 0) {
            return grid.isFootprintFree(x + dx, y, 1) && grid.isFootprintFree(x, y + dy, 1);
        }
        return true;
    }

    private void computePlayerField(ColoGrid grid, short dest, byte[] field) {
        java.util.Arrays.fill(field, (byte) UNREACHABLE);
        int dx0 = ColoCoords.x(dest);
        int dy0 = ColoCoords.y(dest);
        if (grid.isMoveBlocked(dx0, dy0)) {
            return;
        }
        int head = 0;
        int tail = 0;
        field[dest & 0xFFF] = 0;
        bfsQueue[tail++] = dest & 0xFFF;
        while (head < tail) {
            int cur = bfsQueue[head++];
            int cx = cur & 0x3F;
            int cy = (cur >> 6) & 0x3F;
            int dist = field[cur] & 0xFF;
            if (dist >= UNREACHABLE - 1) {
                continue;
            }
            for (int dir = 0; dir < 8; dir++) {
                int dx = DIR_X[dir];
                int dy = DIR_Y[dir];
                int nx = cx + dx;
                int ny = cy + dy;
                if (!grid.inBounds(nx, ny)) {
                    continue;
                }
                int next = (ny << 6) | nx;
                if ((field[next] & 0xFF) != UNREACHABLE) {
                    continue;
                }
                // Neighbour expands from destination outward, so validate the reverse step
                // (from neighbour toward cur) which is what the walking player will take.
                if (!playerCanStep(grid, nx, ny, -dx, -dy)) {
                    continue;
                }
                field[next] = (byte) (dist + 1);
                bfsQueue[tail++] = next;
            }
        }
    }

    private void computeApproachField(ColoGrid grid, short playerPos, int size, byte[] field) {
        java.util.Arrays.fill(field, (byte) UNREACHABLE);
        int px = ColoCoords.x(playerPos);
        int py = ColoCoords.y(playerPos);
        int head = 0;
        int tail = 0;
        // Seed every legal anchor whose footprint is melee-adjacent to the player.
        for (int ay = py - size; ay <= py + 1; ay++) {
            for (int ax = px - size; ax <= px + 1; ax++) {
                if (ax < 0 || ay < 0) {
                    continue;
                }
                if (!ColoLos.isMeleeAdjacent(ax, ay, size, px, py)) {
                    continue;
                }
                if (!grid.isFootprintFree(ax, ay, size)) {
                    continue;
                }
                int idx = (ay << 6) | ax;
                field[idx] = 0;
                bfsQueue[tail++] = idx;
            }
        }
        while (head < tail) {
            int cur = bfsQueue[head++];
            int cx = cur & 0x3F;
            int cy = (cur >> 6) & 0x3F;
            int dist = field[cur] & 0xFF;
            if (dist >= UNREACHABLE - 1) {
                continue;
            }
            for (int dir = 0; dir < 8; dir++) {
                int dx = DIR_X[dir];
                int dy = DIR_Y[dir];
                int nx = cx + dx;
                int ny = cy + dy;
                if (nx < 0 || ny < 0 || nx >= grid.width() || ny >= grid.height()) {
                    continue;
                }
                int next = (ny << 6) | nx;
                if ((field[next] & 0xFF) != UNREACHABLE) {
                    continue;
                }
                if (!npcCanStepStatic(grid, nx, ny, -dx, -dy, size)) {
                    continue;
                }
                field[next] = (byte) (dist + 1);
                bfsQueue[tail++] = next;
            }
        }
    }

    /** Neighbour direction x-offsets shared by BFS and step iteration. */
    public static final int[] DIR_X = {-1, 1, 0, 0, -1, 1, -1, 1};
    /** Neighbour direction y-offsets shared by BFS and step iteration. */
    public static final int[] DIR_Y = {0, 0, -1, 1, -1, -1, 1, 1};
}
