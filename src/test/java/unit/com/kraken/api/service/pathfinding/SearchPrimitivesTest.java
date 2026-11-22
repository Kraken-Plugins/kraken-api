package com.kraken.api.service.pathfinding;

import net.runelite.api.coords.WorldPoint;
import org.junit.jupiter.api.Test;

import java.util.BitSet;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the small data structures a search is built from: packed tiles, the node store, the two
 * queues, the visited set and the collision data they read.
 */
class SearchPrimitivesTest {

    @Test
    void aTilePacksIntoOneIntAndBack() {
        WorldPoint tile = new WorldPoint(3222, 3218, 2);
        int packed = PackedPoint.pack(tile);

        assertEquals(tile, PackedPoint.unpack(packed));
        assertEquals(3222, PackedPoint.x(packed));
        assertEquals(3218, PackedPoint.y(packed));
        assertEquals(2, PackedPoint.plane(packed));
        assertEquals(PackedPoint.UNDEFINED, PackedPoint.pack(null));
        assertEquals(new WorldPoint(16383, 16383, 3), PackedPoint.unpack(PackedPoint.pack(16383, 16383, 3)));
    }

    @Test
    void distanceCountsDiagonalStepsOnceAndIsEndlessAcrossPlanes() {
        int here = PackedPoint.pack(3200, 3200, 0);

        assertEquals(5, PackedPoint.distance(here, PackedPoint.pack(3205, 3203, 0)));
        assertEquals(8, PackedPoint.manhattanDistance(here, PackedPoint.pack(3205, 3203, 0)));
        assertEquals(Integer.MAX_VALUE, PackedPoint.distance(here, PackedPoint.pack(3200, 3200, 1)));
        assertEquals(Integer.MAX_VALUE, PackedPoint.manhattanDistance(here, PackedPoint.pack(3200, 3200, 1)));
    }

    @Test
    void anIntMapKeepsEveryKeyThroughGrowthAndReplacesOnPutAgain() {
        IntObjectMap<String> map = new IntObjectMap<>(2);
        for (int key = 0; key < 5_000; key++) {
            map.put(PackedPoint.pack(3000 + key % 70, 3000 + key / 70, key % 4), "tile " + key);
        }

        assertEquals(5_000, map.size());
        assertEquals("tile 0", map.getOrDefault(PackedPoint.pack(3000, 3000, 0), null));
        assertEquals("tile 4999", map.getOrDefault(PackedPoint.pack(3000 + 4999 % 70, 3000 + 4999 / 70, 3), null));
        assertNull(map.getOrDefault(PackedPoint.pack(1, 1, 0), null));
        assertEquals("none", map.getOrDefault(PackedPoint.UNDEFINED, "none"));

        map.put(PackedPoint.pack(3000, 3000, 0), "replaced");
        assertEquals(5_000, map.size());
        assertEquals("replaced", map.getOrDefault(PackedPoint.pack(3000, 3000, 0), null));
    }

    @Test
    void theQueueHandsNodesBackInTheOrderTheyWereAddedThroughGrowth() {
        IntDeque queue = new IntDeque(2);
        assertEquals(NodeGraph.NO_NODE, queue.pollFirst());

        for (int i = 0; i < 3; i++) {
            queue.addLast(i);
        }
        assertEquals(0, queue.pollFirst());
        for (int i = 3; i < 40; i++) {
            queue.addLast(i);
        }

        assertEquals(1, queue.peekFirst());
        for (int expected = 1; expected < 40; expected++) {
            assertEquals(expected, queue.pollFirst());
        }
        assertTrue(queue.isEmpty());
    }

