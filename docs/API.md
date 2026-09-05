# Using the API

The API is designed around RuneLite and as such expects to be running within the context of the RuneLite client. Trying to run
the API on its own will generally result in strange dependency errors with Guice or issues finding implementations for many of 
RuneLite's own API interfaces.

## API Design & Methodology

The API is broken up into two distinct ways of accessing game information:

- Services (`com.kraken.api.service`)
- Query System (`com.kraken.api.query`)

Each API paradigm has its strengths, and it's likely you will need both when building semi and fully autonomous RuneLite
plugins. Read more about each API paradigm below to see which one (or a combination of both) suits your plugin needs.

The Kraken API also ships with a variety of useful utilities for plugins from logging, mouse, and table overlays to
randomization, math and string utilities, and more! To learn more about Kraken's extra utilities, check out the [utilities doc](UTILITIES.md).

### Services

Services leverage the software design pattern of dependency injection. This is the exact same pattern adopted by RuneLite
to ensure that plugins get exactly what they need to run from RuneLite and nothing more. As the developer you will declare to
your script what you need from the Kraken API, and the dependencies will be directly injected into your script at runtime.
Dependency injection ensures that your script classes remain lightweight, testable, and easy to debug.

The Service API paradigm is useful for static widgets or global game entities, for example:

