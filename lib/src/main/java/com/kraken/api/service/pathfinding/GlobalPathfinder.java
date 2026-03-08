package com.kraken.api.service.pathfinding;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.coords.WorldPoint;

import java.io.*;
import java.util.*;

@Slf4j
@Singleton
public class GlobalPathfinder {

    private static final String MAP_RESOURCE_PATH = "/map.dat";
    private static final int MIN_X = 0;
    private static final int MAX_X = 0x1FFF;
    private static final int MIN_Y = 0;
    private static final int MAX_Y = 0x7FFF;
    private static final int MIN_PLANE = 0;
    private static final int MAX_PLANE = 3;
    private static final int DEFAULT_MAX_EXPANDED_NODES = 2_000_000;

    private static final int X_BITS = 13;
    private static final int Y_BITS = 15;
    private static final int X_MASK = (1 << X_BITS) - 1;
    private static final int Y_MASK = (1 << Y_BITS) - 1;
    private static final int PLANE_SHIFT = X_BITS + Y_BITS;
    private static final int PLANE_MASK = 0x3;
    private static final int EAST_BLOCK_FLAG = 1 << 30;

    private static final byte NORTHWEST = 0x1;
    private static final byte NORTH = 0x2;
    private static final byte NORTHEAST = 0x4;
    private static final byte WEST = 0x8;
    private static final byte EAST = 0x10;
    private static final byte SOUTHWEST = 0x20;
    private static final byte SOUTH = 0x40;
    private static final byte SOUTHEAST = (byte) 0x80;
    private static final byte NONE = 0x0;

    private static final int CARDINAL_COST = 10;
    private static final int DIAGONAL_COST = 14;

    private final SparseBitSet bitSet;
    /**
     * -- GETTER --
     *  Indicates whether the collision map was loaded successfully.
     *
     * @return {@literal true} when the collision map is available and pathfinding can run.
     */
    @Getter
    private final boolean mapLoaded;

    /**
     * Constructs a global pathfinder instance and attempts to load the bundled global collision map.
     */
    @Inject
    public GlobalPathfinder() {
        SparseBitSet loadedBitSet = null;
        try {
            loadedBitSet = readBitSetFromResource(MAP_RESOURCE_PATH);
        } catch (IOException | ClassNotFoundException ex) {
            log.error("Failed to load bundled global collision map from {}", MAP_RESOURCE_PATH, ex);
        }

        this.bitSet = loadedBitSet != null ? loadedBitSet : new SparseBitSet();
        this.mapLoaded = loadedBitSet != null;
    }

    private GlobalPathfinder(SparseBitSet bitSet) {
        this.bitSet = Objects.requireNonNull(bitSet, "bitSet");
        this.mapLoaded = true;
    }

    /**
     * Loads a global pathfinder from the bundled {@code /map.dat} collision map.
     *
     * @return A fully initialized {@literal GlobalPathfinder}.
     * @throws IOException If the map resource cannot be read.
     * @throws ClassNotFoundException If the serialized map payload cannot be deserialized.
     */
    public static GlobalPathfinder load() throws IOException, ClassNotFoundException {
        return new GlobalPathfinder(readBitSetFromResource(MAP_RESOURCE_PATH));
    }

    /**
     * Loads a global pathfinder from a specific serialized map file path.
     *
     * @param filePath The path to the serialized global collision map file.
     * @return A fully initialized {@literal GlobalPathfinder}, or {@literal null} when the file is missing.
     * @throws IOException If the map file cannot be read.
     * @throws ClassNotFoundException If the serialized map payload cannot be deserialized.
     */
    public static GlobalPathfinder load(String filePath) throws IOException, ClassNotFoundException {
        File file = new File(filePath);

        if (!file.exists() || !file.isFile()) {
            log.error("File: {} could not be found.", filePath);
            return null;
        }

        try (InputStream is = new FileInputStream(file)) {
            return new GlobalPathfinder(readBitSet(is));
        }
    }

