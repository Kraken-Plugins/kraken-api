package com.kraken.api.service.pathfinding;

import com.google.inject.Inject;
import com.google.inject.Provides;
import com.kraken.api.service.map.WorldPointService;
import com.kraken.api.service.pathfinding.internal.TransportDataset;
import com.kraken.api.service.pathfinding.internal.VisitedTiles;
import com.kraken.api.service.pathfinding.internal.WildernessChecker;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import shortestpath.PrimitiveIntList;
import shortestpath.ShortestPathConfig;
import shortestpath.WorldPointUtil;
import shortestpath.pathfinder.CollisionMap;
import shortestpath.pathfinder.Node;
import shortestpath.pathfinder.TransportNode;

import javax.inject.Singleton;
import java.util.*;

/**
 * Global pathfinder powered by the RuneLite shortest-path collision data and transport datasets.
 * This is independent of the RuneLite plugin runtime and exposes pathfinding through a compact API.
 */
@Slf4j
@Singleton
public class GlobalPathfinder {

    private final shortestpath.pathfinder.PathfinderConfig pathfinderConfig;

    @Inject
    public GlobalPathfinder(Client client, ClientThread clientThread) {
        pathfinderConfig = new shortestpath.pathfinder.PathfinderConfig(client, config);
        if (GameState.LOGGED_IN.equals(client.getGameState())) {
            clientThread.invokeLater(pathfinderConfig::refresh);
        }
    }

