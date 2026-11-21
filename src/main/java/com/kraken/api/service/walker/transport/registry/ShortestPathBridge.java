package com.kraken.api.service.walker.transport.registry;

import com.kraken.api.service.pathfinding.GlobalPathfinderConfig;
import com.kraken.api.service.walker.transport.PackedTransports;
import com.kraken.api.service.walker.transport.PlayerStateReader;
import com.kraken.api.service.walker.transport.TransportRequirements;
import net.runelite.api.coords.WorldPoint;
import shortestpath.PrimitiveIntHashMap;
import shortestpath.WorldPointUtil;
import shortestpath.pathfinder.PathfinderConfig;
import shortestpath.transport.Transport;
import shortestpath.transport.TransportType;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Puts the registry's transports into one shortest-path search.
 *
 * <p>This is the only class in the registry package that knows the planner is shortest-path.
 * Definitions, requirements and the registry itself are Kraken types, so replacing the planner
 * means replacing this class and nothing that plugins compile against.</p>
 *
 * <p>Every edge is compiled as a plain {@code TRANSPORT} with no requirements of its own, because
 * the registry has already decided which definitions this search may use.</p>
 */
@Singleton
public class ShortestPathBridge {

    private final TransportRegistry registry;
    private final PlayerStateReader playerStateReader;

    /**
     * Creates the bridge.
     *
     * @param registry the transports to plan through
     * @param playerStateReader reads the player state that requirements are checked against
     */
    @Inject
    public ShortestPathBridge(TransportRegistry registry, PlayerStateReader playerStateReader) {
        this.registry = registry;
        this.playerStateReader = playerStateReader;
    }

    /**
     * Applies the registry to a search's captured configuration.
     *
     * <p>Call this on the client thread after {@code refresh()} has rebuilt the configuration's
     * transports, since it reads player state and edits the maps that refresh produced.</p>
     *
     * @param config the planning options of the search
     * @param pathfinderConfig the search's own captured configuration
     * @return which edges of this search came from the registry
     */
    public Injection apply(GlobalPathfinderConfig config, PathfinderConfig pathfinderConfig) {
        TransportRegistry.Snapshot snapshot = registry.snapshot();
        if (snapshot.changesNoSearch()) {
            return Injection.NONE;
        }

        TransportRequirements.PlayerState state = playerStateReader.read(snapshot.requirements());
        return inject(
                snapshot.usable(config, state),
                snapshot.getDisabledEdges(),
                pathfinderConfig.getTransportsPacked(false),
                pathfinderConfig.getTransportsPacked(true));
    }

    /**
     * Removes the hidden dataset edges from a search's transport maps and adds the usable
     * definitions ahead of whatever else leaves their origin tiles.
     *
     * <p>A route step is traced to the first edge from its origin that reaches its destination, so
     * when several transports join the same two tiles a registered one is operated in preference to
     * the dataset's, and the latest registration in preference to an earlier one.</p>
     *
     * <p>The library also files a transport that starts inside a player owned house under the house
     * landing tile. A hidden edge is removed from its own origin tile only.</p>
     *
     * @param usable the definitions this search may plan through
     * @param disabledEdges the dataset edges to hide
     * @param withoutBank the search's transports before a bank visit
     * @param withBank the search's transports after a bank visit
     * @return which edges of this search came from the registry
     */
    static Injection inject(List<TransportDefinition> usable,
                                   Collection<TransportRegistry.Edge> disabledEdges,
                                   PrimitiveIntHashMap<Transport[]> withoutBank,
                                   PrimitiveIntHashMap<Transport[]> withBank) {
        // A configuration may hand back one map for both bank states. Editing it twice would add
        // every edge twice.
        boolean sharedMap = withoutBank == withBank;

        for (TransportRegistry.Edge edge : disabledEdges) {
            int origin = WorldPointUtil.packWorldPoint(edge.getOrigin());
            int destination = WorldPointUtil.packWorldPoint(edge.getDestination());
            PackedTransports.remove(withoutBank, origin, transport -> transport.getDestination() == destination);
            if (!sharedMap) {
                PackedTransports.remove(withBank, origin, transport -> transport.getDestination() == destination);
            }
        }

        Map<Transport, TransportDefinition> definitionsByEdge = new IdentityHashMap<>();
        for (TransportDefinition definition : usable) {
            add(definition, definition.getOrigin(), definition.getDestination(), withoutBank, withBank, sharedMap,
                    definitionsByEdge);
            if (definition.isBidirectional()) {
                add(definition, definition.getDestination(), definition.getOrigin(), withoutBank, withBank, sharedMap,
                        definitionsByEdge);
            }
        }

        return definitionsByEdge.isEmpty() ? Injection.NONE : new Injection(definitionsByEdge);
    }

    private static void add(TransportDefinition definition,
                            WorldPoint origin,
                            WorldPoint destination,
                            PrimitiveIntHashMap<Transport[]> withoutBank,
                            PrimitiveIntHashMap<Transport[]> withBank,
                            boolean sharedMap,
                            Map<Transport, TransportDefinition> definitionsByEdge) {
        Transport edge = compile(definition, origin, destination);
        PackedTransports.prepend(withoutBank, edge);
        if (!sharedMap) {
            PackedTransports.prepend(withBank, edge);
        }
        definitionsByEdge.put(edge, definition);
    }

    /**
     * Builds the shortest-path edge for one direction of a definition.
     *
     * @param definition the definition the edge stands for
     * @param origin the tile this direction starts on
     * @param destination the tile this direction leads to
     * @return the edge
     */
    static Transport compile(TransportDefinition definition, WorldPoint origin, WorldPoint destination) {
        return new Transport.TransportBuilder()
                .type(TransportType.TRANSPORT)
                .origin(WorldPointUtil.packWorldPoint(origin))
                .destination(WorldPointUtil.packWorldPoint(destination))
                .duration(definition.getDuration())
                .displayInfo(definition.getDisplayInfo())
                .objectInfo(definition.getObjectInfo())
                .build();
    }

    /**
     * Records which edges of one search came from the registry, so a route step can be traced back
     * to the definition that put it there.
     */
    public static final class Injection {

        /** A search the registry added nothing to. */
        public static final Injection NONE = new Injection(Map.of());

        private final Map<Transport, TransportDefinition> definitionsByEdge;

        private Injection(Map<Transport, TransportDefinition> definitionsByEdge) {
            this.definitionsByEdge = definitionsByEdge;
        }

        /**
         * Finds the definition behind an edge. Edges are matched by identity, since the planner
         * hands back the same instance that was added.
         *
         * @param edge an edge taken from the search's route, may be null
         * @return the definition, or null when the edge came from the dataset
         */
        public TransportDefinition definitionOf(Transport edge) {
            return edge != null ? definitionsByEdge.get(edge) : null;
        }
    }
}
