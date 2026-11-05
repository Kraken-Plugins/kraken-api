package com.kraken.api.service.movement;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kraken.api.Context;
import com.kraken.api.core.packet.entity.MousePackets;
import com.kraken.api.core.packet.entity.MovementPackets;
import com.kraken.api.service.tile.TileService;
import com.kraken.api.service.ui.UIService;
import com.kraken.api.service.util.SleepService;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

/**
 * Moves the player by clicking the game canvas and sending movement packets. Provides single-destination
 * moves, stride-based waypoint paths and the deprecated path traversal helpers.
 */
@Slf4j
@Singleton
public class MovementService {

    @Inject
    private Context ctx;

    @Inject
    private TileService tileService;

    @Inject
    private MousePackets mousePackets;

    @Inject
    private MovementPackets movementPackets;

    /**
     * Moves the player to the specified {@literal WorldPoint} by clicking its tile and sending a movement packet.
     *
     * <p>Inside an instance the point may be given in either coordinate space: as a template point (the tile's
     * location on the normal world map, from {@link WorldPoint#fromLocalInstance}) or as a point in the loaded scene
     * (the space {@code actor.getWorldLocation()} and {@link WorldPoint#fromLocal} report). See
     * {@link #toSceneDestination(WorldPoint)} for how the point is resolved. Nothing is sent when the point is neither.</p>
     *
     * @param point The {@literal WorldPoint} representing the destination to move towards.
     */
    public void moveTo(WorldPoint point) {
        WorldPoint destination = toSceneDestination(point);
        if (destination == null) {
            log.error("Cannot move to {}: it is neither a template point of this instance nor a tile in the loaded scene", point);
            return;
        }
        sendMovement(destination);
    }

    /**
     * Moves the player to a specified {@literal LocalPoint} by clicking its tile and sending a movement packet. A local
     * point always names a tile of the loaded scene, so it is converted straight to that tile's scene coordinates,
     * inside an instance or not.
     *
     * @param point The {@literal LocalPoint} representing the target destination to move towards.
     */
    public void moveTo(LocalPoint point) {
        WorldPoint destination = toSceneDestination(point);
        if (destination == null) {
            log.error("Cannot move to local point {}", point);
            return;
        }
        sendMovement(destination);
    }

    /**
     * Resolves a destination to the coordinates the movement packet carries: those of a tile in the loaded scene.
     *
     * <p>Outside an instance the point is returned unchanged. Inside an instance a template point is converted to its
     * first occurrence on the active plane, and a point that already lies in the loaded scene is returned unchanged.
     * Template points are tried first, so callers that pass template points keep their behaviour.</p>
     *
     * @param point The destination, as a template point or a point in the loaded scene.
     * @return The scene coordinates to send, or null when the point is null, or is inside an instance neither a
     * template point of it nor a tile in the loaded scene.
     */
    public WorldPoint toSceneDestination(WorldPoint point) {
        if (point == null) {
            return null;
        }

        WorldView worldView = ctx.getClient().getTopLevelWorldView();
        if (!worldView.isInstance()) {
            return point;
        }

        LocalPoint templateOccurrence = tileService.fromWorldInstance(point);
        if (templateOccurrence != null) {
            return WorldPoint.fromLocal(ctx.getClient(), templateOccurrence);
        }

        return LocalPoint.fromWorld(worldView, point) != null ? point : null;
    }

    /**
     * Resolves a local point to the coordinates of its tile in the loaded scene, the coordinates the movement packet
     * carries.
     *
     * @param point The local point.
     * @return The scene coordinates of the tile, or null when the point is null.
     */
    public WorldPoint toSceneDestination(LocalPoint point) {
        if (point == null) {
            return null;
        }
        return WorldPoint.fromLocal(ctx.getClient(), point);
    }

    private void sendMovement(WorldPoint destination) {
        Point clickingPoint = UIService.getClickbox(destination);
        mousePackets.queueClickPacket(clickingPoint.getX(), clickingPoint.getY());
        movementPackets.queueMovement(destination);
    }

    /**
     * Converts a dense path (every tile) into a strided path of "waypoint" tiles. Tiles are based around
     * a variable stride configuration which computes a Gaussian distribution for the length of each stride to
     * take.
     * @param densePath The dense path with which to apply variable strides.
     * @return A list of {@literal WorldPoint} representing the strided path.
     */
    @Deprecated(since = "4.0.5")
    public List<WorldPoint> applyVariableStride(List<WorldPoint> densePath) {
        return applyVariableStride(densePath, VariableStrideConfig.builder().build());
    }

