package com.kraken.api.service.pathfinding;

import com.kraken.api.Context;
import com.kraken.api.core.script.ScriptCancellation;
import com.kraken.api.service.walker.transport.WildernessLevels;
import com.kraken.api.service.walker.transport.registry.TransportCategory;
import com.kraken.api.service.walker.transport.registry.TransportDefinition;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Constants;
import net.runelite.api.Player;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Finds routes between tiles across the game world. A route can use walking and the transports allowed by a
 * {@link GlobalPathfinderConfig}, such as shortcuts, teleports and boats.
 *
 * <p>The search runs over the collision map and the transport dataset bundled with the API, plus
 * whatever transports are registered with the transport registry.</p>
 */
@Slf4j
@Singleton
public class GlobalPathfinder {
    private static final GlobalPathfinderConfig DEFAULT_CONFIG = GlobalPathfinderConfig.builder().build();
    private static final PathResult EMPTY_RESULT = PathResult.empty();

    @Inject
    private Context ctx;

    @Inject
    private Client client;

    @Inject
    private TransportPlanner transportPlanner;

    @Getter
    private volatile PathResult lastResult = EMPTY_RESULT;

    /**
     * Finds a dense path from the local player to the target using default settings.
     * @param destination The destination point.
     * @return A {@code List} of {@code WorldPoint} objects representing the computed path as a sequence of tiles.
     * If the path could not be computed successfully, an empty list is returned.
     */
    public List<WorldPoint> findPath(WorldPoint destination) {
        return findPath(resolveLocalPlayerPoint(), destination, DEFAULT_CONFIG);
    }

    /**
     * Finds a dense tile-by-tile path from the local player's current location to the specified destination
     * using the provided configuration.
     *
     * @param destination The target {@code WorldPoint} to which the path should be generated.
     *                    This represents the final goal location in the game world.
     * @param config      An instance of {@code GlobalPathfinderConfig} containing configuration options
     *                    that influence path generation behaviors, including transport options or path constraints.
     *
     * @return A {@code List} of {@code WorldPoint} objects representing the computed path as a sequence of tiles.
     *         If the path could not be computed successfully, an empty list is returned.
     */
    public List<WorldPoint> findPath(WorldPoint destination, GlobalPathfinderConfig config) {
        return findPath(resolveLocalPlayerPoint(), destination, config);
    }

    /**
     * Finds a dense tile-by-tile path between two {@code WorldPoint} locations using default configuration settings.
     *
     * <p>This method calculates a detailed path from the specified source point to the destination point
     * in the game world, utilizing the {@code DEFAULT_CONFIG} for pathfinding.
     *
     * @param source      The starting {@code WorldPoint} for the path calculation.
     *                    This represents the initial location in the game world.
     * @param destination The target {@code WorldPoint} to which the path should be generated.
     *                    This represents the final goal location in the game world.
     *
     * @return A {@code List} of {@code WorldPoint} objects representing the computed path as a sequence of tiles.
     *         An empty list is returned if the path could not be computed successfully.
     */
    public List<WorldPoint> findPath(WorldPoint source, WorldPoint destination) {
        return findPath(source, destination, DEFAULT_CONFIG);
    }

    /**
     * Finds a path between two {@code WorldPoint} locations in the game world using the specified pathfinding configuration.
     *
     * <p>This method calculates a tile-by-tile path from the specified source point to the destination point,
     * leveraging the provided {@code GlobalPathfinderConfig} to determine the path generation behavior.
     * If the computed path is valid and complete, it is returned as a {@code List} of {@code WorldPoint} objects.
     * Otherwise, an empty list is returned.
     *
     * @param source      The starting {@code WorldPoint} for the path calculation.
     *                    Represents the initial location in the game world.
     * @param destination The target {@code WorldPoint} to which the path should be generated.
     *                    Represents the final desired location in the game world.
     * @param config      An instance of {@code GlobalPathfinderConfig} providing configuration options for the path generation,
     *                    such as transport settings or path constraints.
     *
     * @return A {@code List} of {@code WorldPoint} objects representing the computed path.
     *         If the path could not be computed successfully, an empty {@code List} is returned.
     */
    public List<WorldPoint> findPath(WorldPoint source, WorldPoint destination, GlobalPathfinderConfig config) {
        PathResult result = findPathResult(source, destination, config);
        return result.isComplete() ? result.getPath() : Collections.emptyList();
    }

