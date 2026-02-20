# API Design & Methodology

The API is broken up into 2 distinct ways of accessing game information:

- Services (`com.kraken.api.service`)
- Query System (`com.kraken.api.query`)

Each API paradigm has its strengths, and it's likely you will need both when building semi and fully autonomous RuneLite
plugins. Read more about each API paradigm below to see which one (or a combination of both) suites your plugin needs.

## Services

Services leverage the software design pattern of dependency injection. This is the exact same pattern adopted by RuneLite
to ensure that plugins get exactly what they need to run from RuneLite and nothing more. As the developer you will declare to
your script what you need from the Kraken API and the dependencies will be directly injected into your script at runtime.
Dependency injection ensures that your script classes remain lightweight, testable, and easy to debug.

The Service API paradigm is useful for static widgets or global game entities, for example:

- Bank interface - There is only a single bank interface to open, close, and set withdrawal modes on
- Prayers - A finite number of static prayer widgets
- Spells - A fixed number of in-game spells
- UI - Static utilities for calculating UI element bounds, interfacing with Dialogue, and switching client tabs
- Camera - A single camera exists and is centered around your local player (`ctx.cameras().first()` doesn't really make much sense!)
- etc...

If you needed to toggle a prayer, cast a spell, or close the bank then the service API paradigm would suite your plugin
well.

## Query System

The query system allows you to flexibly "query", refine, and filter for dynamic game entities like:

- Players
- NPC's
- Game objects
- Ground Items
- Widgets
- Worn equipment (in the interface as well as your inventory)
- Inventory items
- and Bank items

The query paradigm wraps familiar RuneLite API objects with an `Interactable` interface allowing you to not
only __find__ game entities but also __interact__ with them in a straightforward fashion.
All interactions use network packets to communicate directly with the game servers.

The API utilizes method chaining to filter for specific game entities loaded within the scene and exposes all methods on the underlying RuneLite
API objects using the `raw()` method on every wrapped game entity class.

The entire query API is exposed through a single class called the game `Context`.
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
                .nearest()
                .interact("Attack");
    }
}
```

The entire query API is designed to be thread safe so any queries, filters, or interactions can be run on non-client threads.
When callable methods need to execute on RuneLite's client thread they will be scheduled there, blocking until the method executes.
This helps ensure your plugin code is fully thread safe, predictable, and easy to read.

To see specific examples of various queries check out the [API tests](https://github.com/cbartram/kraken-api/tree/master/lib/src/test/java) which utilize a real RuneLite plugin to query and find
various game entities around Varrock east bank.

> :warning: When running on non-client threads the action must be scheduled on the client thread and is thus asynchronous in nature.

## Structure

The Kraken API exposes both high and low level functions for working with
game objects, NPC's, movement, pathing, simulations, network packets, and more.
The documentation below describes the most likely packages developers will use when writing scripts or plugins.

- `core` - The package contains abstract base class logic which is used internally by different API methods and exposes the `Script` class.
    - `core.packet` - The package includes low and high level API's to send network packets to game servers for automation actions.
        - `core.packet.entity` - Generally most packet layer use cases will use the `core.packet.entity` API for interaction with Game Objects, NPC's, interfaces, and players.
- `service` - The package contains high level API's for directly interacting with static/global game elements such as (banking, prayer, spells, etc...) and use the `core.packet` package to form the backbone for the interactions
- `input` - Contains classes to help process and use input devices like mouses and keyboards.
- `query` - Contains the query API classes for finding and interacting with dynamic game elements like: inventory, npcs, players, game objects, and more.
- `overlay` - Contains simple and common overlays which can be directly used in RuneLite plugins e.g. Mouse position
- `sim` - Includes classes for simulating game ticks, NPC pathing, movement, line of sight, and players. This is useful for advanced
  plugins which evaluate hundreds of potential outcomes every game tick to determine the best "decision". e.g. Inferno and Colosseum plugins