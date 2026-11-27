# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

Kraken API is a Java 17 library that extends the RuneLite (Old School RuneScape) client API with query, service, interaction, and simulation layers for plugin authors. It publishes a shaded jar (`com.github.kraken:kraken-api`) to GitHub Packages / Maven Local and attaches it, with sources/javadoc jars and the generated `llms.txt` files, to each GitHub release as public assets. It is a library, not an application — the only runnable thing is a RuneLite client launched with test plugins loaded.

`AGENTS.md` in the repo root is the maintained agent guide and carries the project's policies (code reuse, layer boundaries, testing, commits). Read it alongside this file; keep both updated when commands, paths, or workflows change.

## Commands

```bash
./gradlew compileJava -q                 # fast syntax/type check of main sources
./gradlew compileTestJava -q             # also compiles the in-client harness + unit tests
./gradlew test                           # JUnit 5 suite (src/test/java/unit/**), ~10s
./gradlew test --tests '*PlannerTest'    # single test class
./gradlew test --tests '*PlannerTest.plansAgainstBusyWaveWithinBudget'
./gradlew :simulation:test               # the simulation module's tests, the recorded wave gate included
./gradlew :simulation:simulationBenchmark   # JMH rollout benchmarks (rollouts per 15 ms = 15,000,000 / ns per op)
./gradlew clean build publishToMavenLocal shadowJar   # full build + install to ~/.m2
VERSION=1.0.0-SNAPSHOT-LOCAL ./gradlew clean build publishToMavenLocal shadowJar
VERSION=5.1.5 ./gradlew generateLlmsTxt          # llms.txt / llms-full.txt / llms-api.txt -> build/llms (-PllmsOutput=<dir> to redirect)
```

CI (`.github/workflows/build.yml`) runs `./gradlew clean build shadowJar` on PRs to `master`/`develop`.

### Running the client harness

Most API behavior can only be validated inside a live game client. Run the main class `src/test/java/PluginRunnerTest.java` with program args `<plugin-class> --developer-mode` and VM arg `-ea`:

```
plugins.api.ApiTestPlugin --developer-mode        # API test suite plugin
```

`PluginRunnerTest` treats `args[0]` as a plugin class name, loads it via `ExternalPluginManager.loadBuiltin`, and passes the rest to `RuneLite.main`. `./gradlew runelite` launches `plugins.api.ApiTestPlugin` that way; use the IDE when you want a different plugin or a debugger attached.

`docs/TESTS.md` lists the in-game preconditions each harness test needs (location, bank contents, skill levels). Most assume Varrock West Bank.

## Architecture

### `Context` — the single entry point

`com.kraken.api.Context` is a Guice `@Singleton` injected into plugins. It is a facade, not a logic container: it exposes query accessors (`npcs()`, `players()`, `inventory()`, `bank()`, `bankInventory()`, `depositBox()`, `equipment()`, `gameObjects()`, `groundItems()`, `widgets()`, `worlds()`), client-thread helpers (`runOnClientThread`, `runOnClientThreadOptional`, `getVarbitValue`, `getVarpValue`, `getWidget`, `runScript`), and holds `Client`, `ClientThread`, `ItemManager`, `VirtualMouse`, `InteractionManager`. Keep feature logic out of it.

### query vs. service — the core split

- `query/` — fluent filters over *dynamic* entities (NPCs, players, ground items, containers, widgets, worlds). Pattern is `XQuery` + `XEntity` pairs extending `core/AbstractQuery` and `core/AbstractEntity`. Chain filters (`withName`, `withId`, `nameContains`, `within`, `reachable`, `except`, `filter`) then terminate (`first()`, `nearest()`, `list()`, `take()`). Every entity exposes `raw()` for the underlying RuneLite object and implements `core/Interactable`.
- `service/` — *static or global* game systems: bank, prayer, magic, dialogue, camera, movement, pathfinding, grand exchange, map, tile, ui, actor, plus `service/util` (sleep, random, reflection, `TaskChain`, dps, price).

New behavior goes in the narrowest layer that owns it. Do not add a query for something singular (there is one camera, one bank interface), and do not add a service for something you filter and pick from.

### Interaction: `doAction` first, packets second

