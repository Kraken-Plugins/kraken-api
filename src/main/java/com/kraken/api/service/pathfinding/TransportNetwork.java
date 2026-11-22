package com.kraken.api.service.pathfinding;

import com.kraken.api.service.walker.transport.WildernessLevels;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The transports one search may use, already filtered for the player and the planning options.
 *
 * <p>A network belongs to one search and never changes once built, so a registration or a change
 * in what the player carries cannot reach a search that is running.</p>
 */
final class TransportNetwork {

    static final TransportEdge[] NO_EDGES = new TransportEdge[0];

    /** A network with no transports, in which a search can only walk. */
    static final TransportNetwork EMPTY = new Builder(false).build();

    private final IntObjectMap<TransportEdge[]> byOrigin;
    private final TransportEdge[] teleports;
    private final boolean avoidWilderness;

    private TransportNetwork(IntObjectMap<TransportEdge[]> byOrigin, TransportEdge[] teleports, boolean avoidWilderness) {
        this.byOrigin = byOrigin;
        this.teleports = teleports;
        this.avoidWilderness = avoidWilderness;
    }

    /**
     * Returns the edges that leave a tile, in the order they should be preferred.
     *
     * @param packedOrigin the packed tile
     * @return the edges, empty when no transport starts there
     */
    TransportEdge[] from(int packedOrigin) {
        return byOrigin.getOrDefault(packedOrigin, NO_EDGES);
    }

    /**
     * Returns the teleports usable from anywhere, in the order they should be preferred.
     *
     * @return the teleports, which the caller must not modify
     */
    TransportEdge[] teleports() {
        return teleports;
    }

    /**
     * Reports whether a step should be refused because it would lead into the wilderness.
     *
     * @param packedFrom the packed tile the step leaves
     * @param packedTo the packed tile the step leads to
     * @param targetInWilderness whether the search is headed into the wilderness anyway
     * @return true when the search avoids the wilderness and the step would enter it
     */
    boolean avoidWilderness(int packedFrom, int packedTo, boolean targetInWilderness) {
        return avoidWilderness
                && !targetInWilderness
                && !WildernessLevels.isInWilderness(packedFrom)
                && WildernessLevels.isInWilderness(packedTo);
    }

    /**
     * Collects the edges of a network. Edges are kept in the order they are added, which is the
     * order a route step is matched against them.
     */
    static final class Builder {
        private final boolean avoidWilderness;
        private final Map<Integer, List<TransportEdge>> byOrigin = new HashMap<>();
        private final List<TransportEdge> teleports = new ArrayList<>();

        Builder(boolean avoidWilderness) {
            this.avoidWilderness = avoidWilderness;
        }

        /**
         * Adds an edge after the ones already added for its origin.
         *
         * @param edge the edge
         */
        void add(TransportEdge edge) {
            if (edge.isTeleport()) {
                teleports.add(edge);
            } else {
                byOrigin.computeIfAbsent(edge.origin, origin -> new ArrayList<>(2)).add(edge);
            }
        }

        TransportNetwork build() {
            IntObjectMap<TransportEdge[]> packed = new IntObjectMap<>(Math.max(16, byOrigin.size()));
            for (Map.Entry<Integer, List<TransportEdge>> entry : byOrigin.entrySet()) {
                packed.put(entry.getKey(), entry.getValue().toArray(NO_EDGES));
            }
            return new TransportNetwork(packed, teleports.toArray(NO_EDGES), avoidWilderness);
        }
    }
}