    /**
     * Checks whether a tile is blocked in the global collision map.
     *
     * @param worldPoint The tile to test.
     * @return {@literal true} when movement into the tile is blocked.
     */
    public boolean isBlocked(WorldPoint worldPoint) {
        if (!mapLoaded || worldPoint == null) {
            return true;
        }

        return !walkable(worldPoint.getX(), worldPoint.getY(), worldPoint.getPlane());
    }

    /**
     * Finds a dense global path between two world points using A* and global collision data.
     *
     * @param start The starting tile.
     * @param target The destination tile.
     * @return A dense path that includes the start and target tiles, or an empty list when no path is found.
     */
    public List<WorldPoint> findPath(WorldPoint start, WorldPoint target) {
        return findPath(start, target, DEFAULT_MAX_EXPANDED_NODES);
    }

    /**
     * Finds a dense global path between two world points using A* and global collision data.
     *
     * @param start The starting tile.
     * @param target The destination tile.
     * @param maxExpandedNodes Maximum number of expanded nodes before aborting search.
     * @return A dense path that includes the start and target tiles, or an empty list when no path is found.
     */
    public List<WorldPoint> findPath(WorldPoint start, WorldPoint target, int maxExpandedNodes) {
        if (!mapLoaded || start == null || target == null) {
            return Collections.emptyList();
        }

        if (start.getPlane() != target.getPlane()) {
            return Collections.emptyList();
        }

        int plane = start.getPlane();
        if (!isValidPlane(plane)) {
            return Collections.emptyList();
        }

        int startX = start.getX();
        int startY = start.getY();
        int targetX = target.getX();
        int targetY = target.getY();

        if (!isWithinWorldBounds(startX, startY) || !isWithinWorldBounds(targetX, targetY)) {
            return Collections.emptyList();
        }

        if (!walkable(startX, startY, plane) || !walkable(targetX, targetY, plane)) {
            return Collections.emptyList();
        }

        if (startX == targetX && startY == targetY) {
            return Collections.singletonList(start);
        }

        int safeNodeLimit = maxExpandedNodes <= 0 ? DEFAULT_MAX_EXPANDED_NODES : maxExpandedNodes;
        int startPacked = packPoint(startX, startY, plane);
        int targetPacked = packPoint(targetX, targetY, plane);

        PriorityQueue<Node> openSet = new PriorityQueue<>(Comparator.comparingInt(node -> node.fScore));
        Int2IntOpenHashMap gScore = new Int2IntOpenHashMap();
        Int2IntOpenHashMap parent = new Int2IntOpenHashMap();
        IntOpenHashSet closedSet = new IntOpenHashSet();

        gScore.defaultReturnValue(Integer.MAX_VALUE);
        int startHeuristic = octileHeuristic(startX, startY, targetX, targetY);
        openSet.add(new Node(startPacked, 0, startHeuristic));
        gScore.put(startPacked, 0);

        int expandedNodes = 0;

        while (!openSet.isEmpty() && expandedNodes < safeNodeLimit) {
            Node current = openSet.poll();
            int knownBestG = gScore.get(current.packedPoint);
            if (current.gScore != knownBestG) {
                continue;
            }

            if (!closedSet.add(current.packedPoint)) {
                continue;
            }

            if (current.packedPoint == targetPacked) {
                return reconstructPath(parent, targetPacked);
            }

            expandedNodes++;

            int currentX = unpackX(current.packedPoint);
            int currentY = unpackY(current.packedPoint);
            byte flags = movementFlags(currentX, currentY, plane);
            if (flags == NONE) {
                continue;
            }

            if ((flags & WEST) != 0) {
                processNeighbor(current.packedPoint, current.gScore, currentX - 1, currentY, plane, targetX, targetY, CARDINAL_COST, openSet, gScore, parent, closedSet);
            }
            if ((flags & EAST) != 0) {
                processNeighbor(current.packedPoint, current.gScore, currentX + 1, currentY, plane, targetX, targetY, CARDINAL_COST, openSet, gScore, parent, closedSet);
            }
            if ((flags & NORTH) != 0) {
                processNeighbor(current.packedPoint, current.gScore, currentX, currentY + 1, plane, targetX, targetY, CARDINAL_COST, openSet, gScore, parent, closedSet);
            }
            if ((flags & SOUTH) != 0) {
                processNeighbor(current.packedPoint, current.gScore, currentX, currentY - 1, plane, targetX, targetY, CARDINAL_COST, openSet, gScore, parent, closedSet);
            }

            if ((flags & NORTHWEST) != 0) {
                processNeighbor(current.packedPoint, current.gScore, currentX - 1, currentY + 1, plane, targetX, targetY, DIAGONAL_COST, openSet, gScore, parent, closedSet);
            }
            if ((flags & NORTHEAST) != 0) {
                processNeighbor(current.packedPoint, current.gScore, currentX + 1, currentY + 1, plane, targetX, targetY, DIAGONAL_COST, openSet, gScore, parent, closedSet);
            }
            if ((flags & SOUTHWEST) != 0) {
                processNeighbor(current.packedPoint, current.gScore, currentX - 1, currentY - 1, plane, targetX, targetY, DIAGONAL_COST, openSet, gScore, parent, closedSet);
            }
            if ((flags & SOUTHEAST) != 0) {
                processNeighbor(current.packedPoint, current.gScore, currentX + 1, currentY - 1, plane, targetX, targetY, DIAGONAL_COST, openSet, gScore, parent, closedSet);
            }
        }

        return Collections.emptyList();
    }

