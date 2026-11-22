package com.kraken.api.service.pathfinding;

/**
 * How deep in the wilderness a search has been so far, which decides the teleports it may still
 * consider.
 *
 * <p>Teleports usable from anywhere are not tried from every tile. A search steps onto a node that
 * stands for "teleport from here" once for each of these buckets, and expands the teleports that
 * work at that depth.</p>
 */
enum TeleportBucket {
    DEEPER_THAN_30,
    DEEPER_THAN_20,
    IN_WILDERNESS,
    OUTSIDE_WILDERNESS;

    /**
     * Picks the bucket for a wilderness level.
     *
     * @param wildernessLevel the wilderness bucket of the search: 0, 20, 30 or 31
     * @return the bucket
     */
    static TeleportBucket fromWildernessLevel(int wildernessLevel) {
        if (wildernessLevel > 30) {
            return DEEPER_THAN_30;
        }
        if (wildernessLevel > 20) {
            return DEEPER_THAN_20;
        }
        if (wildernessLevel > 0) {
            return IN_WILDERNESS;
        }
        return OUTSIDE_WILDERNESS;
    }

    /**
     * Returns the wilderness level a teleport has to work at to be used from this bucket.
     *
     * @return 31, 30, 20 or 0
     */
    int wildernessLevel() {
        switch (this) {
            case DEEPER_THAN_30:
                return 31;
            case DEEPER_THAN_20:
                return 30;
            case IN_WILDERNESS:
                return 20;
            default:
                return 0;
        }
    }
}
