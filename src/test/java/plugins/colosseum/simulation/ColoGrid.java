package plugins.colosseum.simulation;

import net.runelite.api.CollisionDataFlag;
import net.runelite.api.coords.WorldPoint;

/**
 * Immutable arena collision grid backed by one 64-bit row mask per tile row.
 *
 * <p>Two masks are kept: movement blocking and line-of-sight blocking. Inside the colosseum
 * arena every obstacle (boundary and pillars) blocks both, but the masks are captured
 * independently from the client collision map so the engine stays correct if that ever
 * differs. Movement legality for multi-tile NPCs is answered from precomputed "eroded"
 * masks: {@code blockedForSize[s]} has a bit set where a size-{@code s} footprint anchored
 * (south-west) on that tile would overlap a blocked tile or leave the grid.</p>
 *
 * <p>All queries are static-cost bit tests, safe to call millions of times per decision.</p>
 */
public final class ColoGrid {
    /** Largest NPC footprint the grid precomputes erosion for (Sol Heredit is excluded). */
    public static final int MAX_NPC_SIZE = 5;

    private static final int MOVE_BLOCK_MASK = CollisionDataFlag.BLOCK_MOVEMENT_FULL
            | CollisionDataFlag.BLOCK_MOVEMENT_OBJECT;

    private final int width;
    private final int height;
    private final int baseX;
    private final int baseY;
    private final int plane;
    private final long[] losBlocked;
    /** Index 1..MAX_NPC_SIZE; blockedForSize[1] is the plain movement mask plus bounds. */
    private final long[][] blockedForSize;

    private ColoGrid(int width, int height, int baseX, int baseY, int plane, long[] moveBlocked, long[] losBlocked) {
        if (width <= 0 || height <= 0 || width > ColoCoords.MAX_DIMENSION || height > ColoCoords.MAX_DIMENSION) {
            throw new IllegalArgumentException("Grid dimensions must be 1-" + ColoCoords.MAX_DIMENSION);
        }
        this.width = width;
        this.height = height;
        this.baseX = baseX;
        this.baseY = baseY;
        this.plane = plane;
        this.losBlocked = losBlocked;
        this.blockedForSize = erode(moveBlocked, width, height);
    }

