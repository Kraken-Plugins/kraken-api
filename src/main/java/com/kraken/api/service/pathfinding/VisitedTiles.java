package com.kraken.api.service.pathfinding;

import static net.runelite.api.Constants.REGION_SIZE;

/**
 * The tiles one search has already reached, as one bit per tile.
 *
 * <p>Bits are kept per map region and a region's bits are only allocated once the search enters it.
 * A tile the collision map has no data for counts as visited, which keeps a search from walking off
 * the map.</p>
 */
final class VisitedTiles {

    private final WorldCollisionData collisionData;
    private final Region[] regions;
    private final boolean[] teleportNodes = new boolean[TeleportBucket.values().length];

    VisitedTiles(WorldCollisionData collisionData) {
        this.collisionData = collisionData;
        this.regions = new Region[collisionData.regionCount()];
    }

    /**
     * Reports whether a tile has been reached.
     *
     * @param packed the packed tile
     * @return true when the tile was reached before, or lies outside the map
     */
    boolean get(int packed) {
        int x = PackedPoint.x(packed);
        int y = PackedPoint.y(packed);
        int regionIndex = collisionData.regionIndex(x / REGION_SIZE, y / REGION_SIZE);
        if (regionIndex < 0 || regionIndex >= regions.length) {
            return true;
        }

        Region region = regions[regionIndex];
        return region != null && region.get(x % REGION_SIZE, y % REGION_SIZE, PackedPoint.plane(packed));
    }

    /**
     * Marks a tile as reached.
     *
     * @param packed the packed tile
     * @return true when the tile had not been reached before
     */
    boolean set(int packed) {
        int x = PackedPoint.x(packed);
        int y = PackedPoint.y(packed);
        int regionIndex = collisionData.regionIndex(x / REGION_SIZE, y / REGION_SIZE);
        if (regionIndex < 0 || regionIndex >= regions.length) {
            return false;
        }

        Region region = regions[regionIndex];
        if (region == null) {
            region = new Region(collisionData.planeCount(regionIndex));
            regions[regionIndex] = region;
        }
        return region.set(x % REGION_SIZE, y % REGION_SIZE, PackedPoint.plane(packed));
    }

    /**
     * Reports whether a node of a graph has been reached, whether it stands on a tile or for a
     * teleport bucket.
     *
     * @param id the node id
     * @param graph the graph the node belongs to
     * @return true when it was reached before
     */
    boolean get(int id, NodeGraph graph) {
        return graph.isTile(id) ? get(graph.packedPosition(id)) : getTeleportNode(graph.bucket(id));
    }

    /**
     * Marks a node of a graph as reached.
     *
     * @param id the node id
     * @param graph the graph the node belongs to
     * @return true when it had not been reached before
     */
    boolean set(int id, NodeGraph graph) {
        if (graph.isTile(id)) {
            return set(graph.packedPosition(id));
        }

        int index = graph.bucket(id).ordinal();
        boolean unique = !teleportNodes[index];
        teleportNodes[index] = true;
        return unique;
    }

    /**
     * Reports whether the teleports of a wilderness bucket have been expanded.
     *
     * @param bucket the bucket
     * @return true when they have
     */
    boolean getTeleportNode(TeleportBucket bucket) {
        return teleportNodes[bucket.ordinal()];
    }

    /** Forgets every tile, so the arrays can be collected once the search is over. */
    void clear() {
        java.util.Arrays.fill(regions, null);
        java.util.Arrays.fill(teleportNodes, false);
    }

    /** One map region's bits: a {@code long} per row of 64 tiles, for each plane the region has. */
    private static final class Region {
        private final long[] rows;
        private final byte planeCount;

        Region(byte planeCount) {
            this.planeCount = planeCount;
            this.rows = new long[planeCount * REGION_SIZE];
        }

        boolean set(int x, int y, int plane) {
            if (plane >= planeCount) {
                return false;
            }
            int index = y + plane * REGION_SIZE;
            boolean unique = (rows[index] & (1L << x)) == 0;
            rows[index] |= 1L << x;
            return unique;
        }

        boolean get(int x, int y, int plane) {
            // A tile is checked here before the collision map is, so a plane the region has no
            // data for has to read as visited.
            if (plane >= planeCount) {
                return true;
            }
            return (rows[y + plane * REGION_SIZE] & (1L << x)) != 0;
        }
    }
}