    @Test
    void theHeapHandsBackTheCheapestTransportFirstCountingItsDifferential() {
        NodeGraph graph = new NodeGraph(1);
        int start = graph.createStart(PackedPoint.pack(3200, 3200, 0));
        int slow = graph.createTransport(PackedPoint.pack(3300, 3300, 0), start, 30, 0, false, 0);
        int quick = graph.createTransport(PackedPoint.pack(3301, 3300, 0), start, 4, 0, false, 0);
        int penalised = graph.createTransport(PackedPoint.pack(3302, 3300, 0), start, 4, 0, true, 15);
        IntMinHeap heap = new IntMinHeap(graph, 1);

        heap.add(slow);
        heap.add(penalised);
        heap.add(quick);

        assertEquals(quick, heap.peek());
        assertEquals(quick, heap.poll());
        assertEquals(penalised, heap.poll(), "a differential cost orders the heap without being a real cost");
        assertEquals(4, graph.cost(penalised));
        assertEquals(slow, heap.poll());
        assertEquals(NodeGraph.NO_NODE, heap.poll());
        assertTrue(heap.isEmpty());
    }

    @Test
    void aNodeCostsItsWalkOrItsTransportAndARouteLeavesTeleportNodesOut() {
        NodeGraph graph = new NodeGraph(1);
        int start = graph.createStart(PackedPoint.pack(3200, 3200, 0));
        int walked = graph.createTile(PackedPoint.pack(3201, 3201, 0), start);
        int teleportNode = graph.createTeleportNode(TeleportBucket.OUTSIDE_WILDERNESS, walked);
        int landed = graph.createTransport(PackedPoint.pack(3213, 3424, 0), teleportNode, 4, 0, false, 0);
        int stepped = graph.createTile(PackedPoint.pack(3213, 3425, 0), landed);

        assertEquals(1, graph.cost(walked));
        assertEquals(1, graph.cost(teleportNode), "standing on a teleport node costs nothing");
        assertEquals(5, graph.cost(landed));
        assertEquals(6, graph.cost(stepped));
        assertFalse(graph.isTile(teleportNode));
        assertTrue(graph.isTransport(landed) && graph.isTile(landed));
        assertEquals(TeleportBucket.OUTSIDE_WILDERNESS, graph.bucket(teleportNode));
        assertEquals(PackedPoint.pack(3201, 3201, 0), graph.closestTile(teleportNode));
        assertArrayEquals(new int[]{
                PackedPoint.pack(3200, 3200, 0), PackedPoint.pack(3201, 3201, 0),
                PackedPoint.pack(3213, 3424, 0), PackedPoint.pack(3213, 3425, 0)}, graph.pathTo(stepped));
        assertEquals(0, graph.pathTo(NodeGraph.NO_NODE).length);
        assertEquals(5, graph.size());
    }

    @Test
    void theTeleportBucketFollowsTheWildernessLevel() {
        assertEquals(TeleportBucket.OUTSIDE_WILDERNESS, TeleportBucket.fromWildernessLevel(0));
        assertEquals(TeleportBucket.IN_WILDERNESS, TeleportBucket.fromWildernessLevel(20));
        assertEquals(TeleportBucket.DEEPER_THAN_20, TeleportBucket.fromWildernessLevel(30));
        assertEquals(TeleportBucket.DEEPER_THAN_30, TeleportBucket.fromWildernessLevel(31));
        for (TeleportBucket bucket : TeleportBucket.values()) {
            assertEquals(bucket, TeleportBucket.fromWildernessLevel(bucket.wildernessLevel()));
        }
    }

    /** Builds collision data for one region in which the given tiles can be left to the north and east. */
    private static WorldCollisionData oneRegion(int regionX, int regionY, int[]... openTiles) {
        BitSet bits = new BitSet();
        for (int[] tile : openTiles) {
            int base = (tile[2] * 64 * 64 + (tile[1] & 63) * 64 + (tile[0] & 63)) * 4;
            bits.set(base + WorldCollisionData.FLAG_NORTH);
            bits.set(base + WorldCollisionData.FLAG_EAST);
        }
        return new WorldCollisionData(Map.of(WorldCollisionData.regionKey(regionX, regionY), bits.toByteArray()));
    }

