package com.kraken.api.service.pathfinding;

/**
 * Says where a search can go next from a node: the tiles it can walk to, the transports that leave
 * the tile, and the teleports usable from anywhere.
 *
 * <p>An instance keeps scratch buffers between calls, so each search uses its own.</p>
 */
class CollisionMap {

    /** The eight steps from a tile: west, east, south, north, then the four diagonals. */
    private static final int[] STEP_X = {-1, 1, 0, 0, -1, 1, -1, 1};
    private static final int[] STEP_Y = {0, 0, -1, 1, -1, -1, 1, 1};

    private final WorldCollisionData collisionData;
    private final IntList neighbors = new IntList(16);
    private final boolean[] traversable = new boolean[8];

    CollisionMap(WorldCollisionData collisionData) {
        this.collisionData = collisionData;
    }

    WorldCollisionData data() {
        return collisionData;
    }

    boolean n(int x, int y, int plane) {
        return collisionData.get(x, y, plane, WorldCollisionData.FLAG_NORTH);
    }

    boolean s(int x, int y, int plane) {
        return n(x, y - 1, plane);
    }

    boolean e(int x, int y, int plane) {
        return collisionData.get(x, y, plane, WorldCollisionData.FLAG_EAST);
    }

    boolean w(int x, int y, int plane) {
        return e(x - 1, y, plane);
    }

    private boolean ne(int x, int y, int plane) {
        return n(x, y, plane) && e(x, y + 1, plane) && e(x, y, plane) && n(x + 1, y, plane);
    }

    private boolean nw(int x, int y, int plane) {
        return n(x, y, plane) && w(x, y + 1, plane) && w(x, y, plane) && n(x - 1, y, plane);
    }

    private boolean se(int x, int y, int plane) {
        return s(x, y, plane) && e(x, y - 1, plane) && e(x, y, plane) && s(x + 1, y, plane);
    }

    private boolean sw(int x, int y, int plane) {
        return s(x, y, plane) && w(x, y - 1, plane) && w(x, y, plane) && s(x - 1, y, plane);
    }

    /**
     * Reports whether a tile cannot be walked onto or off in any straight direction.
     *
     * @param x the world x
     * @param y the world y
     * @param plane the plane
     * @return true when all four edges of the tile are closed
     */
    boolean isBlocked(int x, int y, int plane) {
        return !n(x, y, plane) && !s(x, y, plane) && !e(x, y, plane) && !w(x, y, plane);
    }

    /**
     * Creates the nodes a search can reach from a node and returns their ids.
     *
     * @param node the node being expanded
     * @param visited the tiles the search has reached
     * @param network the transports the search may use
     * @param wildernessLevel the deepest wilderness bucket the search has left behind
     * @param targetInWilderness whether the search is headed into the wilderness
     * @param graph the graph to create the nodes in
     * @return the new node ids, in a list that the next call overwrites
     */
    IntList getNeighbors(int node, VisitedTiles visited, TransportNetwork network, int wildernessLevel,
                         boolean targetInWilderness, NodeGraph graph) {
        neighbors.clear();
        if (graph.isTile(node)) {
            addTileNeighbors(node, visited, network, wildernessLevel, graph);
        } else {
            addTeleports(node, visited, network, targetInWilderness, graph);
        }
        return neighbors;
    }

