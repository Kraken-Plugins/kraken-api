package com.kraken.api.service.walker.transport.registry;

import com.kraken.api.service.pathfinding.GlobalPathfinderConfig;
import com.kraken.api.service.walker.transport.TransportHandler;
import com.kraken.api.service.walker.transport.TransportRequirements;
import com.kraken.api.service.walker.transport.TransportShape;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.coords.WorldPoint;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * The client wide set of transports and transport handlers the walker plans with.
 *
 * <p>It holds two things. The dataset is the transports of the game world that ship with the API,
 * read from {@link TransportDataset}; it is fixed. On top of it sit the registered definitions:
 * the hand written rows bundled with the API and whatever plugins add while the client runs.</p>
 *
 * <p>There is one registry per client, shared by every plugin, so a transport registered by one
 * plugin is planned through and operated by every walk from the next search on. Each registration
 * returns a handle; close it in the plugin's {@code shutDown()} so the registry does not keep the
 * plugin's classes alive.</p>
 *
 * <p>Every change builds a new immutable {@link Snapshot}. A search takes one snapshot when it
 * captures state, so a registration made while a search is running cannot change that search.</p>
 */
@Slf4j
@Singleton
public class TransportRegistry {

    private final Map<String, TransportDefinition> definitions = new LinkedHashMap<>();
    private final Map<TransportShape, List<TransportHandler>> shapeHandlers = new EnumMap<>(TransportShape.class);
    private final List<Edge> disabledEdges = new ArrayList<>();
    private final Supplier<List<TransportDefinition>> dataset;

    private volatile Snapshot snapshot = Snapshot.EMPTY;

    /**
     * Creates the registry holding the dataset and the hand written transports bundled with the
     * API. The dataset is read the first time a search asks for it.
     */
    @Inject
    public TransportRegistry() {
        this(BundledTransports.load(), TransportDataset::bundled);
    }

    /**
     * Creates a registry holding a given set of registered definitions and no dataset.
     *
     * @param initial the definitions to start with
     */
    TransportRegistry(Collection<TransportDefinition> initial) {
        this(initial, List::of);
    }

    /**
     * Creates a registry holding a given set of registered definitions over a given dataset.
     *
     * @param initial the definitions to start with
     * @param dataset supplies the fixed transports underneath them, and is asked each time a
     *                snapshot is read, so it should hand back the same list every time
     */
    TransportRegistry(Collection<TransportDefinition> initial, Supplier<List<TransportDefinition>> dataset) {
        this.dataset = dataset;
        synchronized (this) {
            for (TransportDefinition definition : initial) {
                definitions.put(definition.getId(), definition);
            }
            publish();
        }
    }

    /**
     * Adds a transport, replacing any definition already registered under the same id. The replaced
     * definition is gone for good: it does not come back when the new one is removed.
     *
     * @param definition the transport to add
     * @return a handle that removes this definition when closed
     */
    public synchronized Registration register(TransportDefinition definition) {
        if (definition == null) {
            throw new IllegalArgumentException("definition must not be null");
        }

        TransportDefinition previous = definitions.put(definition.getId(), definition);
        if (previous != null && previous != definition) {
            log.info("Transport {} was replaced by a new registration", definition.getId());
        }
        publish();

        return once(() -> removeIfCurrent(definition));
    }

    /**
     * Removes the transport registered under an id.
     *
     * @param id the id it was registered under
     * @return true when a definition was removed
     */
    public synchronized boolean unregister(String id) {
        if (id == null || definitions.remove(id) == null) {
            return false;
        }
        publish();
        return true;
    }

    /**
     * Removes every transport that belongs to an owner, as {@link TransportDefinition#getOwner()}
     * reports it: the part of the id before the first colon.
     *
     * @param owner the owner whose transports to remove
     * @return how many definitions were removed
     */
    public synchronized int unregisterAll(String owner) {
        if (owner == null) {
            return 0;
        }

        int before = definitions.size();
        definitions.values().removeIf(definition -> owner.equals(definition.getOwner()));
        int removed = before - definitions.size();
        if (removed > 0) {
            publish();
        }
        return removed;
    }

