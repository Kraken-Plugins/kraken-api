package com.kraken.api.service.pathfinding;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.runelite.api.coords.WorldPoint;

/**
 * World tiles packed into one {@code int}, which is how the planner stores and compares them.
 *
 * <p>The low 15 bits hold x, the next 15 hold y and the top 2 hold the plane. A search touches
 * hundreds of thousands of tiles, and a packed tile costs no allocation.</p>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class PackedPoint {

    /** The value that stands for no tile, used as the origin of a teleport usable from anywhere. */
    static final int UNDEFINED = -1;

    private static final int COORDINATE_MASK = 0x7FFF;
    private static final int PLANE_MASK = 0x3;

    /**
     * Packs a world point.
     *
     * @param point the tile, may be null
     * @return the packed tile, or {@link #UNDEFINED} for null
     */
    static int pack(WorldPoint point) {
        return point == null ? UNDEFINED : pack(point.getX(), point.getY(), point.getPlane());
    }

    /**
     * Packs a tile given by its coordinates. Values outside the bit fields are cut down to them.
     *
     * @param x the world x
     * @param y the world y
     * @param plane the plane, 0 to 3
     * @return the packed tile
     */
    static int pack(int x, int y, int plane) {
        return (x & COORDINATE_MASK) | ((y & COORDINATE_MASK) << 15) | ((plane & PLANE_MASK) << 30);
    }

    /**
     * Unpacks a tile.
     *
     * @param packed the packed tile
     * @return the world point
     */
    static WorldPoint unpack(int packed) {
        return new WorldPoint(x(packed), y(packed), plane(packed));
    }

    static int x(int packed) {
        return packed & COORDINATE_MASK;
    }

    static int y(int packed) {
        return (packed >> 15) & COORDINATE_MASK;
    }

    static int plane(int packed) {
        return (packed >> 30) & PLANE_MASK;
    }

    /**
     * Counts the steps between two tiles when a diagonal step costs one.
     *
     * @param first a packed tile
     * @param second a packed tile
     * @return the distance, or {@link Integer#MAX_VALUE} when the tiles are on different planes
     */
    static int distance(int first, int second) {
        if (plane(first) != plane(second)) {
            return Integer.MAX_VALUE;
        }
        return Math.max(Math.abs(x(first) - x(second)), Math.abs(y(first) - y(second)));
    }

    /**
     * Counts the steps between two tiles when only straight steps are allowed.
     *
     * @param first a packed tile
     * @param second a packed tile
     * @return the distance, or {@link Integer#MAX_VALUE} when the tiles are on different planes
     */
    static int manhattanDistance(int first, int second) {
        if (plane(first) != plane(second)) {
            return Integer.MAX_VALUE;
        }
        return Math.abs(x(first) - x(second)) + Math.abs(y(first) - y(second));
    }
}
