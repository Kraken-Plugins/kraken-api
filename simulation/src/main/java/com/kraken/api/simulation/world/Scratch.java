package com.kraken.api.simulation.world;

import com.kraken.api.simulation.geom.Coords;
import com.kraken.api.simulation.geom.Grid;

import java.util.Arrays;

/**
 * Per search thread working memory: BFS queue with epoch stamped visited marks (no clearing), the player path cache,
 * the route cache and the smart path approach fields. All allocation happens at construction; the tick and rollout
 * loops allocate nothing.
 *
 * <p>Paths and fields are valid within one search pass ({@link #beginPass}); SimStates created in a pass may hold
 * path ids into this table, revalidated by key on use.
 */
public final class Scratch {
    /** Approach field value of a tile the search did not reach. */
    public static final short UNREACHED = Short.MAX_VALUE;
    /** Sized for a full planner pass: rollouts re click paths from many intermediate tiles. */
    public static final int PATH_CACHE = 1024;
    /** Longest path in tiles one cache entry holds. */
    public static final int PATH_MAX = 96;

    /**
     * Player pathfinder expansion order (west, east, south, north, SW, SE, NW, NE): the OSRS breadth first search
     * enqueues neighbours in this order, which decides which of several shortest paths the server walks
     * (https://oldschool.runescape.wiki/w/Pathfinding).
     */
    public static final int[] DIR_X = {-1, 1, 0, 0, -1, 1, -1, 1};
    /** Y step of each expansion direction, parallel to {@link #DIR_X}. */
    public static final int[] DIR_Y = {0, 0, -1, 1, -1, -1, 1, 1};

    private final int[] queue = new int[Coords.TILES];
    private final int[] visit = new int[Coords.TILES];
    private final byte[] parentDir = new byte[Coords.TILES];
    private int visitEpoch;

    private final int[] pathKeys = new int[PATH_CACHE];
    private final short[][] pathTiles = new short[PATH_CACHE][PATH_MAX];
    private final byte[] pathLens = new byte[PATH_CACHE];
    private int pathCount;

    /**
     * Rollouts replay the same few player positions over and over, so approach fields are cached per (player
     * position, size) with a small deterministic LRU rather than one slot.
     */
    private static final int APPROACH_ENTRIES = 8;
    private final short[][][] approach = new short[Grid.MAX_NPC_SIZE + 1][][];
    private final int[][] approachKey = new int[Grid.MAX_NPC_SIZE + 1][];
    private final int[][] approachStamp = new int[Grid.MAX_NPC_SIZE + 1][];
    private final long[][] approachUse = new long[Grid.MAX_NPC_SIZE + 1][];
    private long approachUseCounter;
    private int passEpoch;

    /** Route search output buffer shared with the engine's short searches (no allocation). */
    public final short[] routeOut = new short[4];

    /**
     * Route prefixes found this pass, keyed by (source, destination): movers re run the same short searches in every
     * rollout of a pass, and the grid is fixed within one. Open addressing over a power of two table, stamped with
     * the pass so nothing is cleared.
     */
    private static final int ROUTE_CACHE = 8192;
    private static final int ROUTE_PROBES = 8;
    private final int[] routeKeys = new int[ROUTE_CACHE];
    private final int[] routeStamps = new int[ROUTE_CACHE];
    private final long[] routeValues = new long[ROUTE_CACHE];

    /** Allocates the path, route and approach caches up front; nothing allocates afterwards. */
    public Scratch() {
        for (int s = 1; s <= Grid.MAX_NPC_SIZE; s++) {
            approach[s] = new short[APPROACH_ENTRIES][Coords.TILES];
            approachKey[s] = new int[APPROACH_ENTRIES];
            approachStamp[s] = new int[APPROACH_ENTRIES];
            approachUse[s] = new long[APPROACH_ENTRIES];
        }
    }

    /**
     * The cached packed route prefix for a key.
     * @param key the route key
     * @return the packed value, or -1 when absent this pass
     */
    public long routeCacheGet(int key) {
        int h = (key * 0x9E3779B1) >>> 19;
        for (int p = 0; p < ROUTE_PROBES; p++) {
            int i = (h + p) & (ROUTE_CACHE - 1);
            if (routeStamps[i] != passEpoch) {
                return -1;
            }
            if (routeKeys[i] == key) {
                return routeValues[i];
            }
        }
        return -1;
    }