    /**
     * Builds a grid from a RuneLite scene collision map.
     *
     * @param flags scene collision flags indexed as [sceneX][sceneY].
     * @param sceneOriginX scene x of the grid's south-west corner.
     * @param sceneOriginY scene y of the grid's south-west corner.
     * @param width grid width in tiles.
     * @param height grid height in tiles.
     * @param baseX world x of the grid's south-west corner.
     * @param baseY world y of the grid's south-west corner.
     * @param plane capture plane.
     * @return immutable grid.
     */
    public static ColoGrid fromCollisionFlags(
            int[][] flags,
            int sceneOriginX,
            int sceneOriginY,
            int width,
            int height,
            int baseX,
            int baseY,
            int plane
    ) {
        long[] move = new long[height];
        long[] los = new long[height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int sceneX = sceneOriginX + x;
                int sceneY = sceneOriginY + y;
                int flag;
                if (sceneX < 0 || sceneY < 0 || sceneX >= flags.length || sceneY >= flags[sceneX].length) {
                    flag = MOVE_BLOCK_MASK | CollisionDataFlag.BLOCK_LINE_OF_SIGHT_FULL;
                } else {
                    flag = flags[sceneX][sceneY];
                }
                if ((flag & MOVE_BLOCK_MASK) != 0) {
                    move[y] |= 1L << x;
                }
                if ((flag & CollisionDataFlag.BLOCK_LINE_OF_SIGHT_FULL) != 0) {
                    los[y] |= 1L << x;
                }
            }
        }
        return new ColoGrid(width, height, baseX, baseY, plane, move, los);
    }

    /**
     * Builds a synthetic grid from explicit blocked tiles, used by tests and offline tooling.
     * Every blocked tile blocks both movement and line of sight, matching colosseum obstacles.
     *
     * @param width grid width.
     * @param height grid height.
     * @param baseX world x of the south-west corner.
     * @param baseY world y of the south-west corner.
     * @param plane plane.
     * @param blockedTiles packed local positions of fully blocked tiles.
     * @return immutable grid.
     */
    public static ColoGrid synthetic(int width, int height, int baseX, int baseY, int plane, short[] blockedTiles) {
        long[] move = new long[height];
        long[] los = new long[height];
        if (blockedTiles != null) {
            for (short packed : blockedTiles) {
                int x = ColoCoords.x(packed);
                int y = ColoCoords.y(packed);
                if (x < width && y < height) {
                    move[y] |= 1L << x;
                    los[y] |= 1L << x;
                }
            }
        }
        return new ColoGrid(width, height, baseX, baseY, plane, move, los);
    }

    /** @return grid width in tiles. */
    public int width() {
        return width;
    }

    /** @return grid height in tiles. */
    public int height() {
        return height;
    }

    /** @return world x of the south-west corner. */
    public int baseX() {
        return baseX;
    }

    /** @return world y of the south-west corner. */
    public int baseY() {
        return baseY;
    }

    /** @return capture plane. */
    public int plane() {
        return plane;
    }

    /**
     * @param x local x.
     * @param y local y.
     * @return true when inside the grid bounds.
     */
    public boolean inBounds(int x, int y) {
        return x >= 0 && y >= 0 && x < width && y < height;
    }

    /**
     * @param x local x.
     * @param y local y.
     * @return true when the tile blocks movement (out-of-bounds counts as blocked).
     */
    public boolean isMoveBlocked(int x, int y) {
        if (!inBounds(x, y)) {
            return true;
        }
        return (blockedForSize[1][y] & (1L << x)) != 0;
    }

    /**
     * @param x local x.
     * @param y local y.
     * @return true when the tile blocks line of sight (out-of-bounds counts as blocked).
     */
    public boolean isLosBlocked(int x, int y) {
        if (!inBounds(x, y)) {
            return true;
        }
        return (losBlocked[y] & (1L << x)) != 0;
    }

    /**
     * Checks whether a size-{@code size} footprint anchored south-west on {@code (x, y)}
     * fits without touching blocked tiles or leaving the grid.
     *
     * @param x anchor local x.
     * @param y anchor local y.
     * @param size footprint size, 1-{@link #MAX_NPC_SIZE}.
     * @return true when the footprint placement is legal.
     */
    public boolean isFootprintFree(int x, int y, int size) {
        if (x < 0 || y < 0 || y >= height) {
            return false;
        }
        return (blockedForSize[size][y] & (1L << x)) == 0;
    }

    /**
     * Converts a local packed position to a world point.
     *
     * @param packed packed local position.
     * @return world point on the grid plane.
     */
    public WorldPoint toWorld(short packed) {
        return new WorldPoint(baseX + ColoCoords.x(packed), baseY + ColoCoords.y(packed), plane);
    }

    /**
     * Converts world coordinates to a packed local position.
     *
     * @param worldX world x.
     * @param worldY world y.
     * @return packed local position, or {@link ColoCoords#NONE} when outside the grid.
     */
    public short toLocal(int worldX, int worldY) {
        int x = worldX - baseX;
        int y = worldY - baseY;
        if (!inBounds(x, y)) {
            return ColoCoords.NONE;
        }
        return ColoCoords.pack(x, y);
    }

    private static long[][] erode(long[] moveBlocked, int width, int height) {
        long[][] result = new long[MAX_NPC_SIZE + 1][];
        long outOfBoundsRow = width >= 64 ? 0L : -(1L << width);
        for (int size = 1; size <= MAX_NPC_SIZE; size++) {
            long[] eroded = new long[height];
            // Bits at x where any tile in x..x+size-1 would exceed the grid width.
            long widthOverflow = width - size + 1 >= 64 ? 0L : -(1L << Math.max(0, width - size + 1));
            for (int y = 0; y < height; y++) {
                long acc = widthOverflow;
                for (int dy = 0; dy < size; dy++) {
                    long row = y + dy < height ? (moveBlocked[y + dy] | outOfBoundsRow) : -1L;
                    for (int shift = 0; shift < size; shift++) {
                        acc |= row >>> shift;
                    }
                }
                eroded[y] = acc;
            }
            result[size] = eroded;
        }
        return result;
    }
}