    /**
     * Attempts to find a path to the target tile and progressively backs off around the target if needed.
     *
     * @param start The starting tile.
     * @param target The preferred destination tile.
     * @param maxBackoffRadius Maximum radius used to search for a nearby unblocked fallback destination.
     * @return A dense path including the start tile, or an empty list when no fallback path exists.
     */
    public List<WorldPoint> findPathWithBackoff(WorldPoint start, WorldPoint target, int maxBackoffRadius) {
        List<WorldPoint> directPath = findPath(start, target);
        if (!directPath.isEmpty()) {
            return directPath;
        }

        if (!mapLoaded || start == null || target == null || start.getPlane() != target.getPlane()) {
            return Collections.emptyList();
        }

        int plane = start.getPlane();
        int maxRadius = Math.max(1, maxBackoffRadius);

        for (int radius = 1; radius <= maxRadius; radius++) {
            WorldPoint bestCandidate = null;
            int bestDistance = Integer.MAX_VALUE;

            for (int dx = -radius; dx <= radius; dx++) {
                for (int dy = -radius; dy <= radius; dy++) {
                    if (Math.max(Math.abs(dx), Math.abs(dy)) != radius) {
                        continue;
                    }

                    int candidateX = target.getX() + dx;
                    int candidateY = target.getY() + dy;

                    if (!isWithinWorldBounds(candidateX, candidateY) || !walkable(candidateX, candidateY, plane)) {
                        continue;
                    }

                    int distance = Math.abs(candidateX - start.getX()) + Math.abs(candidateY - start.getY());
                    if (distance < bestDistance) {
                        bestDistance = distance;
                        bestCandidate = new WorldPoint(candidateX, candidateY, plane);
                    }
                }
            }

            if (bestCandidate != null) {
                List<WorldPoint> backoffPath = findPath(start, bestCandidate);
                if (!backoffPath.isEmpty()) {
                    return backoffPath;
                }
            }
        }

        return Collections.emptyList();
    }

    /**
     * Finds a sparse global path where waypoints are only kept when travel direction changes.
     *
     * @param start The starting tile.
     * @param target The destination tile.
     * @return A sparse path that always includes the start and final tile when a path exists.
     */
    public List<WorldPoint> findSparsePath(WorldPoint start, WorldPoint target) {
        return toSparsePath(findPath(start, target));
    }