`core/interaction/` is the primary path. `InteractionManager` takes RuneLite objects (`NPC`, `Player`, `TileObject`, `Widget`, container items, widget→target combinations), resolves them through `MenuActionResolverRegistry` → a per-type `ActionResolver` → a `ResolvedMenuAction`, and dispatches via `DoActionInvoker` (reflective call into the client's obfuscated `doAction` choke point). This covers ~85% of interactions and is what most entity `interact(...)` calls bottom out in. Where each click lands on its target comes from `ClickPoints` (a pluggable `ClickPointSampler`, default point otherwise), and `MousePackets` posts `KrakenClickSent` on the event bus after every click packet, since Kraken's clicks post no `MenuOptionClicked`. `core/interaction/timing/ClickScheduler` times clicks inside a tick; its gap counts every click the game sends (`ClickHistory`: Kraken's packets plus canvas presses from `input/mouse/MousePresses`), and `setGapModelFactory` replaces the seeded gap model. Events Kraken posts on the AWT thread go through `input/InputDispatch`, so listeners can tell them apart with `InputDispatch.isSynthetic()`.

`core/packet/` handles the remainder — movement, spoofed mouse clicks, some dialogue — by building raw client packets (`PacketFactory`, `PacketClient`, `BufferUtils`, `entity/{Mouse,Movement,Widget}Packets`).

### Walker — Kraken's own planner, dataset and registry

The walker has no pathfinding dependency. `service/pathfinding/` holds the search
(`GlobalPathfinder`) and what it runs on, all package-private: the collision map (`WorldCollisionData`,
`CollisionMap`), the node store and queues (`NodeGraph`, `VisitedTiles`, `IntDeque`, `IntMinHeap`),
and `TransportPlanner`, which builds the `TransportNetwork` one search may use. The data ships in
`src/main/resources/walker/`: `collision-map.zip`, `dataset.json` (about 14,700 transports once its
hub networks are paired up) and `transports.json`. Both the map and the dataset come from the
[shortest-path](https://github.com/Skretzo/shortest-path) project (BSD 2-Clause, see
`walker/NOTICE.txt`); do not add that project back as a dependency.

Every transport is a `TransportDefinition`. `TransportRegistry` (a client-wide `@Singleton` in
`service/walker/transport/registry/`) holds the dataset (`TransportDataset`, read once on first
use) underneath, and registered definitions, shape handler overrides and hidden dataset edges on
top. `TransportExecutor` operates a transport through the handler for its `TransportShape`. A
definition's `TransportCategory` decides the planning option that turns it on and off and its
default shape. A definition without an origin tile is a teleport usable from anywhere.
`docs/WALKER.md` covers the details.

`dataset.json` and `collision-map.zip` are generated by `scripts/update_walker_transports.py` from
a checkout of upstream shortest-path and, optionally, a game cache dump
(`scripts/WalkerCacheDump.java`). Rerun the script instead of editing them, and never put a hand
written row in `dataset.json`.

Corrections to single dataset transports are hand written rows in `transports.json`, not code: the
Al Kharid toll gate (`kraken:al-kharid-gate-*`) and the webs (`kraken:web-*`, shape
`CLEAR_THEN_WALK`, operated by `ClearThenWalkHandler`). Each sets `replaces` to hide the dataset's
edge, and `HandWrittenTransportsTest` checks them against the dataset. Add the next such fix as a
row there instead of a special case in `GlobalPathfinder` or `TransportExecutor`.
`BundledTransports.toJson` writes a definition as a row, and the Transports tool of the DevTools
plugin in `kraken-plugins` records, tests and saves rows in a live client.

### `hooks.json` — the only client-revision-sensitive file

`src/main/resources/hooks.json` holds `reflectionHooks`, `loginHooks`, `securityHooks`, and `packets`. `core/hooks/HooksLoader` parses it in a static initializer into `GameHooks` and friends; everything obfuscation-dependent reads from there. After an OSRS client revision, it will be manually updated by a human. Never update this file directly. Never hardcode obfuscated names elsewhere in the codebase.

`kraken-updater` also writes top-level `runeliteVersion` and `revision` (game revision) fields. `Context` passes `Client.getRevision()` to `HooksLoader.warnOnRevisionMismatch` at startup, and `build.gradle` copies both fields into the shaded jar's manifest as `Kraken-RuneLite-Version` and `Kraken-Game-Revision` (next to `Kraken-Api-Version`) for build tooling. Hooks mapped before these fields existed simply omit them.

### `core/script/` — long-running automation

`Script` (abstract, implements `Scriptable`) drives a `loop()` returning a delay in ms, ticked off RuneLite's `GameTick` with lifecycle (`start`/`stop`/`pause`/`resume`, `onStart`/`onStop`) and break handling (`script/breakhandler`). `loop()` runs off the client thread — anything touching client state must go through `Context.runOnClientThread(...)`. Reusable steps belong behind `Task` / `AbstractTask` / `PriorityTask` / `RunnableTask` or a service, not inside plugin event handlers.

### Simulation: the `simulation` module

- `simulation/` is a Gradle subproject (`settings.gradle` includes it) holding the simulation core, `com.kraken.api.simulation.{geom,world,move,hits,player,npc,tick,data,query,trace}`: a deterministic, allocation free tick model with no planner and no scoring. Its compile classpath is Gson and Lombok only, so a RuneLite, Guice or Kraken import fails its build; keep it that way. The root build shades it into `kraken-api.jar` (`implementation project(':simulation')`) and adds its sources to the sources and javadoc jars.
- Encounter mechanics come from content packs: per type `NpcBehavior`s and world `PackRules` (both abstract classes), state in `SimState`'s lanes, tables loaded by `MechanicsData.load(owner, root, binder)`. The Colosseum pack is a parity copy in `simulation/src/test/java/com/kraken/api/simulation/colosseum/` (Auto Colosseum V2 owns it); it is not shipped.
- `com.kraken.api.simulation.live` in the root project is the RuneLite side: region anchor, collision capture, server tiles, NPC indexer, animation stamping, hp ledger, corpses, the live trace recorder.
- Gates: `RecordedWaveGateTest` replays the seven recorded waves under `simulation/src/test/resources/colosseum/traces/` and may not get worse; `ThroughputGateTest` and `./gradlew :simulation:simulationBenchmark` (JMH, `simulation/src/jmh`) measure rollouts. Hooks are virtual calls the JIT may not inline, so add per tick hooks with care and measure. `docs/SIMULATION.md` documents the module, the pack contract and the trace format.
- `src/test/java/colosim/` is a separate Java port of the community Colosseum line of sight simulator with its own GUI, kept in the test sources for reference.

### Test source layout

`src/test/java` mixes two unrelated things:
- `unit/**` — real JUnit 5 tests, the only thing `./gradlew test` runs.
- `plugins/**` — RuneLite plugins used as the manual harness and as reference examples for API consumers (`plugins/api/` is the canonical usage example; `plugins/api/tests/BaseApiTest` with `@Before`/`@After` is a hand-rolled in-client harness whose tests are registered in `ApiTestPlugin.registerTest(...)` and toggled from `ApiTestConfig`).

`build.gradle` adds `tests/sim` as an extra test srcDir (currently absent) and excludes `unit/plugins/colosseumv2/**` (orphaned tests for a removed plugin).

## Dependency rules

RuneLite, guice, guava, gson, slf4j, and lombok are `compileOnly` on purpose — RuneLite supplies them at runtime, and bundling them causes classloader/version conflicts. The only `implementation` dependency is the repository's own `simulation` project, so the shaded jar holds only Kraken's own classes and resources. Do not promote a `compileOnly` dependency to `implementation`, and think hard before adding any new runtime dependency.

## Conventions

- Java 17 toolchain; Lombok is used where surrounding code already uses it.
- Never hand-write a private no-args constructor (e.g. to prevent instantiating a utility class). Annotate the class with `@NoArgsConstructor(access = AccessLevel.PRIVATE)` instead.
- Descriptive names, no abbreviations. Search for an existing helper before writing a new one — refactor to generalize rather than duplicate.
- Never hand-edit `build/**`, `docs/kraken-api/**` or the generated `llms*.txt` files. The llms files come from `docs/llms/overview.md`, `docs/*.md` and the generator in `src/llms/` (a Javadoc doclet that also renders Lombok-generated members).
- When adding block comments to classes or methods avoid saying what the method did previously or why there was an error in previous versions of the code. Stick to what the code actively does now, since it has been changed.
- Commits: sign off with `git commit -s`. Project policy is that AI agents are **never** listed as commit co-authors — omit any `Co-Authored-By` trailer naming an AI.
- Versioning is automated: CI bumps the patch on merge to `master`; bump `version.txt` manually for a minor/major base.
- Don't include comments like `// ------------- Something here ---------------` that doesn't add anything meaningful to the codebase
- Any public methods should have their respective javadocs with `@param` and `@return` defined. Any private methods which are more complex than some simple conditional logic should also be documented inline with block level comments.
- When writing documentation be sure to include `@param` and `@return` for any parameters and return values like:

```java
/**
 * Clamps damage into [minimum, maximum].
 * @param maximum The maximum value
 * @param minimum The minimum value
 * @return The return value description
 */
```
