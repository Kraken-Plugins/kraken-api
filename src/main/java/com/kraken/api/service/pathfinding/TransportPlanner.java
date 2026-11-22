package com.kraken.api.service.pathfinding;

import com.kraken.api.service.magic.MagicService;
import com.kraken.api.service.walker.transport.PlayerStateReader;
import com.kraken.api.service.walker.transport.TransportRequirements;
import com.kraken.api.service.walker.transport.TransportShape;
import com.kraken.api.service.walker.transport.handler.SpellTeleportHandler;
import com.kraken.api.service.walker.transport.registry.DefinitionRequirements;
import com.kraken.api.service.walker.transport.registry.TransportCategory;
import com.kraken.api.service.walker.transport.registry.TransportDefinition;
import com.kraken.api.service.walker.transport.registry.TransportRegistry;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.WorldType;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.VarbitID;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Works out which transports one search may use and hands them to it as a {@link TransportNetwork}.
 *
 * <p>The transports come from the {@link TransportRegistry}: the dataset bundled with the API and
 * whatever has been registered on top of it. For each search the planner reads the player's state
 * once and keeps the definitions whose kind is turned on, whose requirements are met and whose
 * place the player can use right now.</p>
 *
 * <p>The dataset holds thousands of transports and never changes, so its edges and the list of what
 * its requirements ask about are worked out once and kept.</p>
 */
@Slf4j
@Singleton
class TransportPlanner {

    /** The tiles a player owned house occupies. A house is an instance, planned as one landing tile. */
    private static final int HOUSE_MIN_X = 1856;
    private static final int HOUSE_MAX_X = 2047;
    private static final int HOUSE_MIN_Y = 7040;
    private static final int HOUSE_MAX_Y = 7111;

    private final TransportRegistry registry;
    private final PlayerStateReader playerStateReader;
    private final MagicService magicService;
    private final Client client;

    private volatile CompiledDataset compiledDataset;

    /**
     * Creates the planner.
     *
     * @param registry the transports to plan through
     * @param playerStateReader reads the player state that requirements are checked against
     * @param magicService says whether a spell teleport can be cast right now
     * @param client the game client, read for the kind of world and whether the player is at sea
     */
    @Inject
    TransportPlanner(TransportRegistry registry, PlayerStateReader playerStateReader, MagicService magicService,
                     Client client) {
        this.registry = registry;
        this.playerStateReader = playerStateReader;
        this.magicService = magicService;
        this.client = client;
    }

    /**
     * Reads the dataset and the collision map from the jar when that has not happened yet. Both
     * take a moment, so a search calls this on its worker before it captures state on the client
     * thread.
     */
    void warmUp() {
        compiled(registry.snapshot().getDataset());
        WorldCollisionData.bundled();
    }

    /**
     * Builds the transports one search may use. Call this on the client thread: it reads player
     * state, and the conditions of registered definitions read client state.
     *
     * <p>Where several transports leave one tile, the registered ones come first, the latest
     * registration ahead of an earlier one, and the dataset's follow. A route step is traced to the
     * first edge that reaches its destination, so that order decides which transport is
     * operated.</p>
     *
     * @param config the planning options of the search
     * @return the network for that search
     */
    TransportNetwork plan(GlobalPathfinderConfig config) {
        TransportRegistry.Snapshot snapshot = registry.snapshot();
        List<TransportDefinition> registered = snapshot.getDefinitions();
        CompiledDataset dataset = compiled(snapshot.getDataset());

        TransportRequirements.PlayerState state = playerStateReader.read(
                dataset.needs.and(PlayerStateReader.Needs.of(snapshot.requirements())));
        Search search = new Search(config, state, snapshot, liveRules(config));

        TransportNetwork.Builder network = new TransportNetwork.Builder(config.isAvoidWilderness());
        for (int i = registered.size() - 1; i >= 0; i--) {
            TransportDefinition definition = registered.get(i);
            if (search.usable(definition)) {
                for (TransportEdge edge : TransportEdge.of(definition)) {
                    network.add(edge);
                }
            }
        }

        Set<Long> hidden = hiddenEdges(snapshot);
        for (int i = 0; i < dataset.definitions.size(); i++) {
            if (!search.usable(dataset.definitions.get(i))) {
                continue;
            }
            for (TransportEdge edge : dataset.edges[i]) {
                if (!hidden.contains(pair(edge.origin, edge.destination))) {
                    network.add(edge);
                }
            }
        }

        return network.build();
    }

    /**
     * Reads the facts about the world and the player that rule out whole groups of transports.
     */
    private LiveRules liveRules(GlobalPathfinderConfig config) {
        EnumSet<WorldType> worldTypes = client.getWorldType();
        return new LiveRules(
                client.getVarbitValue(VarbitID.SAILING_BOARDED_BOAT) != 0,
                worldTypes != null && worldTypes.contains(WorldType.SEASONAL),
                config.isUseTeleportationBoxes() || config.isUseTeleportationPortalsPoh());
    }

    /**
     * Lists the dataset edges the registry hides, as packed pairs of tiles.
     */
    private static Set<Long> hiddenEdges(TransportRegistry.Snapshot snapshot) {
        Set<Long> hidden = new HashSet<>();
        for (TransportRegistry.Edge edge : snapshot.getDisabledEdges()) {
            hidden.add(pair(PackedPoint.pack(edge.getOrigin()), PackedPoint.pack(edge.getDestination())));
        }
        return hidden;
    }