    @Test
    void collisionDataAnswersForItsTilesAndSaysNoEverywhereElse() {
        WorldCollisionData data = oneRegion(50, 50, new int[]{3200, 3200, 0}, new int[]{3263, 3263, 0});

        assertTrue(data.get(3200, 3200, 0, WorldCollisionData.FLAG_NORTH));
        assertTrue(data.get(3263, 3263, 0, WorldCollisionData.FLAG_EAST));
        assertFalse(data.get(3201, 3200, 0, WorldCollisionData.FLAG_NORTH));
        assertFalse(data.get(3200, 3200, 1, WorldCollisionData.FLAG_NORTH), "the region has one plane");
        assertFalse(data.get(3264, 3200, 0, WorldCollisionData.FLAG_NORTH), "the next region is not on the map");
        assertFalse(data.get(3136, 3200, 0, WorldCollisionData.FLAG_NORTH), "nor is the one before it");
        assertEquals(1, data.planeCount(data.regionIndex(50, 50)));
        assertEquals(-1, data.regionIndex(49, 50));
    }

    @Test
    void aTileCanBeWalkedOffOnlyWhereAnEdgeIsOpen() {
        CollisionMap map = new CollisionMap(oneRegion(50, 50,
                new int[]{3200, 3200, 0}, new int[]{3200, 3201, 0}, new int[]{3201, 3200, 0}, new int[]{3201, 3201, 0}));

        assertTrue(map.n(3200, 3200, 0) && map.e(3200, 3200, 0));
        assertTrue(map.s(3200, 3201, 0), "going south crosses the north edge of the tile below");
        assertTrue(map.w(3201, 3200, 0), "going west crosses the east edge of the tile beside");
        assertFalse(map.s(3200, 3200, 0));
        assertFalse(map.isBlocked(3200, 3200, 0));
        assertTrue(map.isBlocked(3210, 3210, 0));
    }

    @Test
    void walkingNeighboursAreTheTilesBehindOpenEdgesAndADiagonalNeedsBothWays() {
        CollisionMap map = new CollisionMap(oneRegion(50, 50,
                new int[]{3200, 3200, 0}, new int[]{3200, 3201, 0}, new int[]{3201, 3200, 0}, new int[]{3201, 3201, 0}));
        VisitedTiles visited = new VisitedTiles(map.data());
        NodeGraph graph = new NodeGraph(8);
        int start = graph.createStart(PackedPoint.pack(3200, 3200, 0));
        visited.set(start, graph);

        IntList neighbors = map.getNeighbors(start, visited, TransportNetwork.EMPTY, 0, false, graph);

        java.util.Set<WorldPoint> tiles = new java.util.HashSet<>();
        int teleportNodes = 0;
        for (int i = 0; i < neighbors.size(); i++) {
            if (graph.isTile(neighbors.get(i))) {
                tiles.add(PackedPoint.unpack(graph.packedPosition(neighbors.get(i))));
            } else {
                teleportNodes++;
            }
        }
        assertEquals(java.util.Set.of(
                new WorldPoint(3200, 3201, 0), new WorldPoint(3201, 3200, 0), new WorldPoint(3201, 3201, 0)), tiles);
        assertEquals(1, teleportNodes, "the teleports usable from anywhere are tried once from here");
    }

    /**
     * Builds a strip of ground along y 3205 that can only be walked east and west, so the tile
     * north of each of its tiles is blocked on all four sides, the way the tile under a fairy ring
     * is.
     */
    private static WorldCollisionData groundBelowBlockedTiles() {
        BitSet bits = new BitSet();
        for (int x = 3204; x <= 3207; x++) {
            bits.set((((3205 & 63) * 64) + (x & 63)) * 4 + WorldCollisionData.FLAG_EAST);
        }
        return new WorldCollisionData(Map.of(WorldCollisionData.regionKey(50, 50), bits.toByteArray()));
    }

