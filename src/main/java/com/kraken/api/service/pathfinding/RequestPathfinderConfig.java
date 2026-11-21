package com.kraken.api.service.pathfinding;

import net.runelite.api.Client;
import shortestpath.ShortestPathConfig;
import shortestpath.pathfinder.PathfinderConfig;
import shortestpath.transport.Transport;

/**
 * The planner configuration that one search owns.
 *
 * <p>The planner keeps teleports that can be used from anywhere in an array it builds from its own
 * dataset, with no way to add to it. This class lets a search carry extra ones, such as those from
 * the transport registry, by answering {@link #getUsableTeleports(boolean)} with both sets.</p>
 */
class RequestPathfinderConfig extends PathfinderConfig {

    private volatile Transport[] teleportsWithoutBank;
    private volatile Transport[] teleportsWithBank;

    /**
     * Creates the configuration for one search. This loads the planner's static resources, so it
     * belongs on a worker thread.
     *
     * @param client the game client the planner reads live state from
     * @param config the planning options of the search
     */
    RequestPathfinderConfig(Client client, ShortestPathConfig config) {
        super(client, config);
    }

    /**
     * Adds teleports usable from anywhere to this search, ahead of the dataset's own, so that a
     * lookup by destination finds an added teleport first. They are added to both bank states.
     *
     * <p>Call this after {@code refresh()}, which is what rebuilds the dataset's teleports.</p>
     *
     * @param teleports the teleports to add, may be null or empty
     */
    void addUsableTeleports(Transport[] teleports) {
        if (teleports == null || teleports.length == 0) {
            return;
        }

        teleportsWithoutBank = combine(teleports, super.getUsableTeleports(false));
        teleportsWithBank = combine(teleports, super.getUsableTeleports(true));
    }

    /**
     * Returns the teleports the search may use from any tile: the ones added to this search first,
     * then the dataset's own.
     *
     * @param bankVisited whether the route has passed a bank, which widens what the dataset offers
     * @return the teleports, which the caller must not modify
     */
    @Override
    public Transport[] getUsableTeleports(boolean bankVisited) {
        Transport[] combined = bankVisited ? teleportsWithBank : teleportsWithoutBank;
        return combined != null ? combined : super.getUsableTeleports(bankVisited);
    }

    /**
     * Joins two arrays of teleports into one, keeping their order.
     *
     * @param first the teleports that come first
     * @param second the teleports that follow, may be null
     * @return a new array holding both
     */
    static Transport[] combine(Transport[] first, Transport[] second) {
        int following = second != null ? second.length : 0;
        Transport[] combined = new Transport[first.length + following];
        System.arraycopy(first, 0, combined, 0, first.length);
        if (following > 0) {
            System.arraycopy(second, 0, combined, first.length, following);
        }
        return combined;
    }
}