    /**
     * Computes a sparse path consisting of waypoint clicks from the local player's current location to the
     * specified destination using default configuration settings.
     *
     * <p>This method collapses a dense path into fewer waypoint clicks while maintaining functionality
     * and transport transitions. The primary purpose is to optimize movement paths by reducing unnecessary
     * intermediate steps, making navigation more efficient for the player.
     *
     * @param destination The target {@code WorldPoint} to which the sparse path should be generated.
     *                    This represents the final goal location in the game world.
     *
     * @return A {@code List} of {@code WorldPoint} objects representing the sparse path to the destination.
     *         If the path could not be computed successfully, an empty {@code List} is returned.
     */
    public List<WorldPoint> findSparsePath(WorldPoint destination) {
        return findSparsePath(resolveLocalPlayerPoint(), destination, DEFAULT_CONFIG);
    }

    /**
     * Computes a sparse path consisting of waypoint clicks from the local player's current location
     * to the specified destination using the provided configuration.
     *
     * <p>The computed path reduces unnecessary intermediate steps, collapsing a dense path into
     * fewer waypoints while maintaining essential navigation functionality and transitions.
     * The resulting path optimizes movement efficiency by prioritizing key waypoints over tile-by-tile precision.
     *
     * @param destination The target {@code WorldPoint} to which the sparse path should be generated.
     *                    Represents the final goal location in the game world.
     * @param config      An instance of {@code GlobalPathfinderConfig} containing configuration options.
     *                    This influences pathfinding behaviors such as transport handling and constraints.
     *
     * @return A {@code List} of {@code WorldPoint} objects representing the computed sparse path.
     *         If the path could not be computed successfully, an empty {@code List} is returned.
     */
    public List<WorldPoint> findSparsePath(WorldPoint destination, GlobalPathfinderConfig config) {
        return findSparsePath(resolveLocalPlayerPoint(), destination, config);
    }

    /**
     * Finds a sparse path between the specified source and destination {@literal WorldPoint}s.
     * <p>
     * This method calculates a path consisting of fewer nodes to traverse,
     * simplifying navigation or optimizing pathfinding where sparse routes are needed.
     * </p>
     *
     * @param source the starting {@literal WorldPoint} of the path.
     * @param destination the ending {@literal WorldPoint} of the path.
     * @return a {@literal List<WorldPoint>} representing the sparse path from the source to the destination.
     */
    public List<WorldPoint> findSparsePath(WorldPoint source, WorldPoint destination) {
        return findSparsePath(source, destination, DEFAULT_CONFIG);
    }

    /**
     * Finds a sparse path between the given source and destination points using the specified pathfinding configuration.
     * <p>
     * A sparse path consists of a subset of path points that represent key waypoints along the entire path.
     * If the pathfinding result is incomplete, an empty list is returned.
     *
     * @param source The starting point of the path. Must not be {@literal null}.
     * @param destination The ending point of the path. Must not be {@literal null}.
     * @param config The configuration settings to be used by the pathfinding algorithm. Must not be {@literal null}.
     * @return A {@literal List<WorldPoint>} representing the sparse path if the pathfinding is complete; otherwise,
     *         an empty list if the pathfinding result is incomplete.
     */
    public List<WorldPoint> findSparsePath(WorldPoint source, WorldPoint destination, GlobalPathfinderConfig config) {
        PathResult result = findPathResult(source, destination, config);
        return result.isComplete() ? result.getSparsePath() : Collections.emptyList();
    }

