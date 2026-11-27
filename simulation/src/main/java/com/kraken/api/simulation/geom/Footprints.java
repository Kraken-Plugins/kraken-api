package com.kraken.api.simulation.geom;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Distances between SW anchored square footprints. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Footprints {

    /**
     * Chebyshev gap between two SW anchored footprints; 0 when they touch or overlap.
     * @param ax the first footprint's anchor x
     * @param ay the first footprint's anchor y
     * @param asize the first footprint's size
     * @param bx the second footprint's anchor x
     * @param by the second footprint's anchor y
     * @param bsize the second footprint's size
     * @return the gap in tiles
     */
    public static int distance(int ax, int ay, int asize, int bx, int by, int bsize) {
        int dx = Math.max(0, Math.max(ax - (bx + bsize - 1), bx - (ax + asize - 1)));
        int dy = Math.max(0, Math.max(ay - (by + bsize - 1), by - (ay + asize - 1)));
        return Math.max(dx, dy);
    }

    /**
     * Whether a footprint covers a tile.
     * @param x the footprint's anchor x
     * @param y the footprint's anchor y
     * @param size the footprint's size
     * @param tx the tile's x
     * @param ty the tile's y
     * @return true when the tile lies inside the footprint
     */
    public static boolean covers(int x, int y, int size, int tx, int ty) {
        return tx >= x && tx <= x + size - 1 && ty >= y && ty <= y + size - 1;
    }

    /**
     * Chebyshev distance from a tile to the nearest tile of a footprint (the tile clamped onto the footprint).
     * @param x the footprint's anchor x
     * @param y the footprint's anchor y
     * @param size the footprint's size
     * @param tx the tile's x
     * @param ty the tile's y
     * @return the distance in tiles, 0 when the footprint covers the tile
     */
    public static int toTile(int x, int y, int size, int tx, int ty) {
        int cx = Math.max(x, Math.min(x + size - 1, tx));
        int cy = Math.max(y, Math.min(y + size - 1, ty));
        return Math.max(Math.abs(cx - tx), Math.abs(cy - ty));
    }
}
