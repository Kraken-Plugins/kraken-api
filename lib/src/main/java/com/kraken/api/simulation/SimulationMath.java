package com.kraken.api.simulation;


import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class SimulationMath {

    static int clamp(int value, int minInclusive, int maxInclusive) {
        return Math.max(minInclusive, Math.min(maxInclusive, value));
    }

    static boolean overlaps(int aX, int aY, int aSize, int bX, int bY, int bSize) {
        int aMaxX = aX + aSize - 1;
        int aMaxY = aY + aSize - 1;
        int bMaxX = bX + bSize - 1;
        int bMaxY = bY + bSize - 1;

        return !(aMaxX < bX || bMaxX < aX || aMaxY < bY || bMaxY < aY);
    }
}
