package com.kraken.api.service.pathfinding;

import com.kraken.api.service.walker.transport.registry.TransportCategory;
import com.kraken.api.service.walker.transport.registry.TransportDefinition;
import net.runelite.api.coords.WorldPoint;

/**
 * One direction of a transport, in the form a search reads: packed tiles and plain numbers.
 *
 * <p>An edge is built once from a {@link TransportDefinition} and can be shared by every search,
 * since it holds nothing about the player. A bidirectional definition gives two edges.</p>
 */
final class TransportEdge {

    /** The packed tile the edge is entered from, or {@link PackedPoint#UNDEFINED} for a teleport. */
    final int origin;

    /** The packed tile the edge leads to. */
    final int destination;

    /** How long the crossing takes in game ticks, which is what it costs a search. */
    final int duration;

    /** The deepest wilderness bucket a teleport works at. Unused for an edge with an origin. */
    final int maxWildernessLevel;

    /**
     * Whether the destination is only marked visited once this edge is taken. Two kinds of
     * transport that lead to the same places, such as a quetzal and the whistle that calls one,
     * are compared on cost this way instead of the first one found winning.
     */
    final boolean delayedVisit;

    /** A cost that only decides the order such competing edges are taken in. */
    final int differentialCost;

    /** The definition this edge stands for. */
    final TransportDefinition definition;

    private TransportEdge(int origin, int destination, TransportDefinition definition) {
        TransportCategory category = definition.getCategory();
        this.origin = origin;
        this.destination = destination;
        this.duration = definition.getDuration();
        this.maxWildernessLevel = definition.getMaxWildernessLevel();
        this.delayedVisit = category.sharesDestinations();
        this.differentialCost = category.getDifferentialCost();
        this.definition = definition;
    }

    /**
     * Builds the edges of a definition: one for a teleport or a one way transport, two for a
     * bidirectional one. The forward direction comes first.
     *
     * @param definition the definition
     * @return its edges
     */
    static TransportEdge[] of(TransportDefinition definition) {
        int destination = PackedPoint.pack(definition.getDestination());
        if (definition.isUsableAnywhere()) {
            return new TransportEdge[]{new TransportEdge(PackedPoint.UNDEFINED, destination, definition)};
        }

        int origin = PackedPoint.pack(definition.getOrigin());
        TransportEdge forward = new TransportEdge(origin, destination, definition);
        return definition.isBidirectional()
                ? new TransportEdge[]{forward, new TransportEdge(destination, origin, definition)}
                : new TransportEdge[]{forward};
    }

    /**
     * Reports whether this is a teleport usable from wherever the player stands.
     *
     * @return true when the edge has no origin tile
     */
    boolean isTeleport() {
        return origin == PackedPoint.UNDEFINED;
    }

    /**
     * Reports whether the edge can be used at a wilderness level. Only teleports have a limit.
     *
     * @param wildernessLevel the wilderness bucket: 0, 20, 30 or 31
     * @return true when it can
     */
    boolean usableAtWildernessLevel(int wildernessLevel) {
        return !isTeleport() || wildernessLevel <= maxWildernessLevel;
    }

    WorldPoint originPoint() {
        return isTeleport() ? null : PackedPoint.unpack(origin);
    }

    @Override
    public String toString() {
        return "TransportEdge(" + definition.getId() + ": "
                + (isTeleport() ? "anywhere" : String.valueOf(PackedPoint.unpack(origin)))
                + " -> " + PackedPoint.unpack(destination) + ")";
    }
}