    /**
     * Finds the path result from the local player's current position to the specified destination
     * using the default configuration.
     *
     * <p>This method resolves the local player's current position and calculates the path to the
     * given destination point in the world.
     *
     * @param destination the target {@literal @}WorldPoint to which the path should be calculated
     * @return a {@literal @}PathResult representing the calculated path from the local player's
     *         position to the specified destination
     */
    public PathResult findPathResult(WorldPoint destination) {
        return findPathResult(resolveLocalPlayerPoint(), destination, DEFAULT_CONFIG);
    }

    /**
     * Finds and returns the {@link PathResult} for navigating to the specified destination
     * using the given pathfinder configuration.
     *
     * <p>This method computes the path starting from the local player's position
     * and determines the best route to the specified destination based on the provided
     * configuration parameters.</p>
     *
     * @param destination the target {@link WorldPoint} to which the path should be calculated.
     *        It represents the endpoint of the pathfinding operation.
     * @param config the {@link GlobalPathfinderConfig} containing configuration parameters
     *        such as traversal rules, movement constraints, and additional pathfinding options.
     * @return a {@link PathResult} object containing the computed path data,
     *         including waypoints, traversal status, and relevant metadata.
     */
    public PathResult findPathResult(WorldPoint destination, GlobalPathfinderConfig config) {
        return findPathResult(resolveLocalPlayerPoint(), destination, config);
    }

    /**
     * Finds the path result between a source point and a destination point in the world.
     * <p>
     * This method calculates the path using the provided source and destination
     * points within the given world configuration.
     *
     * @param source The starting point in the world. Must not be {@literal null}.
     * @param destination The end point in the world to navigate to. Must not be {@literal null}.
     * @return A {@code PathResult} object representing the computed path between the source
     *         and destination points, including any metadata about the pathfinding process.
     */
    public PathResult findPathResult(WorldPoint source, WorldPoint destination) {
        return findPathResult(source, destination, DEFAULT_CONFIG);
    }

    /**
     * Finds the path result based on the given source and destination points within a global pathfinding context.
     * This method performs the pathfinding operation and returns a {@code PathResult} object containing the path details.
     * If the provided source or destination is {@code null}, an empty {@code PathResult} is returned.
     *
     * <p>Blocking calls on the client thread are rejected. Each request owns its captured configuration.
     * Search limits return an incomplete best-effort route; lastResult is the last request to finish.</p>
     *
     * @param source The starting point of the pathfinding operation. Must be a valid {@code WorldPoint}.
     *               If {@code null}, an empty {@code PathResult} is returned.
     * @param destination The endpoint of the pathfinding operation. Must be a valid {@code WorldPoint}.
     *                    If {@code null}, an empty {@code PathResult} is returned.
     * @param config Optional configuration object ({@code GlobalPathfinderConfig}) for the pathfinding process.
     *               If {@code null}, the default configuration is used.
     *
     * @return A {@code PathResult} object containing the pathfinding results. If the operation fails
     *         or there are no valid targets, an empty {@code PathResult} is returned. The returned result
     *         includes details such as the computed path, resolved configuration, and other related metadata.
     */
    public PathResult findPathResult(WorldPoint source, WorldPoint destination, GlobalPathfinderConfig config) {
        requireWorkerThread();
        GlobalPathfinderConfig resolvedConfig = Objects.requireNonNullElse(config, DEFAULT_CONFIG);
        if (resolvedConfig.getMaxSearchMillis() <= 0 || resolvedConfig.getMaxSearchNodes() <= 0) {
            throw new IllegalArgumentException("Search time and node limits must be positive");
        }
        if (source == null || destination == null || searchCancelled()) {
            return lastResult = PathResult.empty(resolvedConfig, source, destination);
        }

        // The dataset and the collision map are read from the jar on this worker. The transports
        // the search may use are then captured on the client thread into a network of its own,
        // which no later capture can change while this worker searches.
        transportPlanner.warmUp();
        CollisionMap map = createCollisionMap();
        PreparedSearch prepared = ctx.runOnClientThread(() -> prepare(source, destination, map, resolvedConfig));
        if (prepared == null || searchCancelled()) {
            return lastResult = PathResult.empty(resolvedConfig, source, destination);
        }
        SearchState searchState = search(prepared.start, prepared.target, prepared.network, prepared.map, resolvedConfig);
        return lastResult = buildResult(source, destination, resolvedConfig, prepared.network, searchState);
    }