    /**
     * Supplies the handler for a whole shape of transport, in place of the built in one. The latest
     * registration for a shape is the one used.
     *
     * @param shape the shape to handle
     * @param handler the handler that operates transports of that shape
     * @return a handle that withdraws this handler when closed, restoring the one before it
     */
    public synchronized Registration registerHandler(TransportShape shape, TransportHandler handler) {
        if (shape == null || handler == null) {
            throw new IllegalArgumentException("shape and handler must not be null");
        }

        shapeHandlers.computeIfAbsent(shape, key -> new ArrayList<>()).add(handler);
        publish();

        return once(() -> removeHandler(shape, handler));
    }

    /**
     * Hides the transports of the bundled dataset from one tile to another from every search.
     * Registered definitions between the same tiles are not affected.
     *
     * @param origin the tile the unwanted transport starts on
     * @param destination the tile it leads to
     * @return a handle that brings the transports back when closed
     */
    public synchronized Registration disableEdge(WorldPoint origin, WorldPoint destination) {
        if (origin == null || destination == null) {
            throw new IllegalArgumentException("origin and destination must not be null");
        }

        Edge edge = new Edge(origin, destination);
        disabledEdges.add(edge);
        publish();

        return once(() -> removeDisabledEdge(edge));
    }

    /**
     * Returns the registry's current state.
     *
     * @return an immutable snapshot
     */
    public Snapshot snapshot() {
        return snapshot;
    }

    /**
     * Wraps an undo step so that closing a handle twice undoes it once.
     */
    private static Registration once(Runnable undo) {
        AtomicBoolean closed = new AtomicBoolean();
        return () -> {
            if (closed.compareAndSet(false, true)) {
                undo.run();
            }
        };
    }

    private synchronized void removeIfCurrent(TransportDefinition definition) {
        // A handle only removes its own definition, so closing a stale handle leaves a replacement
        // registered under the same id in place.
        if (definitions.remove(definition.getId(), definition)) {
            publish();
        }
    }

    private synchronized void removeHandler(TransportShape shape, TransportHandler handler) {
        List<TransportHandler> handlers = shapeHandlers.get(shape);
        if (handlers != null && handlers.remove(handler)) {
            publish();
        }
    }

    private synchronized void removeDisabledEdge(Edge edge) {
        // Removes one occurrence, so an edge two plugins disabled stays hidden until both let go.
        if (disabledEdges.remove(edge)) {
            publish();
        }
    }

    /**
     * Rebuilds the immutable snapshot from the mutable state. Callers hold the registry's monitor.
     */
    private void publish() {
        Set<Edge> disabled = new LinkedHashSet<>(disabledEdges);
        for (TransportDefinition definition : definitions.values()) {
            if (!definition.isReplaces()) {
                continue;
            }
            disabled.add(new Edge(definition.getOrigin(), definition.getDestination()));
            if (definition.isBidirectional()) {
                disabled.add(new Edge(definition.getDestination(), definition.getOrigin()));
            }
        }

        Map<TransportShape, TransportHandler> handlers = new EnumMap<>(TransportShape.class);
        for (Map.Entry<TransportShape, List<TransportHandler>> entry : shapeHandlers.entrySet()) {
            List<TransportHandler> registered = entry.getValue();
            if (!registered.isEmpty()) {
                handlers.put(entry.getKey(), registered.get(registered.size() - 1));
            }
        }

        snapshot = new Snapshot(
                List.copyOf(definitions.values()),
                Collections.unmodifiableSet(disabled),
                Collections.unmodifiableMap(handlers),
                dataset);
    }

    /**
     * Undoes one registration.
     */
    public interface Registration extends AutoCloseable {

        /**
         * Withdraws what was registered. Closing more than once does nothing further.
         */
        @Override
        void close();
    }

    /**
     * A directed step from one tile to another.
     */
    @Value
    public static class Edge {

        /** The tile the step starts on. */
        WorldPoint origin;

        /** The tile the step leads to. */
        WorldPoint destination;
    }

    /**
     * The registry's state at one moment. Immutable, so it is safe to read from any thread.
     */
    @Getter
    public static final class Snapshot {

