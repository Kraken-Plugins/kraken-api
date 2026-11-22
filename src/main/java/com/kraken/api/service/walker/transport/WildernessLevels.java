package com.kraken.api.service.walker.transport;

import net.runelite.api.coords.WorldPoint;

/**
 * The wilderness bucket a tile falls into, as the planner counts it.
 *
 * <p>A search works with four buckets, 0, 20, 30 and 31, and not the per tile level the game
 * shows. A teleport's ceiling is compared with {@code <=} against that bucket. Computing anything
 * else here, such as {@code (y - 3520) / 8} from the y coordinate alone, would refuse a teleport
 * the planner had already accepted: Lunar Isle and Wintertodt sit north of y 3520 and are not
 * wilderness at all.</p>
 */
public final class WildernessLevels {

    private static final Area ABOVE_GROUND = new Area(2944, 3525, 448, 448);
    private static final Area UNDERGROUND = new Area(2944, 9918, 518, 458);

    /** Ferox Enclave and the strip south of the wilderness ditch lie inside the box and are safe. */
    private static final Area[] NOT_WILDERNESS = {
            new Area(3123, 3622, 2, 10),
            new Area(3125, 3617, 16, 23),
            new Area(3138, 3636, 18, 10),
            new Area(3141, 3625, 14, 11),
            new Area(3141, 3619, 7, 6),
            new Area(2997, 3525, 34, 9),
            new Area(3005, 3534, 21, 10),
            new Area(3000, 3534, 5, 5),
            new Area(3031, 3525, 2, 2),
    };

    private static final Area ABOVE_GROUND_LEVEL_20 = new Area(2944, 3680, 448, 448);
    private static final Area ABOVE_GROUND_LEVEL_30 = new Area(2944, 3760, 448, 448);
    private static final Area UNDERGROUND_LEVEL_20 = new Area(2944, 10075, 518, 301);
    private static final Area UNDERGROUND_LEVEL_30 = new Area(2944, 10155, 518, 221);

    private WildernessLevels() {
    }

    /**
     * Returns the planner's wilderness bucket for a world tile.
     *
     * @param location the tile to classify, may be null
     * @return 0 outside the wilderness, 20 below level 20, 30 below level 30, or 31 in deep wilderness
     */
    public static int of(WorldPoint location) {
        if (location == null) {
            return 0;
        }

        return of(location.getX(), location.getY());
    }

    /**
     * Returns the planner's wilderness bucket for a packed world tile.
     *
     * @param packed the tile with x in the low 15 bits, y in the next 15 and the plane above them
     * @return 0, 20, 30 or 31
     */
    public static int ofPacked(int packed) {
        return of(packed & 0x7FFF, (packed >> 15) & 0x7FFF);
    }

    /**
     * Reports whether a packed world tile is in the wilderness.
     *
     * @param packed the tile with x in the low 15 bits, y in the next 15 and the plane above them
     * @return true anywhere in the wilderness, above or below ground
     */
    public static boolean isInWilderness(int packed) {
        return isInWilderness(packed & 0x7FFF, (packed >> 15) & 0x7FFF);
    }

    /**
     * Reports whether a packed world tile is above level 20 wilderness, where most teleports stop
     * working.
     *
     * @param packed the tile with x in the low 15 bits, y in the next 15 and the plane above them
     * @return true above level 20
     */
    public static boolean isAboveLevel20(int packed) {
        return isAboveLevel20(packed & 0x7FFF, (packed >> 15) & 0x7FFF);
    }

    /**
     * Reports whether a packed world tile is above level 30 wilderness, where no teleport works.
     *
     * @param packed the tile with x in the low 15 bits, y in the next 15 and the plane above them
     * @return true above level 30
     */
    public static boolean isAboveLevel30(int packed) {
        return isAboveLevel30(packed & 0x7FFF, (packed >> 15) & 0x7FFF);
    }

    /**
     * Classifies a tile by its x and y. The most specific area is tried first, because the level 30
     * box sits inside the level 20 box, which sits inside the wilderness box.
     */
    private static int of(int x, int y) {
        if (isAboveLevel30(x, y)) {
            return 31;
        }
        if (isAboveLevel20(x, y)) {
            return 30;
        }
        if (isInWilderness(x, y)) {
            return 20;
        }
        return 0;
    }

    private static boolean isInWilderness(int x, int y) {
        if (UNDERGROUND.contains(x, y)) {
            return true;
        }
        if (!ABOVE_GROUND.contains(x, y)) {
            return false;
        }
        for (Area safe : NOT_WILDERNESS) {
            if (safe.contains(x, y)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isAboveLevel20(int x, int y) {
        return ABOVE_GROUND_LEVEL_20.contains(x, y) || UNDERGROUND_LEVEL_20.contains(x, y);
    }

    private static boolean isAboveLevel30(int x, int y) {
        return ABOVE_GROUND_LEVEL_30.contains(x, y) || UNDERGROUND_LEVEL_30.contains(x, y);
    }

    /** A rectangle of tiles on any plane, given by its south west corner and its size. */
    private static final class Area {
        private final int x;
        private final int y;
        private final int width;
        private final int height;

        private Area(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        private boolean contains(int tileX, int tileY) {
            return tileX >= x && tileX < x + width && tileY >= y && tileY < y + height;
        }
    }
}