    private static long pair(int packedOrigin, int packedDestination) {
        return ((long) packedOrigin << 32) | (packedDestination & 0xFFFFFFFFL);
    }

    /**
     * Returns the edges and needs of a dataset, working them out the first time a dataset is seen.
     */
    private CompiledDataset compiled(List<TransportDefinition> dataset) {
        CompiledDataset current = compiledDataset;
        if (current == null || current.definitions != dataset) {
            current = new CompiledDataset(dataset);
            compiledDataset = current;
        }
        return current;
    }

    /**
     * Reports whether a tile lies inside a player owned house.
     *
     * @param point the tile, may be null
     * @return true when it does
     */
    static boolean insideHouse(WorldPoint point) {
        return point != null
                && point.getX() >= HOUSE_MIN_X && point.getX() <= HOUSE_MAX_X
                && point.getY() >= HOUSE_MIN_Y && point.getY() <= HOUSE_MAX_Y;
    }

    /** The edges of every dataset definition and what its requirements ask about, worked out once. */
    private static final class CompiledDataset {
        private final List<TransportDefinition> definitions;
        private final TransportEdge[][] edges;
        private final PlayerStateReader.Needs needs;

        private CompiledDataset(List<TransportDefinition> definitions) {
            this.definitions = definitions;
            this.edges = new TransportEdge[definitions.size()][];
            List<DefinitionRequirements> requirements = new ArrayList<>(definitions.size());
            for (int i = 0; i < edges.length; i++) {
                edges[i] = TransportEdge.of(definitions.get(i));
                requirements.add(definitions.get(i).getRequirements());
            }
            this.needs = PlayerStateReader.Needs.of(requirements);
        }
    }

    /** Facts about the world and the player that rule out whole groups of transports. */
    private static final class LiveRules {
        private final boolean aboardBoat;
        private final boolean seasonalWorld;
        private final boolean useHouse;

        private LiveRules(boolean aboardBoat, boolean seasonalWorld, boolean useHouse) {
            this.aboardBoat = aboardBoat;
            this.seasonalWorld = seasonalWorld;
            this.useHouse = useHouse;
        }
    }

    /** Judges definitions for one search, remembering the verdict on requirements it has seen. */
    private final class Search {
        private final GlobalPathfinderConfig config;
        private final TransportRequirements.PlayerState state;
        private final TransportRegistry.Snapshot snapshot;
        private final LiveRules rules;
        private final Map<DefinitionRequirements, Boolean> requirementsMet = new IdentityHashMap<>();
        private final Map<String, Boolean> spellCastable = new HashMap<>();

        private Search(GlobalPathfinderConfig config, TransportRequirements.PlayerState state,
                       TransportRegistry.Snapshot snapshot, LiveRules rules) {
            this.config = config;
            this.state = state;
            this.snapshot = snapshot;
            this.rules = rules;
        }

        /**
         * Reports whether the search may plan through a definition.
         *
         * <p>The cheap rules come first: the kind of transport is turned on, seasonal transports
         * need a seasonal world, a transport through a player owned house needs the house turned
         * on, and no teleport is suggested while the player is aboard a boat at sea. Then the
         * requirements are checked, many definitions sharing one verdict, then the definition's
         * own condition, and last whether the client reports a spell as castable.</p>
         */
        private boolean usable(TransportDefinition definition) {
            TransportCategory category = definition.getCategory();
            if (!category.isEnabled(config)) {
                return false;
            }
            if (category == TransportCategory.SEASONAL && !rules.seasonalWorld) {
                return false;
            }
            if (!rules.useHouse && (insideHouse(definition.getOrigin()) || insideHouse(definition.getDestination()))) {
                return false;
            }
            if (definition.isUsableAnywhere() && rules.aboardBoat) {
                return false;
            }

            Boolean met = requirementsMet.get(definition.getRequirements());
            if (met == null) {
                met = TransportRequirements.unmetReasons(
                        definition.getRequirements(), state, config.getCurrencyThreshold()).isEmpty();
                requirementsMet.put(definition.getRequirements(), met);
            }
            if (!met || !conditionHolds(definition)) {
                return false;
            }

            boolean castByBuiltInHandler = definition.getShape() == TransportShape.SPELL
                    && definition.getHandler() == null
                    && snapshot.handlerFor(TransportShape.SPELL) == null;
            return !castByBuiltInHandler || castable(definition.getDisplayInfo());
        }

        /**
         * Asks the client whether a spell can be cast, once per spell name for the search. Several
         * rows share a name, a home teleport has one per animation, and each question runs a
         * client script.
         */
        private boolean castable(String spellName) {
            return spellCastable.computeIfAbsent(String.valueOf(spellName),
                    name -> SpellTeleportHandler.castRefusal(magicService, spellName) == null);
        }

        /** A condition that throws counts as not holding, so one plugin's fault cannot stop every walk. */
        private boolean conditionHolds(TransportDefinition definition) {
            if (definition.getAvailableWhen() == null) {
                return true;
            }
            try {
                return definition.getAvailableWhen().getAsBoolean();
            } catch (RuntimeException e) {
                log.warn("Condition of transport {} threw, treating it as unavailable", definition.getId(), e);
                return false;
            }
        }
    }
}