    /**
     * Creates the map one search walks over. The map keeps scratch buffers, so each search gets
     * its own. This reads the bundled collision data on first use, so it belongs on a worker.
     *
     * @return a map over the collision data bundled with the API
     */
    CollisionMap createCollisionMap() {
        return new CollisionMap(WorldCollisionData.bundled());
    }

    private void requireWorkerThread() {
        if (client.isClientThread()) {
            throw new IllegalStateException("Global pathfinding must run off the client thread");
        }
    }

    private static boolean searchCancelled() {
        return Thread.currentThread().isInterrupted() || ScriptCancellation.currentThreadCancelled();
    }

    /** Clears the cached route used by overlays and callers inspecting the last result. */
    public void clearLastResult() {
        lastResult = EMPTY_RESULT;
    }

    /**
     * Captures, on the client thread, the transports this search may use.
     */
    private PreparedSearch prepare(WorldPoint source, WorldPoint destination, CollisionMap map,
                                   GlobalPathfinderConfig config) {
        return new PreparedSearch(
                PackedPoint.pack(source),
                PackedPoint.pack(destination),
                map,
                transportPlanner.plan(config));
    }

    /**
     * Runs the graph search over walkable tiles and transport edges.
     *
     * <p>Tiles reached by walking wait in a first in, first out queue, which visits them in order
     * of distance. Tiles reached by a transport wait in a heap ordered by cost, and one is taken
     * as soon as it is cheaper than the next walked tile. The search ends on the target, or with
     * the closest tile it found when it runs out of tiles, time or nodes.</p>
     *
     * @param start the packed tile to search from
     * @param target the packed tile to reach
     * @param network the transports the search may use
     * @param map the map to walk over
     * @param limits the time and node limits of the search
     * @return the route found and how much was explored
     */
    SearchState search(int start, int target, TransportNetwork network, CollisionMap map,
                       GlobalPathfinderConfig limits) {
        VisitedTiles visited = new VisitedTiles(map.data());
        SearchBudget budget = new SearchBudget(limits.getMaxSearchMillis(), limits.getMaxSearchNodes(), System::nanoTime);
        NodeGraph graph = new BudgetedNodeGraph(budget);

        IntDeque boundary = new IntDeque(4096);
        IntMinHeap pending = new IntMinHeap(graph, 256);
        boolean targetInWilderness = WildernessLevels.isInWilderness(target);

        int bestDistance = Integer.MAX_VALUE;
        long bestHeuristic = Integer.MAX_VALUE;
        long cutoffDurationNanos = TimeUnit.MILLISECONDS.toNanos(noProgressCutoffMillis(limits));
        long lastProgressNanos = System.nanoTime();
        int wildernessLevel = 31;
        int nodesChecked = 0;
        int transportsChecked = 0;
        int bestLastNode = NodeGraph.NO_NODE;
        boolean complete = false;

        try {
            try {
                boundary.addLast(graph.createStart(start));
                while (!boundary.isEmpty() || !pending.isEmpty()) {
                    if (budget.exhausted() || System.nanoTime() - lastProgressNanos >= cutoffDurationNanos) break;
                    int boundaryHead = boundary.peekFirst();
                    int pendingHead = pending.peek();
                    int node;

                    if (pendingHead != NodeGraph.NO_NODE
                            && (boundaryHead == NodeGraph.NO_NODE || graph.compareCost(pendingHead) < graph.cost(boundaryHead))) {
                        node = pending.poll();

                        if (graph.isDelayedVisit(node)) {
                            int packed = graph.packedPosition(node);
                            if (visited.get(packed)) {
                                continue;
                            }
                            visited.set(packed);
                        }
                    } else {
                        node = boundary.pollFirst();
                    }

                    if (node == NodeGraph.NO_NODE) {
                        continue;
                    }

                    final boolean nodeIsTile = graph.isTile(node);
                    final int nodePacked = nodeIsTile ? graph.packedPosition(node) : PackedPoint.UNDEFINED;

                    // The level only ever drops: once the search has stood outside an area, the
                    // teleports that work there are open to it.
                    if (nodeIsTile && wildernessLevel > 0) {
                        if (wildernessLevel > 30 && !WildernessLevels.isAboveLevel30(nodePacked)) {
                            wildernessLevel = 30;
                        }
                        if (wildernessLevel > 20 && !WildernessLevels.isAboveLevel20(nodePacked)) {
                            wildernessLevel = 20;
                        }
                        if (wildernessLevel > 0 && !WildernessLevels.isInWilderness(nodePacked)) {
                            wildernessLevel = 0;
                        }
                    }

                    if (nodeIsTile && nodePacked == target) {
                        bestLastNode = node;
                        complete = true;
                        break;
                    }

                    if (nodeIsTile) {
                        int distance = PackedPoint.distance(nodePacked, target);
                        long heuristic = distance + (long) PackedPoint.manhattanDistance(nodePacked, target);
                        if (heuristic < bestHeuristic || (heuristic <= bestHeuristic && distance < bestDistance)) {
                            bestLastNode = node;
                            bestDistance = distance;
                            bestHeuristic = heuristic;
                            lastProgressNanos = System.nanoTime();
                        }
                    }

                    IntList neighbors = map.getNeighbors(node, visited, network, wildernessLevel, targetInWilderness, graph);
                    final int count = neighbors.size();
                    for (int i = 0; i < count; i++) {
                        int neighbor = neighbors.get(i);
                        final boolean neighborIsTile = graph.isTile(neighbor);

                        if (nodeIsTile && neighborIsTile
                                && network.avoidWilderness(nodePacked, graph.packedPosition(neighbor), targetInWilderness)) {
                            continue;
                        }

                        final boolean neighborIsTransport = graph.isTransport(neighbor);
                        if (!(neighborIsTransport && graph.isDelayedVisit(neighbor))) {
                            visited.set(neighbor, graph);
                        }

                        if (neighborIsTransport) {
                            pending.add(neighbor);
                            transportsChecked++;
                        } else {
                            boundary.addLast(neighbor);
                            nodesChecked++;
                        }
                    }
                }
            } catch (SearchBudget.Exhausted e) {
                // A neighbor expansion reached a hard limit before allocating another graph node.
            }

            int[] path = bestLastNode != NodeGraph.NO_NODE ? graph.pathTo(bestLastNode) : new int[0];
            return new SearchState(path, complete, nodesChecked, transportsChecked);
        } finally {
            boundary.clear();
            pending.clear();
            visited.clear();
            graph.release();
        }
    }