    /**
     * Reduces a dense path into sparse waypoints by keeping tiles where direction changes.
     *
     * @param densePath A dense path including adjacent steps.
     * @return A sparse waypoint list.
     */
    public List<WorldPoint> toSparsePath(List<WorldPoint> densePath) {
        if (densePath == null || densePath.isEmpty()) {
            return Collections.emptyList();
        }

        if (densePath.size() < 3) {
            return new ArrayList<>(densePath);
        }

        List<WorldPoint> sparsePath = new ArrayList<>();
        sparsePath.add(densePath.get(0));

        int lastDx = Integer.MIN_VALUE;
        int lastDy = Integer.MIN_VALUE;

        for (int i = 1; i < densePath.size(); i++) {
            WorldPoint previous = densePath.get(i - 1);
            WorldPoint current = densePath.get(i);

            int dx = Integer.compare(current.getX(), previous.getX());
            int dy = Integer.compare(current.getY(), previous.getY());

            if (i == 1) {
                lastDx = dx;
                lastDy = dy;
                continue;
            }

            if (dx != lastDx || dy != lastDy) {
                sparsePath.add(previous);
                lastDx = dx;
                lastDy = dy;
            }
        }

        WorldPoint finalTile = densePath.get(densePath.size() - 1);
        if (!finalTile.equals(sparsePath.get(sparsePath.size() - 1))) {
            sparsePath.add(finalTile);
        }

        return sparsePath;
    }

    private void processNeighbor(
            int currentPacked,
            int currentGScore,
            int nextX,
            int nextY,
            int plane,
            int targetX,
            int targetY,
            int movementCost,
            PriorityQueue<Node> openSet,
            Int2IntOpenHashMap gScore,
            Int2IntOpenHashMap parent,
            IntOpenHashSet closedSet
    ) {
        if (!isWithinWorldBounds(nextX, nextY)) {
            return;
        }

        int neighborPacked = packPoint(nextX, nextY, plane);
        if (closedSet.contains(neighborPacked)) {
            return;
        }

        int tentativeG = currentGScore + movementCost;
        int bestKnownG = gScore.get(neighborPacked);
        if (tentativeG >= bestKnownG) {
            return;
        }

        gScore.put(neighborPacked, tentativeG);
        parent.put(neighborPacked, currentPacked);

        int heuristic = octileHeuristic(nextX, nextY, targetX, targetY);
        openSet.add(new Node(neighborPacked, tentativeG, tentativeG + heuristic));
    }

    private List<WorldPoint> reconstructPath(Int2IntOpenHashMap parent, int targetPacked) {
        List<WorldPoint> reversed = new ArrayList<>();
        int current = targetPacked;
        reversed.add(unpack(current));

        while (parent.containsKey(current)) {
            current = parent.get(current);
            reversed.add(unpack(current));
        }

        Collections.reverse(reversed);
        return reversed;
    }

    private byte movementFlags(int x, int y, int plane) {
        boolean north = northOpen(x, y, plane);
        boolean east = eastOpen(x, y, plane);
        boolean south = southOpen(x, y, plane);
        boolean west = westOpen(x, y, plane);

        if (!(north || east || south || west)) {
            return NONE;
        }

        byte flags = NONE;
        if (north) {
            flags |= NORTH;
        }
        if (east) {
            flags |= EAST;
        }
        if (south) {
            flags |= SOUTH;
        }
        if (west) {
            flags |= WEST;
        }

        if (south && west && westOpen(x, y - 1, plane) && southOpen(x - 1, y, plane)) {
            flags |= SOUTHWEST;
        }
        if (south && east && eastOpen(x, y - 1, plane) && southOpen(x + 1, y, plane)) {
            flags |= SOUTHEAST;
        }
        if (north && west && westOpen(x, y + 1, plane) && northOpen(x - 1, y, plane)) {
            flags |= NORTHWEST;
        }
        if (north && east && eastOpen(x, y + 1, plane) && northOpen(x + 1, y, plane)) {
            flags |= NORTHEAST;
        }

        return flags;
    }

