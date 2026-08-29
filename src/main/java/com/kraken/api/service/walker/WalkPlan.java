package com.kraken.api.service.walker;

import com.kraken.api.service.pathfinding.GlobalPathfinder;
import net.runelite.api.coords.WorldPoint;

import java.util.Collections;
import java.util.List;

/**
 * The route arithmetic the walk loop needs, kept pure so it can be tested without a client.
 */
public final class WalkPlan {

    private WalkPlan() {
    }

    /**
     * Finds the transport the player will reach first.
     *
     * <p>The planner does not promise the transports come back in route order, so this picks by path
     * index rather than trusting the list's ordering.</p>
     *
     * @param transports the transports the route uses, may be null
     * @return the earliest transport, or null when the route uses none
     */
    public static GlobalPathfinder.TransportUsage firstTransport(List<GlobalPathfinder.TransportUsage> transports) {
        if (transports == null || transports.isEmpty()) {
            return null;
        }

        GlobalPathfinder.TransportUsage earliest = null;
        for (GlobalPathfinder.TransportUsage usage : transports) {
            if (usage == null) {
                continue;
            }
            if (earliest == null || usage.getPathIndex() < earliest.getPathIndex()) {
                earliest = usage;
            }
        }

        return earliest;
    }

    /**
     * Takes the part of a route that leads up to a transport, stopping on the tile before the
     * entrance.
     *
     * <p>The walker operates a transport from beside it ({@code transportFireRadius} is two tiles).
     * Walking onto the origin is what timed out on the wilderness ditch: that tile is the object, and
     * Walk-here never puts the player on it. Anything past the origin belongs to the other side of
     * the crossing and must not be walked.</p>
     *
     * @param path the dense route, ordered from the player outwards; may be null
     * @param transportIndex the index at which the transport is entered
     * @return the walkable prefix, possibly empty when the player is already on the origin
     */
    public static List<WorldPoint> approach(List<WorldPoint> path, int transportIndex) {
        if (path == null || path.isEmpty() || transportIndex < 0) {
            return Collections.emptyList();
        }

        int end = Math.min(transportIndex, path.size());
        return path.subList(0, end);
    }

    /**
     * How many tiles of walking one plane of separation is worth, for stall detection.
     *
     * <p>Large enough that a single climb outweighs the default {@code minProgressTiles}, so a
     * staircase round registers as progress even though the player's x and y barely move.</p>
     */
    public static final int PLANE_CHANGE_TILES = 10;

    /**
     * How far the player still has to go, for stall detection.
     *
     * <p>{@link WorldPoint#distanceTo(WorldPoint)} is {@link Integer#MAX_VALUE} across planes, which
     * would make every round toward an upstairs bank look like progress. This is Chebyshev in x and y
     * plus {@link #PLANE_CHANGE_TILES} per plane of separation: walking the ground toward the castle
     * still counts, a climb toward an upstairs destination counts, and a climb away from it counts
     * as losing ground rather than standing still.</p>
     *
     * @param from where the player is, may be null
     * @param to where they are heading, may be null
     * @return the plane-weighted 2D distance in tiles, or {@link Integer#MAX_VALUE} when either
     *         point is missing
     */
    public static int progressDistance(WorldPoint from, WorldPoint to) {
        if (from == null || to == null) {
            return Integer.MAX_VALUE;
        }

        return from.distanceTo2D(to) + PLANE_CHANGE_TILES * Math.abs(from.getPlane() - to.getPlane());
    }

    /**
     * A tile on the way from {@code from} to {@code to}, at most {@code maxStep} tiles out.
     *
     * <p>Used when the next path waypoint is unreachable but far away — a compressed route that
     * jumps a river — so the walker can click a closer tile rather than looking for a door on the
     * far bank. The aim itself is never returned: that tile is already known to be unreachable.</p>
     *
     * @param from where the player is standing, may be null
     * @param to the unreachable waypoint they are heading toward, may be null
     * @param maxStep the furthest the step may be, in Chebyshev tiles
     * @return a tile strictly between the two, on {@code from}'s plane, or null when they are
     *         already adjacent or an argument is missing
     */
    public static WorldPoint closerTile(WorldPoint from, WorldPoint to, int maxStep) {
        if (from == null || to == null || maxStep < 1) {
            return null;
        }

        int dx = to.getX() - from.getX();
        int dy = to.getY() - from.getY();
        int dist = Math.max(Math.abs(dx), Math.abs(dy));
        if (dist <= 1) {
            return null;
        }

        int step = Math.min(maxStep, dist - 1);
        WorldPoint closer = new WorldPoint(
                from.getX() + dx * step / dist,
                from.getY() + dy * step / dist,
                from.getPlane());
        return closer.equals(from) ? null : closer;
    }

    /**
     * Decides whether a round made enough headway to count as progress.
     *
     * @param previousDistance distance to the destination at the start of the round
     * @param currentDistance distance to the destination now
     * @param minProgressTiles the number of tiles that must be gained
     * @return true when the round made progress
     */
    public static boolean madeProgress(int previousDistance, int currentDistance, int minProgressTiles) {
        if (previousDistance == Integer.MAX_VALUE) {
            return true;
        }

        return previousDistance - currentDistance >= minProgressTiles;
    }

    /**
     * Whether the walker should follow this route.
     *
     * <p>A complete path is always followed, including when the next tile is outside the loaded
     * scene. An incomplete path is not: that is the closest land the search found, and walking it
     * cannot create a crossing that is not in the graph. The exception is an incomplete stub whose
     * last tile is already within walk tolerance of the destination — arriving there is success.</p>
     *
     * @param complete whether the planner reached the destination
     * @param path the dense route, may be null
     * @param destination where the walk is headed, may be null
     * @param tolerance how close counts as arrived, in tiles
     * @return true when {@link Walker} should walk this path
     */
    public static boolean isFollowable(boolean complete,
                                       List<WorldPoint> path,
                                       WorldPoint destination,
                                       int tolerance) {
        if (path == null || path.isEmpty()) {
            return false;
        }

        if (complete) {
            return true;
        }

        WorldPoint last = path.get(path.size() - 1);
        return progressDistance(last, destination) <= Math.max(0, tolerance);
    }

    /**
     * A readable reason for {@link WalkOutcome#NO_ROUTE}, naming the closest tile when the search
     * stopped short.
     *
     * @param here where the player is, may be null
     * @param destination where they wanted to go, may be null
     * @param path the incomplete route, may be null
     * @return a reason string
     */
    public static String noRouteReason(WorldPoint here, WorldPoint destination, List<WorldPoint> path) {
        if (path == null || path.isEmpty()) {
            return "no route from " + here + " to " + destination;
        }

        WorldPoint last = path.get(path.size() - 1);
        int remaining = progressDistance(last, destination);
        return "no complete route from " + here + " to " + destination
                + " (closest approach " + last + ", " + remaining + " tiles short)";
    }
}