    /**
     * Returns how long a search may go without getting closer to its target. The limit is counted
     * in whole game ticks, and is never less than one.
     */
    private static long noProgressCutoffMillis(GlobalPathfinderConfig limits) {
        long cutoffMillis = Math.max(Constants.GAME_TICK_LENGTH, limits.getCalculationCutoffMillis());
        long ticks = Math.max(1L, (cutoffMillis + Constants.GAME_TICK_LENGTH - 1) / Constants.GAME_TICK_LENGTH);
        return ticks * Constants.GAME_TICK_LENGTH;
    }

    /** Converts the route a search found into the API-facing route result object. */
    private PathResult buildResult(
            WorldPoint source,
            WorldPoint destination,
            GlobalPathfinderConfig config,
            TransportNetwork network,
            SearchState searchState
    ) {
        if (searchState.path.length == 0) {
            return PathResult.empty(config, source, destination);
        }

        List<WorldPoint> densePath = unpackPath(searchState.path);
        List<TransportUsage> transports = findTransportUsages(searchState.path, network);
        List<WorldPoint> sparsePath = toSparsePath(densePath, transports, config.getSparsePathWaypointDistance());
        boolean complete = searchState.complete
                && !densePath.isEmpty()
                && destination.equals(densePath.get(densePath.size() - 1));

        return new PathResult(
                config,
                source,
                destination,
                densePath,
                sparsePath,
                transports,
                complete,
                searchState.nodesChecked,
                searchState.transportsChecked
        );
    }