    @Provides
    public ShortestPathConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(shortestpath.ShortestPathConfig.class);
    }
    /**
     * Finds the shortest path from {@code start} to {@code destination}.
     *
     * @param start starting world point.
     * @param destination destination world point.
     * @param config pathfinder configuration.
     * @return dense list of world points from start to destination (inclusive).
     */
    public List<WorldPoint> findPath(WorldPoint start, WorldPoint destination, GlobalPathfinderBuilder config) {
        return findPath(start, destination, config, null);
    }

    /**
     * Finds a randomized shortest path from {@code start} to {@code destination}.
     * Randomization is introduced by shuffling neighbor exploration order.
     *
     * @param start starting world point.
     * @param destination destination world point.
     * @param config pathfinder configuration.
     * @param random random source used to vary traversal.
     * @return dense list of world points from start to destination (inclusive).
     */
    public List<WorldPoint> findVariablePath(WorldPoint start, WorldPoint destination, GlobalPathfinderBuilder config, Random random) {
        return findPath(start, destination, config, random);
    }

    /**
     * Finds a randomized shortest path from {@code start} to {@code destination} using a deterministic seed.
     *
     * @param start starting world point.
     * @param destination destination world point.
     * @param config pathfinder configuration.
     * @param seed random seed used to vary traversal.
     * @return dense list of world points from start to destination (inclusive).
     */
    public List<WorldPoint> findVariablePath(WorldPoint start, WorldPoint destination, GlobalPathfinderBuilder config, long seed) {
        return findPath(start, destination, config, new Random(seed));
    }

    /**
     * Computes a sparse path that keeps only waypoints where direction changes occur.
     *
     * @param start starting world point.
     * @param destination destination world point.
     * @param config pathfinder configuration.
     * @return sparse list of world points, excluding the start but including the destination.
     */
    public List<WorldPoint> findSparsePath(WorldPoint start, WorldPoint destination, GlobalPathfinderBuilder config) {
        List<WorldPoint> dense = findPath(start, destination, config);
        return toSparsePath(dense);
    }

    /**
     * Computes a sparse path from a dense path by retaining direction-change waypoints.
     *
     * @param densePath dense path including the start and destination.
     * @return sparse path excluding the start but including the destination.
     */
    public List<WorldPoint> toSparsePath(List<WorldPoint> densePath) {
        if (densePath == null || densePath.isEmpty()) {
            return Collections.emptyList();
        }
        if (densePath.size() == 1) {
            return new ArrayList<>(densePath);
        }

        List<WorldPoint> sparse = new ArrayList<>();
        WorldPoint prev = densePath.get(0);
        int prevDx = 0;
        int prevDy = 0;
        boolean hasPrevDir = false;

        for (int i = 1; i < densePath.size(); i++) {
            WorldPoint current = densePath.get(i);
            int dx = Integer.signum(current.getX() - prev.getX());
            int dy = Integer.signum(current.getY() - prev.getY());
            boolean directionChanged = !hasPrevDir || dx != prevDx || dy != prevDy;
            boolean teleportJump = prev.distanceTo(current) > 1;

            if (directionChanged || teleportJump) {
                sparse.add(prev);
            }

            prevDx = dx;
            prevDy = dy;
            hasPrevDir = true;
            prev = current;
        }

        sparse.add(densePath.get(densePath.size() - 1));
        sparse.remove(0); // remove the start point if it was added as a direction change
        return sparse;
    }

    /**
     * Computes a varied sparse path by selecting alternative waypoints along the dense path.
     *
     * @param start starting world point.
     * @param destination destination world point.
     * @param config pathfinder configuration.
     * @param seed deterministic seed for waypoint variation.
     * @return sparse path with randomized waypoints along the dense path.
     */
    public List<WorldPoint> findVariedSparsePath(WorldPoint start, WorldPoint destination, GlobalPathfinderBuilder config, long seed) {
        return findVariedSparsePath(start, destination, config, new Random(seed));
    }

    /**
     * Computes a varied sparse path by selecting alternative waypoints along the dense path.
     *
     * @param start starting world point.
     * @param destination destination world point.
     * @param config pathfinder configuration.
     * @param random random source for waypoint variation.
     * @return sparse path with randomized waypoints along the dense path.
     */
    public List<WorldPoint> findVariedSparsePath(WorldPoint start, WorldPoint destination, GlobalPathfinderBuilder config, Random random) {
        List<WorldPoint> dense = findPath(start, destination, config);
        List<WorldPoint> sparse = toSparsePath(dense);
        return varySparsePath(dense, sparse, random);
    }

    /**
     * Varies the supplied sparse path by selecting alternative points from the dense path segments.
     * Endpoints are preserved.
     *
     * @param densePath dense path including the start and destination.
     * @param sparsePath sparse path derived from {@code densePath}.
     * @param random random source.
     * @return new sparse path with varied waypoints.
     */
    public List<WorldPoint> varySparsePath(List<WorldPoint> densePath, List<WorldPoint> sparsePath, Random random) {
        if (densePath == null || densePath.isEmpty() || sparsePath == null || sparsePath.isEmpty()) {
            return Collections.emptyList();
        }
        if (densePath.size() == 1 || sparsePath.size() == 1) {
            return new ArrayList<>(sparsePath);
        }
        Random rng = random == null ? new Random() : random;

        List<Integer> indices = new ArrayList<>(sparsePath.size());
        int searchFrom = 0;
        for (WorldPoint waypoint : sparsePath) {
            int index = indexOfFrom(densePath, waypoint, searchFrom);
            if (index < 0) {
                return new ArrayList<>(sparsePath);
            }
            indices.add(index);
            searchFrom = index;
        }

        List<WorldPoint> varied = new ArrayList<>(sparsePath.size());
        int prevIndex = 0;
        for (int i = 0; i < indices.size(); i++) {
            int currentIndex = indices.get(i);
            if (i == indices.size() - 1) {
                varied.add(densePath.get(currentIndex));
                break;
            }

            int min = Math.max(prevIndex + 1, 0);
            int max = Math.max(currentIndex, min);
            int choice = min == max ? max : min + rng.nextInt(max - min + 1);
            varied.add(densePath.get(choice));
            prevIndex = currentIndex;
        }

        return varied;
    }

    private List<WorldPoint> findPath(WorldPoint start, WorldPoint destination, GlobalPathfinderBuilder config, Random random) {
        if (start == null || destination == null || config == null) {
            return Collections.emptyList();
        }
        if (start.equals(destination)) {
            return Collections.singletonList(start);
        }

        int startPacked = WorldPointService.pack(start);
        int destinationPacked = WorldPointService.pack(destination);
        Set<Integer> targets = new HashSet<>();
        targets.add(destinationPacked);

        PrimitiveIntList path = computePath(startPacked, targets, config, random);
        if (path == null || path.isEmpty()) {
            return Collections.emptyList();
        }

        List<WorldPoint> result = new ArrayList<>(path.size());
        for (int i = 0; i < path.size(); i++) {
            result.add(WorldPointService.unpack(path.get(i)));
        }
        return result;
    }

    private PrimitiveIntList computePath(int startPacked,
                                         Set<Integer> targets,
                                         GlobalPathfinderBuilder config,
                                         Random random) {
        CollisionMap map = getCollisionMap();
        TransportDataset dataset = TransportDataset.loadFiltered(config);
        VisitedTiles visited = new VisitedTiles(map);
        Deque<Node> boundary = new ArrayDeque<>(4096);
        Queue<Node> pending = new java.util.PriorityQueue<>(256);

        Node foundTarget = null;
        boolean targetInWilderness = WildernessChecker.isInWilderness(targets);
        int bestDistance = Integer.MAX_VALUE;
        long bestHeuristic = Long.MAX_VALUE;
        long cutoffDurationMillis = config.getCalculationCutoffMillis();
        long cutoffTimeMillis = System.currentTimeMillis() + cutoffDurationMillis;

        visited.set(startPacked);
        boundary.addFirst(new Node(startPacked, null));

        while (!boundary.isEmpty() || !pending.isEmpty()) {
            Node node;
            Node p = pending.peek();
            Node b = boundary.peekFirst();

            if (p != null && (b == null || p.cost < b.cost)) {
                node = pending.poll();
            } else {
                node = boundary.removeFirst();
            }

            if (targets.contains(node.packedPosition)) {
                foundTarget = node;
                break;
            }

            for (int target : targets) {
                int distance = WorldPointUtil.distanceBetween(node.packedPosition, target);
                long heuristic = distance + (long) WorldPointUtil.distanceBetween(node.packedPosition, target, 2);
                if (heuristic < bestHeuristic || (heuristic <= bestHeuristic && distance < bestDistance)) {
                    bestDistance = distance;
                    bestHeuristic = heuristic;
                    cutoffTimeMillis = System.currentTimeMillis() + cutoffDurationMillis;
                }
            }

            if (System.currentTimeMillis() > cutoffTimeMillis) {
                break;
            }

            int wildernessLevel = getWildernessLevel(node.packedPosition);
            List<Node> neighbors = map.getNeighbors(node, visited, dataset, wildernessLevel);
            if (random != null && neighbors.size() > 1) {
                Collections.shuffle(neighbors, random);
            }
            for (Node neighbor : neighbors) {
                if (shouldAvoidWilderness(node.packedPosition, neighbor.packedPosition, targetInWilderness, config)) {
                    continue;
                }
                if (!visited.set(neighbor.packedPosition)) {
                    continue;
                }
                if (neighbor instanceof TransportNode) {
                    pending.add(neighbor);
                } else {
                    boundary.addLast(neighbor);
                }
            }
        }

        if (foundTarget == null) {
            return new PrimitiveIntList();
        }

        return foundTarget.getPath();
    }

    private static boolean shouldAvoidWilderness(int packedPosition,
                                                 int packedNeighborPosition,
                                                 boolean targetInWilderness,
                                                 GlobalPathfinderBuilder config) {
        return config.isAvoidWilderness()
            && !targetInWilderness
            && !WildernessChecker.isInWilderness(packedPosition)
            && WildernessChecker.isInWilderness(packedNeighborPosition);
    }

    private static int getWildernessLevel(int packedPoint) {
        if (WildernessChecker.isInLevel30Wilderness(packedPoint)) {
            return 30;
        }
        if (WildernessChecker.isInLevel20Wilderness(packedPoint)) {
            return 20;
        }
        if (WildernessChecker.isInWilderness(packedPoint)) {
            return 1;
        }
        return 0;
    }

    private static int indexOfFrom(List<WorldPoint> path, WorldPoint target, int start) {
        for (int i = Math.max(start, 0); i < path.size(); i++) {
            if (Objects.equals(path.get(i), target)) {
                return i;
            }
        }
        return -1;
    }
}