- Bank interface – There is only a single bank interface to open, close, and set withdrawal modes on
- Shops – One shop is open at a time, and buying under a price or coin limit is a global concern rather than a per item one (see [Shops](SHOPS.md))
- Prayers – A finite number of static prayer widgets
- Spells – A fixed number of in-game spells
- UI – Static utilities for calculating UI element bounds, interfacing with dialogue, and switching client tabs
- Camera – A single camera exists and is centered around your local player (`ctx.cameras().first()` doesn't really make much sense!)
- Walking – Getting from one place to another is a global concern spanning the whole map rather than a per entity one (see [Walker](WALKER.md))
- etc...

If you needed to toggle a prayer, cast a spell, or close the bank, then the service API paradigm would suit your plugin
well.

### Query System

The query system allows you to flexibly "query", refine, and filter for dynamic game entities like:

- Players
- NPC's
- Game objects
- Tile objects (game, wall, decorative and ground objects; doors and gates are usually wall objects)
- Ground Items
- Projectiles and graphics objects
- Widgets
- Worn equipment
- Inventory, bank, deposit box and shop items
- Worlds

The query paradigm wraps familiar RuneLite API objects with an `Interactable` interface allowing you to not
only __find__ game entities but also __interact__ with them in a straightforward fashion.
Most interactions call the client's own menu-action handler (`doAction`) through reflection, and a small number are sent as
network packets. See [Interaction](INTERACTION.md) for how that works.

The API uses method chaining to filter for specific game entities loaded within the scene and exposes all methods on the underlying RuneLite
API objects using the `raw()` method on every wrapped game entity class.

## Game Context

The entire query API is exposed through a single class called the game `Context` (`com.kraken.api.Context`).
This singleton class allows you to have one lightweight dependency which functions as a facade to query just about any game entity you would want for plugin development.

For example, to attack a nearby Goblin:

```java
@PluginDescriptor(
        name = "Example",
        description = "Example plugin"
)
public class ExamplePlugin extends Plugin {
    
    @Inject
    private Context ctx;
    
    @Subscribe
    private void onGameTick(GameTick e) {
        Player local = ctx.players().local().raw();
        
        if(local.isInteracting()) {
            return;
        }

        ctx.npcs().withName("Goblin")
                .except(n -> n.raw().isInteracting())
                .sortByDistance()
                .interact("Attack");
    }
}
```

The queries available on `Context` are `npcs()`, `players()`, `gameObjects()`, `tileObjects()`, `groundItems()`, `projectiles()`,
`graphicsObjects()`, `widgets()`, `worlds()`, `equipment()`, `inventory()`, `bank()`, `bankInventory()`, `depositBox()`, `shop()` and `shopInventory()`.

### Lifecycle

- `Context` is a Guice singleton; inject it with `@Inject`. Packets and interaction hooks are set up when Guice constructs it, so there is nothing to initialize.
- Call `ctx.shutdown()` from your plugin's `shutDown()`. Otherwise the `Context`'s event bus subscriptions and mouse listener leak across plugin enable/disable cycles.
- Code that cannot be injected (static helpers, for example) can use `com.kraken.api.core.Services.context()`, which resolves against RuneLite's root injector.

### Query Thread Safety

The entire query API is designed to be thread-safe, so any queries, filters, or interactions can be run on non-client threads. When
callable methods need to execute on RuneLite's client thread, they will be scheduled there, blocking until the method executes.
This helps ensure your plugin code is fully thread-safe, predictable, and easy to read.

`ctx.runOnClientThread(Callable)` blocks for up to three seconds and throws `ClientThreadException` if the client thread does not
answer in time. `ctx.runOnClientThreadOptional(Callable)` never throws: a failed hand-off and a `null` result both return an empty `Optional`.

To see specific examples of various queries, check out the [API tests](https://github.com/Kraken-Plugins/kraken-api/tree/master/src/test/java/plugins/api) which utilize a real RuneLite plugin to query and find
various game entities around Varrock East Bank.

> :warning: When running on non-client threads, the action must be scheduled on the client thread and is thus asynchronous in nature.

### Query Abstractions

The query system is built on two main abstractions: `AbstractQuery` and `AbstractEntity`.
These base classes are extended by specific implementations in the `com.kraken.api.query` package, such as `NpcQuery` and `NpcEntity`, `PlayerQuery` and `PlayerEntity`, etc. This design allows for a consistent API across different types of game entities while enabling type-specific functionality.

#### AbstractQuery

`AbstractQuery` is the base class for all game client queries. It provides a fluent API for filtering and manipulating streams of game entities.

Key methods include:

- `filter(Predicate<T> predicate)`: Applies a custom filter to the stream.
- `withName(String name)`: Filters entities by name (case-insensitive).
- `withId(int id)`: Filters entities by ID.
- `nameContains(String name)`: Filters entities whose name contains the specified substring.
- `except(Predicate<T> predicate)`: Filters out elements that match the given predicate.
- `distinct(Function<T, Object> keyExtractor)` / `distinctById()` / `unique()`: Remove duplicates.
- `sorted(Comparator<T> comparator)`, `shuffle()`, `reverse()`: Reorder the stream.
- `stream()`: Returns the raw stream of elements, allowing for manual filtering and matching.
- `toRuneLite()`: Returns the underlying RuneLite entities wrapped by the API.
- `count()`, `isEmpty()`, `isPresent()`: How many matched, and whether anything did.
- `list()` / `result()`: Collects the stream into a list.
- `map()`: Collects the stream into a map keyed by entity ID.
- `take(int n)`: Returns the first N elements from the stream.
- `first()`: Returns the first matched element as an `Optional`, empty when nothing matched.
- `firstMatching(Predicate<T> predicate)`: Returns the first element that also satisfies the predicate, as an `Optional`.
- `random()`: Returns a random matched element as an `Optional`, empty when nothing matched.
- `interact(String action)`: Interacts with the first matched element; returns false when nothing matched.
- `interactRandom(String action)`: Interacts with a random matched element; returns false when nothing matched.

##### Return conventions

The query layer never returns a bare `null`. Collection-valued terminals (`list()`, `result()`, `map()`, `take(n)`) return empty collections, and single-valued terminals (`first()`, `firstMatching(...)`, `random()`, `nearest()`, `nearestTo(...)`) return `Optional`. A query that matches nothing is normal; handle it with `Optional` methods (`ifPresent`, `map`, `orElse`) or use the query-level `interact(...)`/`isPresent()`/`isEmpty()` terminals, which fold the empty case into their return value.

#### AbstractSpatialQuery

Queries over entities that occupy a tile (NPCs, players, game/tile objects, ground items, projectiles, and graphics objects) share one spatial vocabulary, defined once on `AbstractSpatialQuery`:

- `within(int distance)`: Entities within the given tile distance of the local player, same plane only.
- `within(WorldPoint anchor, int distance)`: The same, measured from an anchor point.
- `withinArea(WorldPoint min, WorldPoint max)`: Entities inside the rectangle spanned by two corners.
- `at(WorldPoint point)`: Entities standing on an exact tile, plane included.
- `reachable()`: Entities the player can currently walk to.
- `sortByDistance()` / `sortByDistanceTo(WorldPoint anchor)`: Order by proximity, closest first.
- `nearest()` / `nearestTo(WorldPoint anchor)`: The closest match, as an `Optional`.

Distances are Chebyshev tile distances between world locations in the coordinate space the client reports for the top-level world view, the same space the local player's location uses, so these filters remain valid inside instanced regions such as raids. Entities on another plane never match a distance filter and sort last. Player-anchored filters yield empty results when there is no local player (login screen, mid world-hop).

#### AbstractContainerQuery

Queries over the player's item containers (inventory, bank, bank-side inventory, deposit box, shop-side inventory) share one item vocabulary, defined once on `AbstractContainerQuery`:

- `inSlot(int slot)`: The item occupying a slot.
- `noted()` / `unnoted()`: Bank notes versus physical items.
- `stackable()`: Items that stack.
- `quantityGreaterThan(int amount)`: Stacks strictly larger than the amount.
- `withAction(String action)`: Items offering a menu action in this container.
- `hasItem(int id)` / `hasItem(String name)` / `hasItems(...)`: Presence checks.

#### AbstractEntity

`AbstractEntity` wraps a raw RuneLite API object (e.g., `NPC`, `TileObject`, `Widget`) and implements the `Interactable` interface. It provides a consistent way to interact with different types of game entities.

Key methods include:

- `raw()`: Returns the underlying RuneLite API object.
- `interact(String action)`: Performs an interaction with the entity (e.g., "Attack", "Talk-to"). Returns `false` if nothing was sent, so it is safe to retry on.
- `getId()`: Returns the ID of the entity.
- `getName()`: Returns the name of the entity.

### Structure

The Kraken API exposes both high and low-level functions for working with
game objects, NPC's, movement, pathing, network packets, and more.
The list below describes the packages developers are most likely to use when writing scripts or plugins.

- `core` - Abstract base classes (`AbstractQuery`, `AbstractEntity`, `Interactable`) used by the rest of the API.
    - `core.interaction` - The `InteractionManager` and the reflective `doAction` dispatch behind every `interact(...)` call.
    - `core.packet` - Low level packet construction and the `MousePackets`, `MovementPackets` and `WidgetPackets` helpers.
    - `core.script` - The `Script`, `Task` and break-handling classes described in [Scripting](SCRIPTING.md).
    - `core.hooks` - `HooksLoader` and the `hooks.json` mappings of obfuscated client members.
- `service` - High level API's for directly interacting with static/global game elements such as banking, prayer, spells, dialogue, shops and walking.
- `query` - The query API classes for finding and interacting with dynamic game elements like: inventory, npcs, players, game objects, and more.
- `input` - Keyboard and [mouse](MOUSE.md) input.
- `overlay` - Simple and common overlays which can be directly used in RuneLite plugins, e.g. mouse position, tables and log panels.
- `util` - Math, random and string helpers.

Simulation code (tick engines, NPC pathing, line of sight) lives in the repository's test sources rather than the published jar.

### Packets

When the `Context` is built, the API reads `hooks.json` to find the client members it needs and prepares the reflective calls it uses to
interact with the game. Most interactions call the client's own `doAction` method, which builds and queues the packet exactly as a real
click would. Five packets are built and sent by the API itself: `EVENT_MOUSE_CLICK`, `MOVE_GAMECLICK`, `RESUME_COUNTDIALOG`,
`RESUME_OBJDIALOG` and `RESUME_STRINGDIALOG`. In both cases client methods are invoked with reflection; the API does not modify the client's bytecode.

Packet source code can be [viewed here](https://github.com/Kraken-Plugins/kraken-api/tree/master/src/main/java/com/kraken/api/core/packet). The core foundation of packets, 
mappings, and client deobfuscation techniques would not be possible without the [EthanVann API](https://github.com/Ethan-Vann/PacketUtils/tree/master). Please see the README.md 
of the Kraken API for credits to their foundational work.

### Events

The following custom event can be subscribed to within plugins. It is posted by the Kraken client's **Patch Packet Listener**
option (on by default, see [configuration](https://kraken-plugins.com/docs/client/configuration.html#client-patches)), so it is available when your plugin runs inside the Kraken client.

> :warning: Note: New events *may* be added in the future however, adding new events frequently
requires runtime bytecode manipulation of the client which can be both brittle and goes against a core principle of 
an API which works **without** client modifications. Events are added with caution and testing.

| Event Name  | Trigger                                                      | Example Usage                                 |
|-------------|--------------------------------------------------------------|-----------------------------------------------|
| Packet Sent | Invoked when a packet is sent from the client to the server. | `@Subscribe onPacketSent(PacketSent e) {...}` |