    /**
     * Matches route steps against transport edges so callers can see which hops were used.
     *
     * @param path the packed tiles of the route
     * @param network the transports the search could use
     * @return the transports the route takes, in route order
     */
    static List<TransportUsage> findTransportUsages(int[] path, TransportNetwork network) {
        if (path.length < 2) {
            return Collections.emptyList();
        }

        List<TransportUsage> transportUsages = new ArrayList<>();
        for (int i = 1; i < path.length; i++) {
            int origin = path[i - 1];
            int destination = path[i];

            TransportEdge edge = hopBetween(network.from(origin), network.teleports(), origin, destination);
            if (edge == null) {
                continue;
            }

            TransportDefinition definition = edge.definition;
            transportUsages.add(new TransportUsage(
                    i - 1,
                    PackedPoint.unpack(origin),
                    PackedPoint.unpack(destination),
                    definition.getCategory(),
                    definition.getDisplayInfo(),
                    definition.getObjectInfo(),
                    definition.isConsumable(),
                    definition.getDuration(),
                    definition
            ));
        }

        return Collections.unmodifiableList(transportUsages);
    }

    /**
     * Whether two packed tiles are a single walking step, including diagonals.
     *
     * <p>A teleport usable from anywhere has no origin tile, so it is recovered by matching the
     * landing tile. Walking onto that landing tile later must not count as rubbing the ring.</p>
     *
     * @param from packed start tile
     * @param to packed end tile
     * @return true when the hop is on the same plane and at most one tile away
     */
    static boolean isWalkNeighbour(int from, int to) {
        return PackedPoint.distance(from, to) <= 1;
    }

    /**
     * Picks the transport used between two consecutive path tiles.
     *
     * <p>Edges attached to the origin tile are preferred (doors, gliders, fairy rings). Teleports
     * usable from anywhere, such as jewellery, tabs and spells, are only used when the hop is not a
     * walkable neighbour. The first edge that lands on the destination is taken, so the order of
     * the edges decides between two transports that join the same tiles.</p>
     *
     * @param fromOrigin transports that leave the path tile, may be null
     * @param teleports teleports usable from wherever the player stands, may be null
     * @param origin packed path tile the hop leaves
     * @param destination packed path tile the hop lands on
     * @return the matching edge, or null when the hop is ordinary walking
     */
    static TransportEdge hopBetween(TransportEdge[] fromOrigin, TransportEdge[] teleports, int origin, int destination) {
        TransportEdge tiled = byDestination(fromOrigin, destination);
        if (tiled != null) {
            return tiled;
        }
        if (isWalkNeighbour(origin, destination)) {
            return null;
        }
        return byDestination(teleports, destination);
    }

    private static TransportEdge byDestination(TransportEdge[] edges, int destination) {
        if (edges == null) {
            return null;
        }
        for (TransportEdge edge : edges) {
            if (edge != null && edge.destination == destination) {
                return edge;
            }
        }
        return null;
    }

