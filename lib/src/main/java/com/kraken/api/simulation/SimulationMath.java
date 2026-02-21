package com.kraken.api.simulation;


import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Internal math helpers used by the simulation engine.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class SimulationMath {
    static final int[][] DIRECTIONS_8 = new int[][]{
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
     * Clamps a value to an inclusive range.
     */
    static int clamp(int value, int minInclusive, int maxInclusive) {
        return Math.max(minInclusive, Math.min(maxInclusive, value));
    }

    /**
     * @return Chebyshev distance between two tiles.
     */
    static int chebyshevDistance(int ax, int ay, int bx, int by) {
        return Math.max(Math.abs(ax - bx), Math.abs(ay - by));
    }

    /**
     * Returns whether two square footprints overlap.
     */
    static boolean overlaps(int aX, int aY, int aSize, int bX, int bY, int bSize) {
        int aMaxX = aX + aSize - 1;
        int aMaxY = aY + aSize - 1;
        int bMaxX = bX + bSize - 1;
        int bMaxY = bY + bSize - 1;

        return !(aMaxX < bX || bMaxX < aX || aMaxY < bY || bMaxY < aY);
    }
}
