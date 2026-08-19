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
     * Takes the part of a route that leads up to a transport's entrance.
     *
     * <p>The entrance tile is included, because the player has to stand on or beside it before the
     * transport can be operated. Anything past it belongs to the other side of the crossing and must
     * not be walked — those tiles may be a continent away.</p>
     *
     * @param path the dense route, ordered from the player outwards; may be null
     * @param transportIndex the index at which the transport is entered
     * @return the walkable prefix, possibly empty
     */
    public static List<WorldPoint> approach(List<WorldPoint> path, int transportIndex) {
        if (path == null || path.isEmpty() || transportIndex < 0) {
            return Collections.emptyList();
        }

        int end = Math.min(transportIndex + 1, path.size());
        return path.subList(0, end);
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
}