    /** Unpacks the packed tiles of a route into world points. */
    private List<WorldPoint> unpackPath(int[] packedPath) {
        List<WorldPoint> path = new ArrayList<>(packedPath.length);
        for (int packed : packedPath) {
            path.add(PackedPoint.unpack(packed));
        }
        return Collections.unmodifiableList(path);
    }

    /** Collapses a dense path into click-sized waypoints while preserving transport transitions. */
    private List<WorldPoint> toSparsePath(List<WorldPoint> densePath, List<TransportUsage> transports, int spacing) {
        if (densePath == null || densePath.isEmpty()) {
            return Collections.emptyList();
        }

        int waypointSpacing = Math.max(1, spacing);
        List<WorldPoint> sparsePath = new ArrayList<>();

        int lastWaypointIndex = 0;
        for (int i = 1; i < densePath.size(); i++) {
            TransportUsage transportUsage = transportStartingAt(transports, i - 1);
            if (transportUsage != null) {
                if (!transportUsage.getOrigin().equals(densePath.get(0))) {
                    addIfAbsent(sparsePath, transportUsage.getOrigin());
                }
                addIfAbsent(sparsePath, transportUsage.getDestination());
                lastWaypointIndex = i;
                continue;
            }

            if (i - lastWaypointIndex >= waypointSpacing) {
                addIfAbsent(sparsePath, densePath.get(i));
                lastWaypointIndex = i;
            }
        }

        addIfAbsent(sparsePath, densePath.get(densePath.size() - 1));
        return Collections.unmodifiableList(sparsePath);
    }

    /** Finds the transport that begins at the given dense-path step, if one exists. */
    private TransportUsage transportStartingAt(List<TransportUsage> transports, int pathIndex) {
        for (TransportUsage transport : transports) {
            if (transport.getPathIndex() == pathIndex) {
                return transport;
            }
        }
        return null;
    }

    /** Appends a waypoint only when it differs from the current tail. */
    private void addIfAbsent(List<WorldPoint> path, WorldPoint point) {
        if (path.isEmpty() || !path.get(path.size() - 1).equals(point)) {
            path.add(point);
        }
    }

    /**
     * Resolves the local player's current world point on the client thread.
     *
     * <p>Inside an instance the tile is the one in the world the instance was copied from, and
     * aboard a boat it is where the boat is in the world, since those are the tiles the collision
     * map knows.</p>
     */
    private WorldPoint resolveLocalPlayerPoint() {
        requireWorkerThread();
        return ctx.runOnClientThread(() -> {
            Player player = client.getLocalPlayer();
            if (player == null) {
                return null;
            }

            WorldView view = player.getWorldView();
            LocalPoint local = view != null && view.getId() != WorldView.TOPLEVEL
                    ? client.getTopLevelWorldView().worldEntities().byIndex(view.getId()).getLocalLocation()
                    : player.getLocalLocation();
            return WorldPoint.fromLocalInstance(client, local);
        });
    }

    /**
     * The outcome of a {@link GlobalPathfinder} search: the dense and sparse routes, the transports the route
     * uses, search statistics, and whether the search reached the destination.
     */
    @Getter
    public static final class PathResult {
        private final GlobalPathfinderConfig config;
        private final WorldPoint source;
        private final WorldPoint destination;
        private final List<WorldPoint> path;
        private final List<WorldPoint> sparsePath;
        private final List<TransportUsage> transports;
        private final boolean complete;
        private final int nodesChecked;
        private final int transportsChecked;