    /**
     * Expands a tile: the transports that leave it, the teleport node for the search's wilderness
     * bucket when that has not been expanded yet, and the tiles that can be walked to.
     */
    private void addTileNeighbors(int node, VisitedTiles visited, TransportNetwork network, int wildernessLevel,
                                  NodeGraph graph) {
        int packed = graph.packedPosition(node);
        int x = PackedPoint.x(packed);
        int y = PackedPoint.y(packed);
        int plane = PackedPoint.plane(packed);

        // A tile reached by a delayed visit teleport passes its differential on to the competing
        // transports that leave it, as a real cost. Flying to a landing site and riding on from
        // there then always costs more than flying straight to the destination.
        int inheritedDifferential = graph.isTransport(node) && graph.isDelayedVisit(node)
                ? graph.differentialCost(node) : 0;
        for (TransportEdge edge : network.from(packed)) {
            if (!edge.delayedVisit && visited.get(edge.destination)) {
                continue;
            }
            int chainPenalty = edge.delayedVisit && inheritedDifferential > 0 ? inheritedDifferential : 0;
            neighbors.add(graph.createTransport(edge.destination, node, edge.duration, chainPenalty,
                    edge.delayedVisit, edge.delayedVisit ? edge.differentialCost : 0));
        }

        TeleportBucket bucket = TeleportBucket.fromWildernessLevel(wildernessLevel);
        if (!visited.getTeleportNode(bucket)) {
            neighbors.add(graph.createTeleportNode(bucket, node));
        }

        if (isBlocked(x, y, plane)) {
            // A transport can land the search on a tile the map marks as blocked. From there any
            // neighbouring tile that is not itself blocked can be stepped onto.
            boolean westBlocked = isBlocked(x - 1, y, plane);
            boolean eastBlocked = isBlocked(x + 1, y, plane);
            boolean southBlocked = isBlocked(x, y - 1, plane);
            boolean northBlocked = isBlocked(x, y + 1, plane);
            traversable[0] = !westBlocked;
            traversable[1] = !eastBlocked;
            traversable[2] = !southBlocked;
            traversable[3] = !northBlocked;
            traversable[4] = !isBlocked(x - 1, y - 1, plane) && !westBlocked && !southBlocked;
            traversable[5] = !isBlocked(x + 1, y - 1, plane) && !eastBlocked && !southBlocked;
            traversable[6] = !isBlocked(x - 1, y + 1, plane) && !westBlocked && !northBlocked;
            traversable[7] = !isBlocked(x + 1, y + 1, plane) && !eastBlocked && !northBlocked;
        } else {
            traversable[0] = w(x, y, plane);
            traversable[1] = e(x, y, plane);
            traversable[2] = s(x, y, plane);
            traversable[3] = n(x, y, plane);
            traversable[4] = sw(x, y, plane);
            traversable[5] = se(x, y, plane);
            traversable[6] = nw(x, y, plane);
            traversable[7] = ne(x, y, plane);
        }

        for (int i = 0; i < traversable.length; i++) {
            int neighborPacked = PackedPoint.pack(x + STEP_X[i], y + STEP_Y[i], plane);
            if (visited.get(neighborPacked)) {
                continue;
            }

            if (traversable[i]) {
                neighbors.add(graph.createTile(neighborPacked, node));
            } else if (i < 4 && isBlocked(x + STEP_X[i], y + STEP_Y[i], plane)) {
                // A transport can start on a blocked tile beside this one, as a fairy ring does.
                // The search steps onto it so the transport can be taken.
                for (TransportEdge edge : network.from(neighborPacked)) {
                    if (edge.isTeleport() || visited.get(edge.origin)) {
                        continue;
                    }
                    neighbors.add(graph.createTile(edge.origin, node));
                }
            }
        }
    }

    /**
     * Expands a teleport node: every teleport usable from anywhere that works at the node's
     * wilderness bucket and does not lead into a wilderness the search avoids.
     */
    private void addTeleports(int node, VisitedTiles visited, TransportNetwork network, boolean targetInWilderness,
                              NodeGraph graph) {
        int sourceTile = graph.closestTile(node);
        int wildernessLevel = graph.bucket(node).wildernessLevel();
        for (TransportEdge edge : network.teleports()) {
            if (!edge.delayedVisit && visited.get(edge.destination)) {
                continue;
            }
            if (!edge.usableAtWildernessLevel(wildernessLevel)) {
                continue;
            }
            if (network.avoidWilderness(sourceTile, edge.destination, targetInWilderness)) {
                continue;
            }
            neighbors.add(graph.createTransport(edge.destination, node, edge.duration, 0,
                    edge.delayedVisit, edge.delayedVisit ? edge.differentialCost : 0));
        }
    }
}