    /**
     * Caches a packed route prefix for this pass.
     * @param key the route key
     * @param value the packed value
     */
    public void routeCachePut(int key, long value) {
        int h = (key * 0x9E3779B1) >>> 19;
        int victim = h & (ROUTE_CACHE - 1);
        for (int p = 0; p < ROUTE_PROBES; p++) {
            int i = (h + p) & (ROUTE_CACHE - 1);
            if (routeStamps[i] != passEpoch || routeKeys[i] == key) {
                victim = i;
                break;
            }
        }
        routeKeys[victim] = key;
        routeStamps[victim] = passEpoch;
        routeValues[victim] = value;
    }

    /**
     * The shared breadth first search queue.
     * @return the queue
     */
    public int[] bfsQueue() {
        return queue;
    }

    /**
     * The shared visited stamps.
     * @return the stamps
     */
    public int[] bfsVisit() {
        return visit;
    }

    /**
     * The shared parent directions.
     * @return the directions
     */
    public byte[] bfsParent() {
        return parentDir;
    }

    /**
     * A fresh visited stamp for one breadth first search over {@link #bfsVisit}.
     * @return the stamp
     */
    public int nextVisitEpoch() {
        return ++visitEpoch;
    }

    /** Starts a new search pass: path table, route cache and approach fields reset. */
    public void beginPass() {
        passEpoch++;
        pathCount = 0;
    }

    /**
     * Length of a cached path.
     * @param pathId the path id
     * @return the number of tiles after the source, 0 when unreachable
     */
    public int pathLen(int pathId) {
        return pathLens[pathId];
    }

    /**
     * One tile of a cached path.
     * @param pathId the path id
     * @param i the index along the path
     * @return the packed tile
     */
    public short pathTile(int pathId, int i) {
        return pathTiles[pathId][i];
    }

    /**
     * Returns a valid path id for the key, trusting the hint when it still matches.
     * @param g the arena
     * @param hint the id the state last used
     * @param pathKey the path's key
     * @return the path id
     */
    public int resolvePath(Grid g, int hint, int pathKey) {
        if (hint >= 0 && hint < pathCount && pathKeys[hint] == pathKey) {
            return hint;
        }
        return findOrComputePath(g, pathKey >>> 12 & 0xfff, pathKey & 0xfff);
    }

    /**
     * OSRS player pathfinding: breadth first search in the fixed expansion order, size 1 footprint, diagonal steps
     * only when both flanking cardinal tiles are walkable. The path (tiles after the source, ending on the
     * destination) is cached by (source, destination); length 0 means unreachable.
     * @param g the arena
     * @param src the packed source tile
     * @param dest the packed destination tile
     * @return the path id
     */
    public int findOrComputePath(Grid g, int src, int dest) {
        int key = src << 12 | dest;
        for (int i = 0; i < pathCount; i++) {
            if (pathKeys[i] == key) {
                return i;
            }
        }
        if (pathCount == PATH_CACHE) {
            throw new IllegalStateException("path cache exhausted in pass");
        }
        int id = pathCount++;
        pathKeys[id] = key;
        pathLens[id] = (byte) bfsPath(g, src, dest, pathTiles[id]);
        return id;
    }