        /** Stores the immutable result of a completed path computation. */
        private PathResult(
                GlobalPathfinderConfig config,
                WorldPoint source,
                WorldPoint destination,
                List<WorldPoint> path,
                List<WorldPoint> sparsePath,
                List<TransportUsage> transports,
                boolean complete,
                int nodesChecked,
                int transportsChecked
        ) {
            this.config = config;
            this.source = source;
            this.destination = destination;
            this.path = List.copyOf(path);
            this.sparsePath = List.copyOf(sparsePath);
            this.transports = List.copyOf(transports);
            this.complete = complete;
            this.nodesChecked = nodesChecked;
            this.transportsChecked = transportsChecked;
        }

        /** Creates an empty result with no route data. */
        private static PathResult empty() {
            return empty(DEFAULT_CONFIG, null, null);
        }

        /** Creates an empty result while preserving the caller's source and destination. */
        private static PathResult empty(GlobalPathfinderConfig config, WorldPoint source, WorldPoint destination) {
            return new PathResult(
                    config,
                    source,
                    destination,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList(),
                    false,
                    0,
                    0
            );
        }
    }

    /** Describes a single transport chosen in the final route. */
    @Getter
    public static final class TransportUsage {
        private final int pathIndex;
        private final WorldPoint origin;
        private final WorldPoint destination;

        /** The kind of transport, which is the category of its definition. */
        private final TransportCategory type;
        private final String displayInfo;
        private final String objectInfo;
        private final boolean consumable;
        private final int duration;

        /**
         * The definition of the transport this step takes, which says how it is operated and what
         * the player needs. It is part of the dataset bundled with the API or was registered with
         * the transport registry. Null only for a usage built without one.
         */
        private final TransportDefinition definition;

        /**
         * Builds a usage that carries no definition, for display.
         *
         * @param pathIndex index into the route where the transport is entered
         * @param origin the tile the transport is entered from
         * @param destination the tile the transport leads to
         * @param type the kind of transport
         * @param displayInfo names the destination for transports that offer a choice
         * @param objectInfo what to interact with, as one string
         * @param consumable whether using the transport consumes an item
         * @param duration how long the transport takes, in game ticks
         */
        public TransportUsage(int pathIndex, WorldPoint origin, WorldPoint destination, TransportCategory type,
                              String displayInfo, String objectInfo, boolean consumable, int duration) {
            this(pathIndex, origin, destination, type, displayInfo, objectInfo, consumable, duration, null);
        }

        /**
         * Builds a usage for a transport.
         *
         * @param pathIndex index into the route where the transport is entered
         * @param origin the tile the transport is entered from
         * @param destination the tile the transport leads to
         * @param type the kind of transport
         * @param displayInfo names the destination for transports that offer a choice
         * @param objectInfo what to interact with, as one string
         * @param consumable whether using the transport consumes an item
         * @param duration how long the transport takes, in game ticks
         * @param definition the definition of the transport, may be null
         */
        public TransportUsage(int pathIndex, WorldPoint origin, WorldPoint destination, TransportCategory type,
                              String displayInfo, String objectInfo, boolean consumable, int duration,
                              TransportDefinition definition) {
            this.pathIndex = pathIndex;
            this.origin = origin;
            this.destination = destination;
            this.type = type;
            this.displayInfo = displayInfo;
            this.objectInfo = objectInfo;
            this.consumable = consumable;
            this.duration = duration;
            this.definition = definition;
        }

        /**
         * Reports whether the transport is a teleport usable from wherever the player stands. Its
         * origin is then the tile the route leaves from, not a place the transport has to be
         * entered at.
         *
         * @return true when the definition has no origin tile
         */
        public boolean isTeleport() {
            return definition != null && definition.isUsableAnywhere();
        }
    }

    /** Bundles the packed search inputs with the transports captured for the search. */
    @AllArgsConstructor
    private static final class PreparedSearch {
        private final int start;
        private final int target;
        private final CollisionMap map;
        private final TransportNetwork network;
    }

    /** The route a search found and lightweight search metrics. */
    @AllArgsConstructor
    static final class SearchState {
        final int[] path;
        final boolean complete;
        final int nodesChecked;
        final int transportsChecked;
    }
}