    private boolean walkable(int x, int y, int plane) {
        return northOpen(x, y, plane)
                || eastOpen(x, y, plane)
                || southOpen(x, y, plane)
                || westOpen(x, y, plane);
    }

    private boolean northOpen(int x, int y, int plane) {
        if (!isWithinWorldBounds(x, y) || !isValidPlane(plane)) {
            return false;
        }

        return !bitSet.get(packPoint(x, y, plane));
    }

    private boolean eastOpen(int x, int y, int plane) {
        if (!isWithinWorldBounds(x, y) || !isValidPlane(plane)) {
            return false;
        }

        return !bitSet.get(packPoint(x, y, plane) | EAST_BLOCK_FLAG);
    }

    private boolean southOpen(int x, int y, int plane) {
        return northOpen(x, y - 1, plane);
    }

    private boolean westOpen(int x, int y, int plane) {
        return eastOpen(x - 1, y, plane);
    }

    private static boolean isWithinWorldBounds(int x, int y) {
        return x >= MIN_X && x <= MAX_X && y >= MIN_Y && y <= MAX_Y;
    }

    private static boolean isValidPlane(int plane) {
        return plane >= MIN_PLANE && plane <= MAX_PLANE;
    }

    private static int octileHeuristic(int x, int y, int targetX, int targetY) {
        int dx = Math.abs(targetX - x);
        int dy = Math.abs(targetY - y);
        int min = Math.min(dx, dy);
        int max = Math.max(dx, dy);
        return (DIAGONAL_COST * min) + (CARDINAL_COST * (max - min));
    }

    private static int packPoint(int x, int y, int plane) {
        return (x & X_MASK) | ((y & Y_MASK) << X_BITS) | ((plane & PLANE_MASK) << PLANE_SHIFT);
    }

    private static WorldPoint unpack(int packed) {
        return new WorldPoint(unpackX(packed), unpackY(packed), unpackPlane(packed));
    }

    private static int unpackX(int packed) {
        return packed & X_MASK;
    }

    private static int unpackY(int packed) {
        return (packed >>> X_BITS) & Y_MASK;
    }

    private static int unpackPlane(int packed) {
        return (packed >>> PLANE_SHIFT) & PLANE_MASK;
    }

    private static SparseBitSet readBitSetFromResource(String resourcePath) throws IOException, ClassNotFoundException {
        try (InputStream is = GlobalPathfinder.class.getResourceAsStream(resourcePath)) {
            return readBitSet(is);
        }
    }

    private static SparseBitSet readBitSet(InputStream inputStream) throws IOException, ClassNotFoundException {
        if (inputStream == null) {
            throw new FileNotFoundException("Unable to locate map collision resource");
        }

        try (CompatibleObjectInputStream objectInputStream = new CompatibleObjectInputStream(inputStream)) {
            Object payload = objectInputStream.readObject();
            if (!(payload instanceof SparseBitSet)) {
                throw new IOException("Unsupported map payload type: " + payload.getClass().getName());
            }

            return (SparseBitSet) payload;
        }
    }

    private static final class Node {
        private final int packedPoint;
        private final int gScore;
        private final int fScore;

        private Node(int packedPoint, int gScore, int fScore) {
            this.packedPoint = packedPoint;
            this.gScore = gScore;
            this.fScore = fScore;
        }
    }

    private static final class CompatibleObjectInputStream extends ObjectInputStream {
        private CompatibleObjectInputStream(InputStream in) throws IOException {
            super(in);
        }

        @Override
        protected Class<?> resolveClass(ObjectStreamClass descriptor) throws IOException, ClassNotFoundException {
            if ("com.tonic.services.pathfinder.collision.SparseBitSet".equals(descriptor.getName())
                    || "com.kraken.api.service.pathfinding.pathfinder.collision.SparseBitSet".equals(descriptor.getName())) {
                return SparseBitSet.class;
            }

            return super.resolveClass(descriptor);
        }
    }
}