    private static java.util.Set<WorldPoint> tileNeighbours(CollisionMap map, TransportNetwork network, WorldPoint from) {
        VisitedTiles visited = new VisitedTiles(map.data());
        NodeGraph graph = new NodeGraph(8);
        int start = graph.createStart(PackedPoint.pack(from));
        visited.set(start, graph);

        IntList neighbors = map.getNeighbors(start, visited, network, 0, false, graph);
        java.util.Set<WorldPoint> tiles = new java.util.HashSet<>();
        for (int i = 0; i < neighbors.size(); i++) {
            if (graph.isTile(neighbors.get(i)) && !graph.isTransport(neighbors.get(i))) {
                tiles.add(PackedPoint.unpack(graph.packedPosition(neighbors.get(i))));
            }
        }
        return tiles;
    }

    @Test
    void aBlockedTileIsSteppedOntoOnlyWhenATransportStartsThere() {
        CollisionMap map = new CollisionMap(groundBelowBlockedTiles());
        WorldPoint ground = new WorldPoint(3205, 3205, 0);
        WorldPoint ring = new WorldPoint(3205, 3206, 0);
        TransportNetwork.Builder withRing = new TransportNetwork.Builder(false);
        withRing.add(TransportEdge.of(com.kraken.api.service.walker.transport.registry.TransportDefinition
                .object("test:ring", ring, new WorldPoint(2412, 4434, 0), 29560, "Use").build())[0]);

        assertTrue(map.isBlocked(ring.getX(), ring.getY(), 0));
        assertFalse(map.isBlocked(ground.getX(), ground.getY(), 0));
        assertEquals(java.util.Set.of(ground.dx(-1), ground.dx(1)), tileNeighbours(map, TransportNetwork.EMPTY, ground),
                "a blocked tile with no transport is a wall");
        assertEquals(java.util.Set.of(ground.dx(-1), ground.dx(1), ring), tileNeighbours(map, withRing.build(), ground),
                "the tile under a transport can be stepped onto to take it");
    }

    @Test
    void fromABlockedTileEveryNeighbourThatIsNotBlockedCanBeSteppedOnto() {
        CollisionMap map = new CollisionMap(groundBelowBlockedTiles());
        WorldPoint ring = new WorldPoint(3205, 3206, 0);

        assertEquals(java.util.Set.of(ring.dy(-1)), tileNeighbours(map, TransportNetwork.EMPTY, ring),
                "the ground south of it is open, the blocked tiles beside it and the diagonals past them are not");
    }

    @Test
    void visitedTilesRememberTilesAndTreatWhatIsOffTheMapAsVisited() {
        WorldCollisionData data = oneRegion(50, 50, new int[]{3200, 3200, 0});
        VisitedTiles visited = new VisitedTiles(data);
        int tile = PackedPoint.pack(3205, 3205, 0);

        assertFalse(visited.get(tile));
        assertTrue(visited.set(tile), "the first visit is new");
        assertFalse(visited.set(tile), "the second is not");
        assertTrue(visited.get(tile));
        assertTrue(visited.get(PackedPoint.pack(3205, 3205, 1)), "a plane the map lacks is never explored");
        assertTrue(visited.get(PackedPoint.pack(5000, 5000, 0)), "a region the map lacks is never explored");
        assertFalse(visited.getTeleportNode(TeleportBucket.IN_WILDERNESS));

        visited.clear();
        assertFalse(visited.get(tile));
    }

    @Test
    void theBundledCollisionMapCoversTheWorldAndKnowsLumbridgeCastle() {
        CollisionMap map = new CollisionMap(WorldCollisionData.bundled());

        assertTrue(WorldCollisionData.bundled().regionCount() > 2_000);
        assertFalse(map.isBlocked(3222, 3218, 0), "the castle courtyard is open ground");
        assertTrue(map.n(3222, 3218, 0) && map.e(3222, 3218, 0) && map.s(3222, 3218, 0) && map.w(3222, 3218, 0));
        assertFalse(map.n(3222, 3218, 3), "there is no fourth floor above the courtyard");
    }
}
