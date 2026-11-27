package com.kraken.api.simulation.geom;

/**
 * Static arena geometry as bitboards: one long per row, bit x of rows[y].
 *
 * <p>{@code blockMove} holds full tile movement blockers (walls, pillars, out of the arena), {@code blockLos} holds
 * sight blockers. Inside the Colosseum the two sets are identical, but they are captured separately from the client's
 * collision flags and never assumed so.
 *
 * <p>{@code erodedIllegal[size]} is precomputed per NPC footprint size (1 to 3): the bit at (x,y) is set when a
 * size x size footprint anchored SW on that tile would overlap a blocker or leave the 64x64 window, so any step
 * legality test is a single bit test regardless of NPC size.
 */
public final class Grid {
    /** Edge length of the arena window in tiles, which is also the row count of every board. */
    public static final int SIZE = Coords.GRID;
    /** Largest NPC footprint size the eroded boards cover. */
    public static final int MAX_NPC_SIZE = 3;

    /** Tiles that block movement, bit x of row y. */
    public final long[] blockMove = new long[SIZE];
    /** Tiles that block sight, bit x of row y. */
    public final long[] blockLos = new long[SIZE];
    /** Illegal footprint anchors per NPC size, indexed by size from 1 to {@link #MAX_NPC_SIZE}. */
    public final long[][] erodedIllegal = new long[MAX_NPC_SIZE + 1][];
    /**
     * Walkable tiles diagonally against a blocker whose two flanking cardinals are open: the pillar corner (and wall
     * corner) tiles that naive chasers cannot round, used as cover candidate seeds by planners.
     */
    public final long[] cornerTiles = new long[SIZE];

    /**
     * Sets a tile's bit on a board.
     * @param rows the board
     * @param x the tile's x
     * @param y the tile's y
     */
    public static void set(long[] rows, int x, int y) {
        rows[y] |= 1L << x;
    }

    /**
     * Clears a tile's bit on a board.
     * @param rows the board
     * @param x the tile's x
     * @param y the tile's y
     */
    public static void clear(long[] rows, int x, int y) {
        rows[y] &= ~(1L << x);
    }

    /**
     * Reads a tile's bit on a board.
     * @param rows the board
     * @param x the tile's x
     * @param y the tile's y
     * @return true when the bit is set
     */
    public static boolean get(long[] rows, int x, int y) {
        return (rows[y] >>> x & 1) != 0;
    }

    /**
     * Whether a tile blocks movement.
     * @param x the tile's x
     * @param y the tile's y
     * @return true when the tile blocks movement
     */
    public boolean blockedMove(int x, int y) {
        return get(blockMove, x, y);
    }

    /**
     * Whether a tile blocks sight.
     * @param x the tile's x
     * @param y the tile's y
     * @return true when the tile blocks sight
     */
    public boolean blockedLos(int x, int y) {
        return get(blockLos, x, y);
    }

    /**
     * Whether a size x size footprint anchored SW at (x,y) is statically illegal. Requires {@link #finish()}.
     * @param size the footprint size, 1 to 3
     * @param x the anchor's x
     * @param y the anchor's y
     * @return true when the footprint overlaps a blocker or leaves the window
     */
    public boolean illegalStatic(int size, int x, int y) {
        return (erodedIllegal[size][y] >>> x & 1) != 0;
    }

    /** Precomputes the eroded footprint masks and corner tiles; call once after the block boards are populated. */
    public void finish() {
        for (int s = 1; s <= MAX_NPC_SIZE; s++) {
            erodedIllegal[s] = erode(s);
        }
        for (int y = 0; y < SIZE; y++) {
            cornerTiles[y] = 0;
            for (int x = 0; x < SIZE; x++) {
                if (blockedMove(x, y)) {
                    continue;
                }
                for (int d = 0; d < 4; d++) {
                    int dx = (d & 1) == 0 ? -1 : 1;
                    int dy = (d & 2) == 0 ? -1 : 1;
                    int cx = x + dx;
                    int cy = y + dy;
                    if (cx < 0 || cy < 0 || cx >= SIZE || cy >= SIZE) {
                        continue;
                    }
                    if (blockedMove(cx, cy) && !blockedMove(cx, y) && !blockedMove(x, cy)) {
                        cornerTiles[y] |= 1L << x;
                        break;
                    }
                }
            }
        }
    }

    private long[] erode(int size) {
        // Anchors whose footprint would cross x=63 are illegal: x + size - 1 > 63.
        long xBound = size == 1 ? 0L : ~0L << (SIZE + 1 - size);
        long[] horiz = new long[SIZE];
        for (int y = 0; y < SIZE; y++) {
            long row = blockMove[y];
            long m = row;
            for (int dx = 1; dx < size; dx++) {
                m |= row >>> dx;
            }
            horiz[y] = m | xBound;
        }
        long[] out = new long[SIZE];
        for (int y = 0; y < SIZE; y++) {
            long m = 0;
            for (int dy = 0; dy < size; dy++) {
                m |= y + dy < SIZE ? horiz[y + dy] : ~0L;
            }
            out[y] = m;
        }
        return out;
    }
}
