package com.kraken.api.simulation.geom;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Region local coordinate packing. The simulation works in a 64x64 window anchored on one game region's base
 * (the frame live capture gives), (0,0) at the south-west corner, x east, y north. A position packs into 12 bits
 * as {@code x | y << 6}; -1 means "none".
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Coords {
    /** Edge length of the region local window in tiles. */
    public static final int GRID = 64;
    /** Number of tiles in the window. */
    public static final int TILES = GRID * GRID;
    /** Packed position meaning no tile. */
    public static final short NONE = -1;

    /**
     * Packs a tile.
     * @param x the tile's x, 0 to 63
     * @param y the tile's y, 0 to 63
     * @return the packed position
     */
    public static short pack(int x, int y) {
        return (short) (x | y << 6);
    }

    /**
     * The x of a packed position.
     * @param pos the packed position
     * @return its x
     */
    public static int x(int pos) {
        return pos & 0x3f;
    }

    /**
     * The y of a packed position.
     * @param pos the packed position
     * @return its y
     */
    public static int y(int pos) {
        return pos >> 6 & 0x3f;
    }
}