    private int bfsPath(Grid g, int src, int dest, short[] out) {
        if (src == dest) {
            return 0;
        }
        int epoch = ++visitEpoch;
        int head = 0;
        int tail = 0;
        queue[tail++] = src;
        visit[src] = epoch;
        parentDir[src] = -1;
        boolean found = false;
        while (head < tail && !found) {
            int pos = queue[head++];
            int x = Coords.x(pos);
            int y = Coords.y(pos);
            for (int d = 0; d < 8 && !found; d++) {
                int nx = x + DIR_X[d];
                int ny = y + DIR_Y[d];
                if (nx < 0 || ny < 0 || nx >= Grid.SIZE || ny >= Grid.SIZE || g.blockedMove(nx, ny)) {
                    continue;
                }
                if (d >= 4 && (g.blockedMove(nx, y) || g.blockedMove(x, ny))) {
                    continue;
                }
                int np = nx | ny << 6;
                if (visit[np] == epoch) {
                    continue;
                }
                visit[np] = epoch;
                parentDir[np] = (byte) d;
                queue[tail++] = np;
                found = np == dest;
            }
        }
        if (!found) {
            return 0;
        }
        int len = 0;
        for (int pos = dest; pos != src; ) {
            int d = parentDir[pos];
            len++;
            pos = (Coords.x(pos) - DIR_X[d]) | (Coords.y(pos) - DIR_Y[d]) << 6;
        }
        if (len > PATH_MAX) {
            return 0;
        }
        int i = len;
        for (int pos = dest; pos != src; ) {
            out[--i] = (short) pos;
            int d = parentDir[pos];
            pos = (Coords.x(pos) - DIR_X[d]) | (Coords.y(pos) - DIR_Y[d]) << 6;
        }
        return len;
    }

    /**
     * Distance to melee reach field for smart pathers: flood fill outward from every statically legal anchor in
     * melee adjacency to the player, over size legal anchors (diagonals corner checked for size 1, matching NPC step
     * legality). Cached per (player position, size) within the pass and shared by every smart NPC; dynamic NPC
     * occupancy is deliberately excluded, the descent handles blockers locally.
     * @param g the arena
     * @param size the mover's footprint size
     * @param px the player's x
     * @param py the player's y
     * @return the field, {@link #UNREACHED} where no route exists
     */
    public short[] approachField(Grid g, int size, int px, int py) {
        int key = px | py << 6;
        int[] keys = approachKey[size];
        int[] stamps = approachStamp[size];
        long[] uses = approachUse[size];
        int victim = 0;
        long oldest = Long.MAX_VALUE;
        for (int i = 0; i < APPROACH_ENTRIES; i++) {
            if (stamps[i] == passEpoch && keys[i] == key) {
                uses[i] = ++approachUseCounter;
                return approach[size][i];
            }
            long age = stamps[i] == passEpoch ? uses[i] : Long.MIN_VALUE + i;
            if (age < oldest) {
                oldest = age;
                victim = i;
            }
        }
        short[] f = approach[size][victim];
        keys[victim] = key;
        stamps[victim] = passEpoch;
        uses[victim] = ++approachUseCounter;
        Arrays.fill(f, UNREACHED);
        int tail = 0;
        for (int i = 0; i < size; i++) {
            tail = seedGoal(g, f, size, px - i, py + 1, tail);
            tail = seedGoal(g, f, size, px - i, py - size, tail);
            tail = seedGoal(g, f, size, px + 1, py - i, tail);
            tail = seedGoal(g, f, size, px - size, py - i, tail);
        }
        int head = 0;
        while (head < tail) {
            int pos = queue[head++];
            int x = Coords.x(pos);
            int y = Coords.y(pos);
            int nd = f[pos] + 1;
            for (int d = 0; d < 8; d++) {
                int nx = x + DIR_X[d];
                int ny = y + DIR_Y[d];
                if (nx < 0 || ny < 0 || nx >= Grid.SIZE || ny >= Grid.SIZE || g.illegalStatic(size, nx, ny)) {
                    continue;
                }
                if (size == 1 && d >= 4 && (g.illegalStatic(1, nx, y) || g.illegalStatic(1, x, ny))) {
                    continue;
                }
                int np = nx | ny << 6;
                if (f[np] != UNREACHED) {
                    continue;
                }
                f[np] = (short) nd;
                queue[tail++] = np;
            }
        }
        return f;
    }

    private int seedGoal(Grid g, short[] f, int size, int x, int y, int tail) {
        if (x < 0 || y < 0 || x >= Grid.SIZE || y >= Grid.SIZE || g.illegalStatic(size, x, y) || f[x | y << 6] == 0) {
            return tail;
        }
        f[x | y << 6] = 0;
        queue[tail++] = x | y << 6;
        return tail;
    }
}
