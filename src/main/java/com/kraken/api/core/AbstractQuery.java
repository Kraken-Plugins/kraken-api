package com.kraken.api.core;

import com.kraken.api.Context;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Forms the base class for all game client queries. This class defines generic actions which can be taken
 * on streams of game objects like NPC's, Ground Items, Tile Objects, Players and Widgets.
 *
 * <h3>Threading</h3>
 * <p>Every terminal operation funnels through a single evaluation that runs entirely on the client
 * thread and returns a materialised snapshot. Nothing lazy escapes: the {@link Stream} handed back by
 * {@link #stream()} iterates that snapshot, not the live scene, so filters and downstream operations
 * never touch client state from a script thread.</p>
 *
 * <h3>Failure</h3>
 * <p>Collection-valued results are never {@code null}, and single-valued terminals ({@link #first()},
 * {@link #firstMatching(Predicate)}, {@link #random()}) return {@link Optional} — the query layer never
 * hands back a bare {@code null}. If the client thread cannot answer — it is blocked, or the client is
 * loading — the query logs and yields an empty result, so callers can treat "nothing matched" and
 * "could not look" the same way when that is what they want. Callers who need to distinguish them
 * should ask {@link Context#runOnClientThread(java.util.concurrent.Callable)} directly.</p>
 *
 * <h3>Reuse</h3>
 * <p>A query may be evaluated repeatedly and returns fresh results each time. Filters, de-duplication
 * and sorting are declarations, not consumed state.</p>
 *
 * @param <T> The type of object being queried (e.g., NpcEntity, WidgetEntity)
 * @param <Q> The concrete query class (e.g., NpcQuery)
 * @param <R> The raw RuneLite type (NPC, Widget, TileObject, etc.)
 */
@Slf4j
public abstract class AbstractQuery<T extends Interactable<R>, Q extends AbstractQuery<T, Q, R>, R> {
    protected final Context ctx;
    private final List<Predicate<T>> filters = new ArrayList<>();
    private final List<Function<T, Object>> distinctKeys = new ArrayList<>();
    private Comparator<T> comparator = null;
    private Supplier<Stream<T>> override = null;

    public AbstractQuery(Context ctx) {
        this.ctx = ctx;
    }

    protected abstract Supplier<Stream<T>> source();

    /**
     * Evaluates the query on the client thread and returns a materialised snapshot.
     *
     * <p>This is the single place the source is read. The collect happens inside the client-thread block
     * so that traversal of live game state stays on the client thread regardless of which thread the
     * caller is on.</p>
     *
     * @return the matching entities, never {@code null}; empty if nothing matched or the client thread
     *         could not be reached.
     */
    private List<T> evaluate() {
        try {
            return ctx.runOnClientThread(() -> {
                Stream<T> stream = (override != null ? override : source()).get();
                if (stream == null) {
                    return Collections.emptyList();
                }

                for (Predicate<T> filter : filters) {
                    stream = stream.filter(filter);
                }

                List<T> items = stream.collect(Collectors.toList());

                // De-duplication uses a set built per evaluation, so re-running the query does not
                // inherit the keys seen by a previous run.
                for (Function<T, Object> keyExtractor : distinctKeys) {
                    Set<Object> seen = new HashSet<>();
                    items.removeIf(item -> !seen.add(keyExtractor.apply(item)));
                }

                if (comparator != null) {
                    items.sort(comparator);
                }
                return items;
            });
        } catch (ClientThreadException e) {
            log.warn("Query could not be evaluated: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Evaluates against the supplied entities instead of {@link #source()}. Useful when you already hold
     * a snapshot and want to re-filter it without re-walking the scene. Filters, de-duplication and
     * sorting still apply, and the collection is copied so later mutation by the caller is not observed.
     *
     * @param items The entities to query over. {@code null} or empty yields a query matching nothing.
     * @return Q backed by the given items.
     */
    @SuppressWarnings("unchecked")
    public Q from(Collection<T> items) {
        List<T> snapshot = (items == null || items.isEmpty())
                ? Collections.emptyList()
                : new ArrayList<>(items);
        this.override = snapshot::stream;
        return (Q) this;
    }

    /**
     * Varargs overload of {@code #of(Collection)}.
     * @param items The items to query over
     * @return Q backed by the given items.
     */
    @SafeVarargs
    public final Q from(T... items) {
        return from(items == null ? Collections.emptyList() : Arrays.asList(items));
    }

    /**
     * Overload method for {@code from()}
     * @param items The entities to query over.
     * @return Q backed by the given items
     */
    @SafeVarargs
    public final Q of(T... items) {
        return from(items);
    }

    /**
     * Applies a predicate to the stream to filter elements of the stream.
     * @param predicate Filter to add
     * @return Q
     */
    @SuppressWarnings("unchecked")
    public Q filter(Predicate<T> predicate) {
        if (predicate != null) {
            filters.add(predicate);
        }
        return (Q) this;
    }

    /**
     * Filters for only entities whose name matches the provided name. This is case-insensitive.
     * @param name The name of the object to filter for
     * @return Q entities whose name matches
     */
    public Q withName(String name) {
        return filter(t -> t.getName() != null && t.getName().equalsIgnoreCase(name));
    }

    /**
     * Filters the stream of game entities for ones where the ID matches a provided id
     * @param id Int the item ID to match against.
     * @return Q entities whose item id matches the provided ID.
     */
    public Q withId(int id) {
        return filter(t -> t.getId() == id);
    }

    /**
     * Filters for entities whose name contains the substring or a portion of the name parameter.
     * @param name The name to match against
     * @return Entities whose name contains the prefix
     */
    public Q nameContains(String name) {
        return filter(t -> t.getName() != null && t.getName().toLowerCase().contains(name.toLowerCase()));
    }

    /**
     * Filters out every element, so the query yields nothing.
     * @return Q matching no entities.
     */
    public Q empty() {
        return filter(t -> false);
    }

    /**
     * Checks if the query matches no elements.
     * @return {@code true} if nothing matched; otherwise {@code false}.
     */
    public boolean isEmpty() {
        return evaluate().isEmpty();
    }

    /**
     * Randomises the order of the matched elements.
     * @return A {@code Stream<T>} over the matched entities in randomised order.
     */
    public Stream<T> shuffle() {
        List<T> items = evaluate();
        Collections.shuffle(items);
        return items.stream();
    }

    /**
     * Reverses the order of the matched elements.
     * @return A {@code Stream<T>} over the matched entities in reverse order.
     */
    public Stream<T> reverse() {
        List<T> items = evaluate();
        Collections.reverse(items);
        return items.stream();
    }

    /**
     * Returns the matched entities as a stream.
     *
     * <p>The stream iterates a snapshot taken on the client thread, so it is safe to consume from any
     * thread and will not observe entities spawning or despawning mid-traversal.</p>
     *
     * @return Stream of entities
     */
    public Stream<T> stream() {
        return evaluate().stream();
    }

    /**
     * Returns the underlying RuneLite entities that have been wrapped by the API. For example:
     * {@code ctx.gameObjects().toRuneLite()} returns a list of {@code TileObjects}. You will not be
     * able to perform any interactions on these objects after calling {@code toRuneLite} as they lose
     * their {@code Interactable} wrapping.
     * @return Stream of RuneLite API objects
     */
    public Stream<R> toRuneLite() {
        return evaluate().stream().map(T::raw);
    }

    /**
     * Returns a count of matched entities.
     * @return long count of objects.
     */
    public long count() {
        return evaluate().size();
    }

    /**
     * Filters out elements that match the given predicate.
     * Effectively: filter(!predicate)
     * @param predicate The predicate to apply
     * @return Q All entities except for the ones that match the given predicate
     */
    @SuppressWarnings("unchecked")
    public Q except(Predicate<T> predicate) {
        if (predicate != null) {
            filters.add(predicate.negate());
        }
        return (Q) this;
    }

    /**
     * Interacts with the first matching entity using the given action.
     * Returns false if no entity is found or the interaction fails.
     * Usage: ctx.gameObjects().nameContains("Bank").interact("Open");
     * @param action The action string to perform the interaction i.e. "Open", "Take", "Bank", etc...
     * @return Boolean true if the interaction was successful and false otherwise.
     */
    public boolean interact(String action) {
        return first().map(entity -> entity.interact(action)).orElse(false);
    }

    /**
     * Interacts with a random matching entity.
     * Returns false if no entity is found or the interaction fails.
     * Usage: ctx.npcs().withName("Cow").interactRandom("Attack");
     * @param action The action string to perform the interaction i.e. "Open", "Take", "Bank", etc...
     * @return Boolean true if the interaction was successful and false otherwise.
     */
    public boolean interactRandom(String action) {
        return random().map(entity -> entity.interact(action)).orElse(false);
    }

    /**
     * Returns a random element from the matched entities.
     * Useful for anti-ban measures (e.g., picking a random cow to attack).
     * @return A random matched entity, or {@link Optional#empty()} if nothing matched.
     */
    public Optional<T> random() {
        List<T> all = evaluate();
        if (all.isEmpty()) return Optional.empty();
        return Optional.ofNullable(all.get(ThreadLocalRandom.current().nextInt(all.size())));
    }

    /**
     * Keeps only the first entity for each distinct key.
     * Usage: {@code ctx.npcs().distinct(NpcEntity::getName).list();}
     * (Returns one of each type of NPC nearby)
     *
     * <p>De-duplication is applied after filtering, and a fresh key set is used on every evaluation, so
     * the query can be re-run and will produce the same result.</p>
     *
     * @param keyExtractor The function to use to determine uniqueness keys between entities
     * @return Q The distinct entities
     */
    @SuppressWarnings("unchecked")
    public Q distinct(Function<T, Object> keyExtractor) {
        if (keyExtractor != null) {
            distinctKeys.add(keyExtractor);
        }
        return (Q) this;
    }

    /**
     * Ensures that only unique elements based on their IDs are included.
     * <p>An alias for {@link #distinctById()}.</p>
     * @return Q containing only unique elements based on their IDs.
     */
    public Q unique() {
        return distinctById();
    }

    /**
     * Keeps only the first entity for each distinct id.
     * @return Q A filtered result containing only elements with unique IDs.
     */
    public Q distinctById() {
        return distinct(Interactable::getId);
    }

    /**
     * Applies a comparator used to sort the matched entities.
     * @param comparator Comparator to add
     * @return Q A sorted query
     */
    @SuppressWarnings("unchecked")
    public Q sorted(Comparator<T> comparator) {
        this.comparator = comparator;
        return (Q) this;
    }

    /**
     * Returns the matched entities as a list.
     * @return A list of matched entities (e.g., NpcEntity, WidgetEntity), never {@code null}.
     */
    public List<T> list() {
        return evaluate();
    }

    /**
     * An alias for {@code list()}.
     * @return The matched entities as a list.
     */
    public List<T> result() {
        return list();
    }

    /**
     * Collects the matched entities into a map keyed by the id of the element. Generally this will
     * be the item id for objects like {@code ContainerItem}, {@code EquipmentEntity}, and {@code GroundObjectEntity} but
     * can take on other ids for things like Game objects, NPC's and widgets.
     *
     * <p>Ids are not unique in general — two cows share an NPC id — so when several entities share an
     * id, the first one encountered wins.</p>
     *
     * @return Map of entities keyed by their id.
     */
    public Map<Integer, T> map() {
        return evaluate().stream()
                .collect(Collectors.toMap(T::getId, Function.identity(), (first, duplicate) -> first));
    }

    /**
     * Returns the first matched entity (e.g., NpcEntity, WidgetEntity).
     * @return The first matched entity, or {@link Optional#empty()} if nothing matched.
     */
    public Optional<T> first() {
        List<T> items = evaluate();
        return items.isEmpty() ? Optional.empty() : Optional.ofNullable(items.get(0));
    }

    /**
     * Returns the first matched entity that also satisfies the provided predicate, respecting any
     * predicates already applied via {@link #filter}.
     * @param predicate The predicate to apply
     * @return The first matching entity, or {@link Optional#empty()} if nothing matched.
     */
    public Optional<T> firstMatching(Predicate<T> predicate) {
        return evaluate().stream().filter(predicate).findFirst();
    }

    /**
     * Returns true if at least one entity matched.
     * @return True if the entity is present and false otherwise.
     */
    public boolean isPresent() {
        return !evaluate().isEmpty();
    }

    /**
     * Takes the first N matched entities.
     * @param n The number of elements to take.
     * @return List of entities
     */
    public List<T> take(int n) {
        List<T> items = evaluate();
        return items.size() <= n ? items : new ArrayList<>(items.subList(0, Math.max(0, n)));
    }
}
