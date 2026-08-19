package com.kraken.api.service.pathfinding;

import com.kraken.api.Context;
import com.kraken.api.service.walker.transport.AlKharidGate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Constants;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.Player;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import shortestpath.*;
import shortestpath.pathfinder.*;
import shortestpath.transport.Transport;
import shortestpath.transport.TransportType;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.*;

@Slf4j
@Singleton
public class GlobalPathfinder {
    private static final GlobalPathfinderConfig DEFAULT_CONFIG = GlobalPathfinderConfig.builder().build();
    private static final Transport[] NO_TRANSPORTS = new Transport[0];
    private static final PathResult EMPTY_RESULT = PathResult.empty();

    @Inject
    private Context ctx;

    @Inject
    private Client client;

    private final MutableShortestPathConfigAdapter configAdapter = new MutableShortestPathConfigAdapter(DEFAULT_CONFIG);
    private PathfinderConfig reusablePathfinderConfig;

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
     * <p><b>Thread-safety:</b> This method is synchronized to ensure thread safety during execution.</p>
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
        synchronized (this) {
            GlobalPathfinderConfig resolvedConfig = Objects.requireNonNullElse(config, DEFAULT_CONFIG);
            if (source == null || destination == null) {
                PathResult emptyResult = PathResult.empty(resolvedConfig, source, destination);
                lastResult = emptyResult;
                return emptyResult;
            }

            PreparedPathfinder prepared = ctx.runOnClientThread(() -> prepare(source, destination, resolvedConfig));
            if (prepared == null || prepared.targets.isEmpty()) {
                PathResult emptyResult = PathResult.empty(resolvedConfig, source, destination);
                lastResult = emptyResult;
                return emptyResult;
            }

            SearchState searchState = search(prepared.start, prepared.targets, prepared.pathfinderConfig);
            PathResult result = buildResult(
                    source, destination, resolvedConfig, prepared.pathfinderConfig, searchState,
                    prepared.alKharidGateFree);
            lastResult = result;
            return result;
        }
    }

    /** Clears the cached route used by overlays and callers inspecting the last result. */
    public void clearLastResult() {
        lastResult = EMPTY_RESULT;
    }

    /** Refreshes the reusable shortest-path config and packs the search inputs. */
    private PreparedPathfinder prepare(WorldPoint source, WorldPoint destination, GlobalPathfinderConfig config) {
        configAdapter.setConfig(config);
        if (reusablePathfinderConfig == null) {
            reusablePathfinderConfig = new PathfinderConfig(client, configAdapter);
        }

        reusablePathfinderConfig.bank = client.getItemContainer(InventoryID.BANK);
        PathfinderConfig pathfinderConfig = reusablePathfinderConfig;
        pathfinderConfig.refresh();
        int gateVarp = client.getVarpValue(AlKharidGate.GATE_VARP);
        boolean princeAliFinished = QuestState.FINISHED.equals(Quest.PRINCE_ALI_RESCUE.getState(client));
        AlKharidGate.stripIfUnusable(
                pathfinderConfig.getTransportsPacked(false),
                pathfinderConfig.getTransportsPacked(true),
                countCoins(),
                gateVarp,
                princeAliFinished);

        Set<Integer> targets = new HashSet<>();
        targets.add(WorldPointUtil.packWorldPoint(destination));
        pathfinderConfig.filterLocations(targets, true);

        return new PreparedPathfinder(
                WorldPointUtil.packWorldPoint(source),
                targets,
                pathfinderConfig,
                AlKharidGate.isFree(gateVarp, princeAliFinished));
    }

    /** Runs the shortest-path graph search over walkable tiles and transport edges. */
    private SearchState search(int start, Set<Integer> targets, PathfinderConfig config) {
        CollisionMap map = config.getMap();
        VisitedTiles visited = new VisitedTiles(map);
        NodeGraph graph = new NodeGraph(1 << 14);

        IntDeque boundary = new IntDeque(4096);
        IntMinHeap pending = new IntMinHeap(graph, 256);
        boolean targetInWilderness = WildernessChecker.isInWilderness(targets);

        boundary.addFirst(graph.createStart(start));

        int bestDistance = Integer.MAX_VALUE;
        long bestHeuristic = Integer.MAX_VALUE;
        long cutoffDurationMillis = config.getCalculationCutoffMillis();
        long cutoffTimeMillis = System.currentTimeMillis() + cutoffDurationMillis;
        int wildernessLevel = 31;
        int nodesChecked = 0;
        int transportsChecked = 0;
        int bestLastNode = NodeGraph.NO_NODE;
        boolean complete = false;

        while (!boundary.isEmpty() || !pending.isEmpty()) {
            int boundaryHead = boundary.peekFirst();
            int pendingHead = pending.peek();
            int node;

            if (pendingHead != NodeGraph.NO_NODE
                    && (boundaryHead == NodeGraph.NO_NODE || graph.compareCost(pendingHead) < graph.cost(boundaryHead))) {
                node = pending.poll();

                if (graph.isDelayedVisit(node)) {
                    int packed = graph.packedPosition(node);
                    boolean bank = graph.bankVisited(node);
                    if (visited.get(packed, bank)) {
                        continue;
                    }
                    visited.set(packed, bank);
                }
            } else {
                node = boundary.pollFirst();
            }

            if (node == NodeGraph.NO_NODE) {
                continue;
            }

            final boolean nodeIsTile = graph.isTile(node);
            final int nodePacked = nodeIsTile ? graph.packedPosition(node) : WorldPointUtil.UNDEFINED;

            if (nodeIsTile && wildernessLevel > 0) {
                if (wildernessLevel > 30 && !WildernessChecker.isInLevel30Wilderness(nodePacked)) {
                    wildernessLevel = 30;
                }
                if (wildernessLevel > 20 && !WildernessChecker.isInLevel20Wilderness(nodePacked)) {
                    wildernessLevel = 20;
                }
                if (wildernessLevel > 0 && !WildernessChecker.isInWilderness(nodePacked)) {
                    wildernessLevel = 0;
                }
            }

            if (nodeIsTile && targets.contains(nodePacked)) {
                bestLastNode = node;
                complete = true;
                break;
            }

            if (nodeIsTile) {
                for (int target : targets) {
                    int distance = WorldPointUtil.distanceBetween(nodePacked, target);
                    long heuristic = distance + (long) WorldPointUtil.distanceBetween(nodePacked, target, 2);
                    if (heuristic < bestHeuristic || (heuristic <= bestHeuristic && distance < bestDistance)) {
                        bestLastNode = node;
                        bestDistance = distance;
                        bestHeuristic = heuristic;
                        cutoffTimeMillis = System.currentTimeMillis() + cutoffDurationMillis;
                    }
                }
            }

            if (System.currentTimeMillis() > cutoffTimeMillis) {
                break;
            }

            PrimitiveIntList neighbors = map.getNeighbors(node, visited, config, wildernessLevel, targetInWilderness, graph);
            final int count = neighbors.size();
            for (int i = 0; i < count; i++) {
                int neighbor = neighbors.get(i);
                final boolean neighborIsTile = graph.isTile(neighbor);

                if (nodeIsTile && neighborIsTile) {
                    final int neighborPacked = graph.packedPosition(neighbor);
                    if (config.avoidWilderness(nodePacked, neighborPacked, targetInWilderness)) {
                        continue;
                    }
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

        List<PathStep> pathSteps = Collections.emptyList();
        if (bestLastNode != NodeGraph.NO_NODE) {
            pathSteps = graph.getPathSteps(bestLastNode);
        }

        boundary.clear();
        pending.clear();
        visited.clear();
        graph.release();

        return new SearchState(pathSteps, complete, nodesChecked, transportsChecked);
    }

    /** Converts the final node chain into the API-facing route result object. */
    private PathResult buildResult(
            WorldPoint source,
            WorldPoint destination,
            GlobalPathfinderConfig config,
            PathfinderConfig pathfinderConfig,
            SearchState searchState,
            boolean alKharidGateFree
    ) {
        if (searchState.pathSteps == null || searchState.pathSteps.isEmpty()) {
            return PathResult.empty(config, source, destination);
        }

        List<PathStep> pathSteps = searchState.pathSteps;
        List<WorldPoint> densePath = unpackPath(pathSteps);
        List<TransportUsage> transports = findTransportUsages(pathSteps, pathfinderConfig, alKharidGateFree);
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

    /** Matches route steps against transport edges so callers can see which hops were used. */
    private List<TransportUsage> findTransportUsages(
            List<PathStep> pathSteps,
            PathfinderConfig pathfinderConfig,
            boolean alKharidGateFree
    ) {
        if (pathSteps == null || pathSteps.size() < 2) {
            return Collections.emptyList();
        }

        List<TransportUsage> transportUsages = new ArrayList<>();
        for (int i = 1; i < pathSteps.size(); i++) {
            PathStep currentStep = pathSteps.get(i - 1);
            PathStep nextStep = pathSteps.get(i);
            int origin = currentStep.getPackedPosition();
            int destination = nextStep.getPackedPosition();
            boolean bankVisited = currentStep.isBankVisited() || nextStep.isBankVisited();

            Transport[] fromTile = pathfinderConfig.getTransportsPacked(bankVisited)
                    .getOrDefault(origin, NO_TRANSPORTS);
            Transport transport = hopBetween(
                    fromTile, pathfinderConfig.getUsableTeleports(bankVisited), origin, destination);
            if (transport == null) {
                continue;
            }

            transportUsages.add(new TransportUsage(
                    i - 1,
                    WorldPointUtil.unpackWorldPoint(origin),
                    WorldPointUtil.unpackWorldPoint(destination),
                    transport.getType(),
                    transport.getDisplayInfo(),
                    AlKharidGate.liveObjectInfo(transport, alKharidGateFree),
                    transport.isConsumable(),
                    transport.getDuration(),
                    transport
            ));
        }

        return Collections.unmodifiableList(transportUsages);
    }

    /**
     * Whether two packed tiles are a single walking step, including diagonals.
     *
     * <p>A jewellery teleport is stored with an undefined origin, so it is recovered by matching the
     * landing tile. Walking onto that landing tile later must not count as rubbing the ring.</p>
     *
     * @param from packed start tile
     * @param to packed end tile
     * @return true when the hop is on the same plane and at most one tile away
     */
    public static boolean isWalkNeighbour(int from, int to) {
        return WorldPointUtil.unpackWorldPlane(from) == WorldPointUtil.unpackWorldPlane(to)
                && WorldPointUtil.distanceBetween(from, to) <= 1;
    }

    /**
     * Picks the transport used between two consecutive path tiles.
     *
     * <p>Edges attached to the origin tile are preferred (doors, gliders, fairy rings). Player-centered
     * teleports — jewellery, tabs, spells — have an undefined origin and are only used when the hop
     * is not a walkable neighbour.</p>
     *
     * @param fromOrigin transports keyed by the path tile, may be null
     * @param playerCentered teleports usable from wherever the player stands, may be null
     * @param origin packed path tile the hop leaves
     * @param destination packed path tile the hop lands on
     * @return the matching edge, or null when the hop is ordinary walking
     */
    public static Transport hopBetween(
            Transport[] fromOrigin, Transport[] playerCentered, int origin, int destination) {
        Transport tiled = byDestination(fromOrigin, destination);
        if (tiled != null) {
            return tiled;
        }
        if (isWalkNeighbour(origin, destination)) {
            return null;
        }
        return byDestination(playerCentered, destination);
    }

    private static Transport byDestination(Transport[] transports, int destination) {
        if (transports == null) {
            return null;
        }
        for (Transport transport : transports) {
            if (transport != null && transport.getDestination() == destination) {
                return transport;
            }
        }
        return null;
    }

    /** Unpacks the shortest-path packed coordinate list into world points. */
    private List<WorldPoint> unpackPath(List<PathStep> pathSteps) {
        if (pathSteps == null || pathSteps.isEmpty()) {
            return Collections.emptyList();
        }

        List<WorldPoint> path = new ArrayList<>(pathSteps.size());
        for (PathStep pathStep : pathSteps) {
            path.add(WorldPointUtil.unpackWorldPoint(pathStep.getPackedPosition()));
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

    /** Resolves the local player's current world point on the client thread. */
    private WorldPoint resolveLocalPlayerPoint() {
        return ctx.runOnClientThread(() -> {
            Player player = client.getLocalPlayer();
            if (player == null) {
                return null;
            }

            return WorldPointUtil.unpackWorldPoint(WorldPointUtil.fromLocalInstance(client, player));
        });
    }

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

    /** Describes a single transport edge chosen in the final route. */
    @Getter
    public static final class TransportUsage {
        private final int pathIndex;
        private final WorldPoint origin;
        private final WorldPoint destination;
        private final TransportType type;
        private final String displayInfo;
        private final String objectInfo;
        private final boolean consumable;
        private final int duration;

        /**
         * The transport edge this usage was built from, carrying the requirements the planner applied.
         *
         * <p>The fields above flatten the edge for display. Anything that has to <em>execute</em> the
         * transport needs the edge itself, because that is where the item, skill, quest and varbit
         * requirements live in structured form. May be null when the usage was built without one.</p>
         *
         * <p>This exposes a shortest-path type whose shape follows the pinned dependency rather than
         * Kraken's own compatibility promise.</p>
         */
        private final Transport transport;

        /**
         * Builds a usage without the underlying edge.
         *
         * <p>Retained so that code compiled against the previous all-args constructor keeps working.</p>
         *
         * @param pathIndex index into the route where the transport is entered
         * @param origin the tile the transport is entered from
         * @param destination the tile the transport leads to
         * @param type the kind of transport
         * @param displayInfo human readable description of the transport
         * @param objectInfo what to interact with, as stored by the dataset
         * @param consumable whether using the transport consumes an item
         * @param duration how long the transport takes, in game ticks
         */
        public TransportUsage(int pathIndex, WorldPoint origin, WorldPoint destination, TransportType type,
                              String displayInfo, String objectInfo, boolean consumable, int duration) {
            this(pathIndex, origin, destination, type, displayInfo, objectInfo, consumable, duration, null);
        }

        /**
         * Builds a usage carrying the underlying transport edge.
         *
         * @param pathIndex index into the route where the transport is entered
         * @param origin the tile the transport is entered from
         * @param destination the tile the transport leads to
         * @param type the kind of transport
         * @param displayInfo human readable description of the transport
         * @param objectInfo what to interact with, as stored by the dataset
         * @param consumable whether using the transport consumes an item
         * @param duration how long the transport takes, in game ticks
         * @param transport the dataset edge, may be null
         */
        public TransportUsage(int pathIndex, WorldPoint origin, WorldPoint destination, TransportType type,
                              String displayInfo, String objectInfo, boolean consumable, int duration,
                              Transport transport) {
            this.pathIndex = pathIndex;
            this.origin = origin;
            this.destination = destination;
            this.type = type;
            this.displayInfo = displayInfo;
            this.objectInfo = objectInfo;
            this.consumable = consumable;
            this.duration = duration;
            this.transport = transport;
        }
    }

    /** Totals coins in the inventory. The Al Kharid overlay needs a live count, not a spend cap. */
    private int countCoins() {
        ItemContainer inventory = client.getItemContainer(InventoryID.INV);
        if (inventory == null) {
            return 0;
        }

        int coins = 0;
        for (Item item : inventory.getItems()) {
            if (item != null && item.getId() == ItemID.COINS) {
                coins += Math.max(item.getQuantity(), 0);
            }
        }
        return coins;
    }

    /** Bundles the packed search inputs with the refreshed shortest-path config. */
    @AllArgsConstructor
    private static final class PreparedPathfinder {
        private final int start;
        private final Set<Integer> targets;
        private final PathfinderConfig pathfinderConfig;
        private final boolean alKharidGateFree;
    }

    /** Captures the terminal search node and lightweight search metrics. */
    @AllArgsConstructor
    private static final class SearchState {
        private final List<PathStep> pathSteps;
        private final boolean complete;
        private final int nodesChecked;
        private final int transportsChecked;
    }

    /**
     * Maps the on/off flag to shortest-path's jewellery setting.
     *
     * <p>The plugin default is {@code INVENTORY_NON_CONSUMABLE} so an overlay does not burn a glory
     * charge. A walker should use what is actually carried, matching VitaLite: a Ring of wealth (1)
     * in the inventory is a Grand Exchange teleport, not a gnome glider.</p>
     *
     * @param useTeleportationItems whether item teleports are allowed
     * @return inventory including charged jewellery, or none
     */
    public static TeleportationItem teleportationItemSetting(boolean useTeleportationItems) {
        return useTeleportationItems ? TeleportationItem.INVENTORY : TeleportationItem.NONE;
    }

    private static final class MutableShortestPathConfigAdapter implements ShortestPathConfig {
        private volatile GlobalPathfinderConfig config;

        /** Wraps the Kraken config in the shortest-path config interface. */
        private MutableShortestPathConfigAdapter(GlobalPathfinderConfig config) {
            this.config = config;
        }

        /** Swaps in the config used for the next search. */
        private void setConfig(GlobalPathfinderConfig config) {
            this.config = config;
        }

        @Override
        public boolean avoidWilderness() {
            return config.isAvoidWilderness();
        }

        @Override
        public boolean includeBankPath() {
            return config.isIncludeBankPath();
        }

        @Override
        public int currencyThreshold() {
            return config.getCurrencyThreshold();
        }

        @Override
        public int calculationCutoff() {
            long cutoffMillis = Math.max(Constants.GAME_TICK_LENGTH, config.getCalculationCutoffMillis());
            return (int) Math.max(1L, Math.round(Math.ceil((double) cutoffMillis / Constants.GAME_TICK_LENGTH)));
        }

        @Override
        public boolean useAgilityShortcuts() {
            return config.isUseAgilityShortcuts();
        }

        @Override
        public boolean useGrappleShortcuts() {
            return config.isUseGrappleShortcuts();
        }

        @Override
        public boolean useBoats() {
            return config.isUseBoats();
        }

        @Override
        public boolean useCanoes() {
            return config.isUseCanoes();
        }

        @Override
        public boolean useCharterShips() {
            return config.isUseCharterShips();
        }

        @Override
        public boolean useShips() {
            return config.isUseShips();
        }

        @Override
        public boolean useFairyRings() {
            return config.isUseFairyRings();
        }

        @Override
        public boolean useGnomeGliders() {
            return config.isUseGnomeGliders();
        }

        @Override
        public boolean useHotAirBalloons() {
            return config.isUseHotAirBalloons();
        }

        @Override
        public boolean useMagicCarpets() {
            return config.isUseMagicCarpets();
        }

        @Override
        public boolean useMagicMushtrees() {
            return config.isUseMagicMushtrees();
        }

        @Override
        public boolean useMinecarts() {
            return config.isUseMinecarts();
        }

        @Override
        public boolean useQuetzals() {
            return config.isUseQuetzals();
        }

        @Override
        public boolean useSeasonalTransports() {
            return config.isUseSeasonalTransports();
        }

        @Override
        public boolean useSpiritTrees() {
            return config.isUseSpiritTrees();
        }

        @Override
        public TeleportationItem useTeleportationItems() {
            return teleportationItemSetting(config.isUseTeleportationItems());
        }

        @Override
        public boolean useTeleportationLevers() {
            return config.isUseTeleportationLevers();
        }

        @Override
        public boolean useTeleportationPortals() {
            return config.isUseTeleportationPortals();
        }

        @Override
        public boolean useTeleportationSpells() {
            return config.isUseTeleportationSpells();
        }

        @Override
        public boolean useTeleportationMinigames() {
            return config.isUseTeleportationMinigames();
        }

        @Override
        public boolean useWildernessObelisks() {
            return config.isUseWildernessObelisks();
        }

        @Override
        public boolean usePoh() {
            return config.isUseTeleportationBoxes() || config.isUseTeleportationPortalsPoh();
        }

        @Override
        public boolean useTeleportationPortalsPoh() {
            return config.isUseTeleportationPortalsPoh();
        }

        @Override
        public JewelleryBoxTier pohJewelleryBoxTier() {
            return config.isUseTeleportationBoxes() ? JewelleryBoxTier.ORNATE : JewelleryBoxTier.NONE;
        }

        @Override
        public boolean usePohMountedItems() {
            return config.isUseTeleportationBoxes();
        }

        @Override
        public void setBuiltTeleportationBoxes(String content) {
            // GlobalPathfinder provides a transient adapter, not persisted plugin config storage.
        }

        @Override
        public void setBuiltTeleportationPortalsPoh(String content) {
            // GlobalPathfinder provides a transient adapter, not persisted plugin config storage.
        }
    }
}