        static final Snapshot EMPTY = new Snapshot(List.of(), Set.of(), Map.of(), List::of);

        /**
         * Every registered transport, in registration order: the hand written bundled rows first,
         * then what plugins added. The dataset is not part of this list, see {@link #getDataset()}.
         */
        private final List<TransportDefinition> definitions;

        /**
         * The dataset edges hidden from searches: those disabled directly, and those a definition
         * marked {@code replaces} takes over.
         */
        private final Set<Edge> disabledEdges;

        @Getter(AccessLevel.NONE)
        private final Map<TransportShape, TransportHandler> shapeHandlers;

        @Getter(AccessLevel.NONE)
        private final Supplier<List<TransportDefinition>> dataset;

        private Snapshot(List<TransportDefinition> definitions,
                         Set<Edge> disabledEdges,
                         Map<TransportShape, TransportHandler> shapeHandlers,
                         Supplier<List<TransportDefinition>> dataset) {
            this.definitions = definitions;
            this.disabledEdges = disabledEdges;
            this.shapeHandlers = shapeHandlers;
            this.dataset = dataset;
        }

        /**
         * Returns the transports of the dataset bundled with the API, which sit underneath the
         * registered ones. There are thousands, and the first call reads them from the jar, so
         * call this off the client thread.
         *
         * @return the dataset's transports, which never change while the client runs
         */
        public List<TransportDefinition> getDataset() {
            return dataset.get();
        }

        /**
         * Reports whether a definition is still part of what the walker plans with: registered
         * right now, or a transport of the dataset.
         *
         * @param definition the definition to look for, may be null
         * @return true when a walk may still operate it
         */
        public boolean holds(TransportDefinition definition) {
            return definition != null && (definitions.contains(definition) || dataset.get().contains(definition));
        }

        /**
         * Returns the handler registered for a shape.
         *
         * @param shape the shape of transport, may be null
         * @return the registered handler, or null when the built in handler applies
         */
        public TransportHandler handlerFor(TransportShape shape) {
            return shape != null ? shapeHandlers.get(shape) : null;
        }

        /**
         * Lists the requirements of every registered transport, so the player state they need can be
         * read once. The dataset's are not included.
         *
         * @return one entry per registered definition
         */
        public List<DefinitionRequirements> requirements() {
            List<DefinitionRequirements> requirements = new ArrayList<>(definitions.size());
            for (TransportDefinition definition : definitions) {
                requirements.add(definition.getRequirements());
            }
            return requirements;
        }

        /**
         * Picks the transports a search may plan through.
         *
         * <p>A definition is usable when its category is turned on, its requirements are met and its
         * own condition holds. A condition that throws counts as not holding, so one plugin's fault
         * cannot stop every walk. Call this on the client thread: conditions read client state.</p>
         *
         * @param config the planning options of the search, or null for the defaults
         * @param state the player state the requirements are checked against
         * @return the usable registered definitions, in registration order
         */
        public List<TransportDefinition> usable(GlobalPathfinderConfig config, TransportRequirements.PlayerState state) {
            List<TransportDefinition> usable = new ArrayList<>(definitions.size());
            for (TransportDefinition definition : definitions) {
                if (isUsable(definition, config, state)) {
                    usable.add(definition);
                }
            }
            return usable;
        }

        /**
         * Reports whether a search may plan through one definition: its category is turned on, its
         * requirements are met without spending more of a currency than the search allows, and its
         * own condition holds.
         *
         * @param definition the definition to judge
         * @param config the planning options of the search, or null for the defaults
         * @param state the player state the requirements are checked against
         * @return true when the definition is usable
         */
        public static boolean isUsable(TransportDefinition definition, GlobalPathfinderConfig config,
                                       TransportRequirements.PlayerState state) {
            int currencyThreshold = config != null ? config.getCurrencyThreshold() : Integer.MAX_VALUE;
            return definition.getCategory().isEnabled(config)
                    && TransportRequirements.unmetReasons(definition.getRequirements(), state, currencyThreshold).isEmpty()
                    && conditionHolds(definition);
        }

        private static boolean conditionHolds(TransportDefinition definition) {
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
