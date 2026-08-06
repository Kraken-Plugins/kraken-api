package com.kraken.api.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.runelite.api.coords.WorldPoint;

/**
 * Geometry helpers for testing world coordinates against rectangular areas.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class WorldAreaUtils {

    /**
     * Tests whether a point lies within the axis-aligned rectangle defined by two opposite corners.
     * The corners may be given in any order; the plane component is ignored, matching the query-layer
     * area filters this backs.
     *
     * @param point   The point to test.
     * @param cornerA One corner of the area.
     * @param cornerB The opposite corner of the area.
     * @return true if the point's x and y both fall within the inclusive bounds of the two corners.
     */
    public static boolean contains(WorldPoint point, WorldPoint cornerA, WorldPoint cornerB) {
        int x1 = cornerA.getX();
        int x2 = cornerB.getX();
        int y1 = cornerA.getY();
        int y2 = cornerB.getY();

        int x = point.getX();
        int y = point.getY();

        if (x > Math.max(x1, x2) || x < Math.min(x1, x2)) {
            return false;
        }

        return y <= Math.max(y1, y2) && y >= Math.min(y1, y2);
    }
}