    /**
     * Converts a dense path (every tile) into a strided path of "waypoint" tiles. Tiles are based around
     * a variable stride configuration which computes a Gaussian distribution for the length of each stride to
     * take.
     * @param densePath The dense path with which to apply variable strides.
     * @param config A variable stride configuration where the mean, min, max, and std dev can be configured
     *               to produce unique strides when traversing paths.
     * @return A list of {@literal WorldPoint} representing the strided path. An empty list is returned when
     *         {@code densePath} is null or empty.
     */
    @Deprecated(since = "4.0.5")
    public List<WorldPoint> applyVariableStride(List<WorldPoint> densePath, VariableStrideConfig config) {
        // A null or empty path has no final tile to stride toward. Without this guard the size <= 5
        // branch below calls get(-1) and throws IndexOutOfBoundsException.
        if (densePath == null || densePath.isEmpty()) {
            return Collections.emptyList();
        }

        if (densePath.size() <= 5) {
            return Collections.singletonList(densePath.get(densePath.size() - 1));
        }

        List<WorldPoint> waypoints = new ArrayList<>();

        // Randomize the starting point slightly (0-2 tiles) to shift the whole chain
        // This ensures we don't always "base" our stride from the exact same first pixel.
        int currentIndex = ThreadLocalRandom.current().nextInt(0, 3);

        if (currentIndex >= densePath.size()) currentIndex = 0;

        while (currentIndex < densePath.size() - 1) {
            int stride = config.computeStride();
            int nextIndex = currentIndex + stride;

            if (nextIndex >= densePath.size() - 1) {
                waypoints.add(densePath.get(densePath.size() - 1));
                break;
            }

            WorldPoint targetPoint = densePath.get(nextIndex);

            if (config.isTileDeviation()) {
            int dx = 0;
            int dy = 0;

            // Loop until we get a non-zero deviation (ensures we actually move the point)
            // This covers -1, 0, 1 for both axes.
            // Example results: (-1, 1) = Diagonal, (0, 1) = Cardinal, (-1, -1) = Diagonal
            while (dx == 0 && dy == 0) {
                dx = ThreadLocalRandom.current().nextInt(-1, 2); // returns -1, 0, or 1
                dy = ThreadLocalRandom.current().nextInt(-1, 2);
            }

            WorldPoint deviatedTile = targetPoint.dx(dx).dy(dy);

            if (tileService.isTileReachable(deviatedTile)) {
                waypoints.add(deviatedTile);
            } else {
                waypoints.add(targetPoint);
            }
        } else {
            waypoints.add(targetPoint);
        }

        currentIndex = nextIndex;
    }

    return waypoints;
    }

    /** How close to a waypoint counts as reached when the caller does not say otherwise. */
    private static final int DEFAULT_ARRIVAL_TILES = 2;

    /**
     * Traverses a given path made up of waypoints, attempting to successfully move the player
     * to each {@literal WorldPoint} in the sequence. This method moves the player towards the target
     * waypoints and handles retries for unreachable points. Waypoints that are not reachable in the
     * live scene are skipped rather than waited on — a stride that lands on a ditch or river is not
     * a failed walk. The traversal stops if any remaining waypoint cannot be reached after retries.
     *
     * @param client The client instance used to interact with the game world and manage player movement.
     * @param path A list of {@literal WorldPoint} objects representing the waypoints to traverse in sequence.
     * @return {@code true} if the path was successfully traversed to the end, or {@code false} if any waypoint
     *         could not be reached after retries.
     */
    @Deprecated(since = "4.0.5, use Walker.walkTo() instead")
    public boolean traversePath(Client client, List<WorldPoint> path) {
        return traversePath(client, path, (ignored) -> {}, (ignored) -> {}, DEFAULT_ARRIVAL_TILES);
    }

    /**
     * Traverses a path, treating a waypoint as reached when the player is within {@code withinTiles}.
     *
     * @param client the game client
     * @param path the waypoints to walk, in order
     * @param withinTiles how close to a waypoint counts as arrived, in tiles
     * @return true when every remaining waypoint was reached or skipped
     */
    @Deprecated(since = "4.0.5, use Walker.walkTo() instead")
    public boolean traversePath(Client client, List<WorldPoint> path, int withinTiles) {
        return traversePath(client, path, (ignored) -> {}, (ignored) -> {}, withinTiles);
    }

