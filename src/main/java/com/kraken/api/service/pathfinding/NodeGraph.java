package com.kraken.api.service.pathfinding;

import java.util.Arrays;

/**
 * The nodes one search has created, held as parallel arrays instead of one object per node.
 *
 * <p>A long search creates hundreds of thousands of nodes. Here a node is an index, and its packed
 * tile, the node it was reached from, its cost and its flags sit at that index in primitive arrays,
 * so a whole search owns a handful of arrays however far it explores.</p>
 *
 * <p>There are three kinds of node. A tile node was walked to. A transport node is a tile reached
 * by a transport, whose cost is the transport's duration and not the distance. A teleport node has
 * no tile: it stands for "teleport from here" and leads to the teleports usable from anywhere.</p>
 */
class NodeGraph {

    /** The id that stands for no node. */
    static final int NO_NODE = -1;

    private static final byte FLAG_TELEPORT_NODE = 1;
    private static final byte FLAG_DELAYED_VISIT = 1 << 1;
    private static final byte FLAG_TRANSPORT = 1 << 2;

    private static final TeleportBucket[] BUCKETS = TeleportBucket.values();

    private int[] packedPosition;
    private int[] previous;
    private int[] cost;
    private int[] differentialCost;
    private byte[] flags;
    private byte[] bucket;
    private int size;

    NodeGraph(int initialCapacity) {
        int capacity = Math.max(1, initialCapacity);
        packedPosition = new int[capacity];
        previous = new int[capacity];
        cost = new int[capacity];
        differentialCost = new int[capacity];
        flags = new byte[capacity];
        bucket = new byte[capacity];
    }

    int size() {
        return size;
    }

    /** Adds a node, growing the arrays by half when they are full. */
    private int append(int packed, int from, int nodeCost, int differential, byte flagBits, byte teleportBucket) {
        if (size == packedPosition.length) {
            int newCapacity = packedPosition.length + Math.max(1, packedPosition.length >> 1);
            packedPosition = Arrays.copyOf(packedPosition, newCapacity);
            previous = Arrays.copyOf(previous, newCapacity);
            cost = Arrays.copyOf(cost, newCapacity);
            differentialCost = Arrays.copyOf(differentialCost, newCapacity);
            flags = Arrays.copyOf(flags, newCapacity);
            bucket = Arrays.copyOf(bucket, newCapacity);
        }

        int id = size;
        packedPosition[id] = packed;
        previous[id] = from;
        cost[id] = nodeCost;
        differentialCost[id] = differential;
        flags[id] = flagBits;
        bucket[id] = teleportBucket;
        size = id + 1;
        return id;
    }

    private int costOf(int id) {
        return id == NO_NODE ? 0 : cost[id];
    }

    /**
     * Creates the node a search starts from.
     *
     * @param packed the packed tile the search starts on
     * @return the node id
     */
    int createStart(int packed) {
        return append(packed, NO_NODE, 0, 0, (byte) 0, (byte) 0);
    }

    /**
     * Creates a node for a tile that was walked to. Walking from a tile adds the distance walked.
     * Stepping off a teleport node adds nothing.
     *
     * @param packed the packed tile
     * @param from the node it was reached from
     * @return the node id
     */
    int createTile(int packed, int from) {
        int travelTime = from != NO_NODE && isTile(from) ? PackedPoint.distance(packedPosition[from], packed) : 0;
        return append(packed, from, costOf(from) + travelTime, 0, (byte) 0, (byte) 0);
    }

    /**
     * Creates a node for the tile a transport leads to.
     *
     * @param packed the packed tile the transport leads to
     * @param from the node the transport was taken from
     * @param duration how long the transport takes, in game ticks
     * @param additionalCost a cost added on top of the duration
     * @param delayedVisit whether the tile is only marked visited when this node is taken, so that
     *                     two transports to the same tile can compete on cost
     * @param differential a cost that only decides the order nodes are taken in
     * @return the node id
     */
    int createTransport(int packed, int from, int duration, int additionalCost, boolean delayedVisit,
                        int differential) {
        byte flagBits = FLAG_TRANSPORT;
        if (delayedVisit) {
            flagBits |= FLAG_DELAYED_VISIT;
        }
        return append(packed, from, costOf(from) + duration + additionalCost, differential, flagBits, (byte) 0);
    }

    /**
     * Creates a node that stands for teleporting from the node it was reached from. It has no tile
     * and costs nothing.
     *
     * @param teleportBucket how deep in the wilderness the search has been
     * @param from the node it was reached from
     * @return the node id
     */
    int createTeleportNode(TeleportBucket teleportBucket, int from) {
        return append(PackedPoint.UNDEFINED, from, costOf(from), 0, FLAG_TELEPORT_NODE, (byte) teleportBucket.ordinal());
    }

    int packedPosition(int id) {
        return packedPosition[id];
    }

    int cost(int id) {
        return cost[id];
    }

    int differentialCost(int id) {
        return differentialCost[id];
    }

    /**
     * Returns the cost that decides the order transports are taken in.
     *
     * @param id the node id
     * @return the cost of the node plus its differential cost
     */
    int compareCost(int id) {
        return cost[id] + differentialCost[id];
    }

    /**
     * Reports whether a node stands on a tile.
     *
     * @param id the node id
     * @return true for a tile or transport node, false for a teleport node
     */
    boolean isTile(int id) {
        return (flags[id] & FLAG_TELEPORT_NODE) == 0;
    }

    boolean isTransport(int id) {
        return (flags[id] & FLAG_TRANSPORT) != 0;
    }

    boolean isDelayedVisit(int id) {
        return (flags[id] & FLAG_DELAYED_VISIT) != 0;
    }

    TeleportBucket bucket(int id) {
        return BUCKETS[bucket[id]];
    }

    /**
     * Lists the tiles on the way from the start to a node, leaving out teleport nodes.
     *
     * @param id the node the route ends at
     * @return the packed tiles in walking order, empty when the id is {@link #NO_NODE}
     */
    int[] pathTo(int id) {
        int count = 0;
        for (int node = id; node != NO_NODE; node = previous[node]) {
            if (isTile(node)) {
                count++;
            }
        }

        int[] path = new int[count];
        for (int node = id; node != NO_NODE; node = previous[node]) {
            if (isTile(node)) {
                path[--count] = packedPosition[node];
            }
        }
        return path;
    }

    /**
     * Finds the tile a node stands on, or the tile the nearest earlier node stands on when the node
     * is a teleport node.
     *
     * @param id the node id
     * @return the packed tile, or {@link PackedPoint#UNDEFINED} when there is none
     */
    int closestTile(int id) {
        int node = id;
        while (node != NO_NODE && !isTile(node)) {
            node = previous[node];
        }
        return node != NO_NODE ? packedPosition[node] : PackedPoint.UNDEFINED;
    }

    /** Lets go of the arrays once the route has been read, so a finished search holds no memory. */
    void release() {
        packedPosition = new int[0];
        previous = new int[0];
        cost = new int[0];
        differentialCost = new int[0];
        flags = new byte[0];
        bucket = new byte[0];
        size = 0;
    }
}
