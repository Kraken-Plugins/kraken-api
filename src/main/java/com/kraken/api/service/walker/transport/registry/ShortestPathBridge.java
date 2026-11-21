package com.kraken.api.service.walker.transport.registry;

import com.kraken.api.service.magic.MagicService;
import com.kraken.api.service.pathfinding.GlobalPathfinderConfig;
import com.kraken.api.service.walker.transport.PackedTransports;
import com.kraken.api.service.walker.transport.PlayerStateReader;
import com.kraken.api.service.walker.transport.TransportRequirements;
import com.kraken.api.service.walker.transport.TransportShape;
import com.kraken.api.service.walker.transport.handler.SpellTeleportHandler;
import net.runelite.api.coords.WorldPoint;
import shortestpath.PrimitiveIntHashMap;
import shortestpath.WorldPointUtil;
import shortestpath.pathfinder.PathfinderConfig;
import shortestpath.transport.Transport;
import shortestpath.transport.TransportType;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
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
 * <p>A definition with an origin tile is compiled as a plain {@code TRANSPORT} edge leaving that
 * tile. A definition without one is compiled as a teleport the planner may use from any tile, up to
 * the definition's wilderness level. Neither carries requirements of its own, because the registry
 * has already decided which definitions this search may use.</p>
 */
@Singleton
public class ShortestPathBridge {

    private static final Transport[] NO_TELEPORTS = new Transport[0];

    private final TransportRegistry registry;
    private final PlayerStateReader playerStateReader;
    private final MagicService magicService;

    /**
     * Creates the bridge.
     *
     * @param registry the transports to plan through
     * @param playerStateReader reads the player state that requirements are checked against
     * @param magicService says whether a registered spell teleport can be cast right now
     */
    @Inject
    public ShortestPathBridge(TransportRegistry registry, PlayerStateReader playerStateReader,
                              MagicService magicService) {
        this.registry = registry;
        this.playerStateReader = playerStateReader;
        this.magicService = magicService;
    }

    /**
     * Applies the registry to a search's captured configuration.
     *
     * <p>Call this on the client thread after {@code refresh()} has rebuilt the configuration's
     * transports, since it reads player state and edits the maps that refresh produced. Teleports
     * usable from anywhere cannot be put into those maps; they come back in the result for the
     * caller to hand to the search.</p>
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
        List<TransportDefinition> usable = snapshot.usable(config, state);
        boolean aboardBoat = pathfinderConfig.isOnSailingBoat();
        usable.removeIf(definition -> !operableNow(definition, snapshot, aboardBoat));

        return inject(
                usable,
                snapshot.getDisabledEdges(),
                pathfinderConfig.getTransportsPacked(false),
                pathfinderConfig.getTransportsPacked(true));
    }

    /**
     * Applies the rules that depend on where the player is and what the client reports, beyond a
     * definition's stated requirements.
     *
     * <p>The planner does not suggest teleporting off a boat at sea, so teleports usable from
     * anywhere are left out while the player is aboard one. A spell teleport that the built in
     * handler would cast is left out while the client reports the spell as not castable.</p>
     */
    private boolean operableNow(TransportDefinition definition, TransportRegistry.Snapshot snapshot,
                                boolean aboardBoat) {
        if (definition.isUsableAnywhere() && aboardBoat) {
            return false;
        }

        boolean castByBuiltInHandler = definition.getShape() == TransportShape.SPELL
                && definition.getHandler() == null
                && snapshot.handlerFor(TransportShape.SPELL) == null;
        return !castByBuiltInHandler
                || SpellTeleportHandler.castRefusal(magicService, definition.getDisplayInfo()) == null;
    }

    /**
     * Removes the hidden dataset edges from a search's transport maps, adds the usable definitions
     * that have an origin tile ahead of whatever else leaves those tiles, and compiles the ones
     * usable from anywhere into teleports.
     *
     * <p>A route step is traced to the first edge from its origin that reaches its destination, so
     * when several transports join the same two tiles a registered one is operated in preference to
     * the dataset's, and the latest registration in preference to an earlier one. Teleports are
     * returned latest registration first for the same reason.</p>
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
        List<Transport> teleports = new ArrayList<>();
        for (TransportDefinition definition : usable) {
            if (definition.isUsableAnywhere()) {
                Transport teleport = compileTeleport(definition);
                teleports.add(0, teleport);
                definitionsByEdge.put(teleport, definition);
                continue;
            }

            add(definition, definition.getOrigin(), definition.getDestination(), withoutBank, withBank, sharedMap,
                    definitionsByEdge);
            if (definition.isBidirectional()) {
                add(definition, definition.getDestination(), definition.getOrigin(), withoutBank, withBank, sharedMap,
                        definitionsByEdge);
            }
        }

        return definitionsByEdge.isEmpty()
                ? Injection.NONE
                : new Injection(definitionsByEdge, teleports.toArray(NO_TELEPORTS));
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
     * Builds the shortest-path edge for one direction of a definition that has an origin tile.
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
     * Builds the shortest-path teleport for a definition usable from anywhere.
     *
     * <p>The edge has no origin and a teleport type, which is what makes the planner consider it
     * from every tile and stop considering it above the definition's wilderness level. Which
     * teleport type is used only decides the planner's bookkeeping, so a spell is filed as a spell
     * and everything else as an item.</p>
     *
     * @param definition the definition the teleport stands for
     * @return the teleport
     */
    static Transport compileTeleport(TransportDefinition definition) {
        TransportType type = definition.getShape() == TransportShape.SPELL
                ? TransportType.TELEPORTATION_SPELL
                : TransportType.TELEPORTATION_ITEM;
        return new Transport.TransportBuilder()
                .type(type)
                .destination(WorldPointUtil.packWorldPoint(definition.getDestination()))
                .duration(definition.getDuration())
                .displayInfo(definition.getDisplayInfo())
                .objectInfo(definition.getObjectInfo())
                .maxWildernessLevel(definition.getMaxWildernessLevel())
                .build();
    }

    /**
     * Records which edges of one search came from the registry, so a route step can be traced back
     * to the definition that put it there.
     */
    public static final class Injection {

        /** A search the registry added nothing to. */
        public static final Injection NONE = new Injection(Map.of(), NO_TELEPORTS);

        private final Map<Transport, TransportDefinition> definitionsByEdge;
        private final Transport[] teleports;

        private Injection(Map<Transport, TransportDefinition> definitionsByEdge, Transport[] teleports) {
            this.definitionsByEdge = definitionsByEdge;
            this.teleports = teleports;
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

        /**
         * Returns the registered teleports usable from anywhere, for the search to consider beside
         * the dataset's own. The latest registration comes first.
         *
         * @return a copy of the teleports, empty when the registry has none for this search
         */
        public Transport[] getTeleports() {
            return teleports.clone();
        }
    }
}