    /**
     * Traverses a given path made up of waypoints, attempting to successfully move the player
     * to each {@literal WorldPoint} in the sequence.
     *
     * @param client The client instance used to interact with the game world and retrieve the player's location.
     * @param path A list of {@literal WorldPoint} objects representing the waypoints to traverse in sequence.
     * @param onWaypointReached A functional interface invoked when a waypoint is reached
     * @return {@code true} if the path was successfully traversed to the end, {@code false} if any waypoint
     *         could not be reached after retries.
     */
    @Deprecated(since = "4.0.5, use Walker.walkTo() instead")
    public boolean traversePath(Client client, List<WorldPoint> path, Consumer<String> onWaypointReached) {
        return traversePath(client, path, onWaypointReached, (ignored) -> {}, DEFAULT_ARRIVAL_TILES);
    }

    /**
     * Traverses a given path made up of waypoints, attempting to successfully move the player
     * to each {@literal WorldPoint} in the sequence.
     *
     * @param client The client instance used to interact with the game world and retrieve the player's location.
     * @param path A list of {@literal WorldPoint} objects representing the waypoints to traverse in sequence.
     * @param onWaypointReached A functional interface invoked when a waypoint is reached
     * @param onDestinationReached A functional interface invoked when the paths final destination is reached
     * @return {@code true} if the path was successfully traversed to the end, {@code false} if any waypoint
     *         could not be reached after retries.
     */
    @Deprecated(since = "4.0.5, use Walker.walkTo() instead")
    public boolean traversePath(Client client, List<WorldPoint> path, Consumer<String> onWaypointReached, Consumer<String> onDestinationReached) {
        return traversePath(client, path, onWaypointReached, onDestinationReached, DEFAULT_ARRIVAL_TILES);
    }

    /**
     * Traverses a given path made up of waypoints, attempting to successfully move the player
     * to each {@literal WorldPoint} in the sequence. This method moves the player towards the target
     * waypoints and handles retries for unreachable points. Waypoints that are not reachable in the
     * live scene are skipped rather than waited on. The last click uses {@code withinTiles} so a walk
     * whose destination tolerance is three tiles does not retry for a one-tile miss.
     *
     * @param client The client instance used to interact with the game world and retrieve the player's location.
     * @param path A list of {@literal WorldPoint} objects representing the waypoints to traverse in sequence.
     * @param onWaypointReached A functional interface invoked when a waypoint is reached
     * @param onDestinationReached A functional interface invoked when the paths final destination is reached
     * @param withinTiles how close to a waypoint counts as arrived, in tiles
     * @return {@code true} if the path was successfully traversed to the end, {@code false} if any waypoint
     *         could not be reached after retries.
     */
    @Deprecated(since = "4.0.5, use Walker.walkTo() instead")
    public boolean traversePath(Client client, List<WorldPoint> path, Consumer<String> onWaypointReached,
                                Consumer<String> onDestinationReached, int withinTiles) {
        int near = Math.max(0, withinTiles);
        for (WorldPoint step : path) {
            if (step != null && !tileService.isTileReachable(step)) {
                log.debug("Skipping unreachable waypoint {}", step);
                continue;
            }

            boolean stepSuccess = false;

            // Try to move to this waypoint up to 2 times
            for (int attempt = 0; attempt < 2; attempt++) {
                ctx.runOnClientThread(() -> moveTo(step));

                // Walking = 1 tile/0.6s. Running = 2 tiles/0.6s.
                // We assume walking speed (worst case) + 2 seconds buffer.
                WorldPoint playerLoc = ctx.runOnClientThread(() -> client.getLocalPlayer().getWorldLocation());
                int dist = playerLoc.distanceTo(step);

                // (Distance * 600ms) is exact walking time. We multiply by 2.1 for path variance, plus 2000ms base buffer.
                long timeoutDuration = (long) ((dist * 600 * 2.1) + 2000);
                long timeout = System.currentTimeMillis() + timeoutDuration;

                boolean reached = false;
                while (System.currentTimeMillis() < timeout) {
                    playerLoc = ctx.runOnClientThread(() -> client.getLocalPlayer().getWorldLocation());

                    if (playerLoc.distanceTo(step) <= near) {
                        reached = true;
                        break;
                    }
                    SleepService.sleep(50);
                }

                if (reached) {
                    stepSuccess = true;
                    onWaypointReached.accept("Reached waypoint: " + step);
                    break;
                } else {
                    log.warn("TaskChain: Timeout moving to {}, retrying... (Attempt {}/2)", step, attempt + 1);
                }
            }

            // If we failed all attempts for this specific step, abort the whole chain
            if (!stepSuccess) {
                log.error("TaskChain: Failed to reach waypoint {} after retries. Aborting.", step);
                return false;
            }
        }

        if(path.isEmpty()) {
            onDestinationReached.accept("Empty path");
            return true;
        }

        onDestinationReached.accept("Reached destination: " + path.get(path.size() - 1));
        return true;
    }
}