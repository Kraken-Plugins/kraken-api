# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

Kraken API is a Java 11 library that extends the RuneLite (Old School RuneScape) client API with query, service, interaction, and simulation layers for plugin authors. It publishes a shaded jar (`com.github.kraken:kraken-api`) to GitHub Packages / Maven Local. It is a library, not an application — the only runnable thing is a RuneLite client launched with test plugins loaded.

`AGENTS.md` in the repo root is the maintained agent guide and carries the project's policies (code reuse, layer boundaries, testing, commits). Read it alongside this file; keep both updated when commands, paths, or workflows change.

## Commands

```bash
./gradlew compileJava -q                 # fast syntax/type check of main sources
./gradlew compileTestJava -q             # also compiles the in-client harness + unit tests
./gradlew test                           # JUnit 5 suite (src/test/java/unit/**), ~10s
./gradlew test --tests '*PlannerTest'    # single test class
./gradlew test --tests '*PlannerTest.plansAgainstBusyWaveWithinBudget'
./gradlew clean build publishToMavenLocal shadowJar   # full build + install to ~/.m2
VERSION=1.0.0-SNAPSHOT-LOCAL ./gradlew clean build publishToMavenLocal shadowJar
```

CI (`.github/workflows/build.yml`) runs `./gradlew clean build shadowJar` on PRs to `master`/`develop`.

### Running the client harness

Most API behavior can only be validated inside a live game client. Run the main class `src/test/java/PluginRunnerTest.java` with program args `<plugin-class> --developer-mode` and VM arg `-ea`:

```
plugins.api.ApiTestPlugin --developer-mode        # API test suite plugin
plugins.simulation.SimulationPlugin               # simulation example
plugins.colosseum.AutoColosseumPlugin             # colosseum engine debug plugin
```

`PluginRunnerTest` treats `args[0]` as a plugin class name, loads it via `ExternalPluginManager.loadBuiltin`, and passes the rest to `RuneLite.main`. The `./gradlew runelite` task passes `'--developer-mode plugins.api.ApiTestPlugin'` as a *single* argument, which does not match that contract — launch from the IDE instead.

`docs/TESTS.md` lists the in-game preconditions each harness test needs (location, bank contents, skill levels). Most assume Varrock West Bank.

## Architecture

### `Context` — the single entry point

`com.kraken.api.Context` is a Guice `@Singleton` injected into plugins. It is a facade, not a logic container: it exposes query accessors (`npcs()`, `players()`, `inventory()`, `bank()`, `bankInventory()`, `depositBox()`, `equipment()`, `gameObjects()`, `groundItems()`, `widgets()`, `worlds()`), client-thread helpers (`runOnClientThread`, `runOnClientThreadOptional`, `getVarbitValue`, `getVarpValue`, `getWidget`, `runScript`), and holds `Client`, `ClientThread`, `ItemManager`, `VirtualMouse`, `InteractionManager`. Keep feature logic out of it.

### query vs. service — the core split

- `query/` — fluent filters over *dynamic* entities (NPCs, players, ground items, containers, widgets, worlds). Pattern is `XQuery` + `XEntity` pairs extending `core/AbstractQuery` and `core/AbstractEntity`. Chain filters (`withName`, `withId`, `nameContains`, `within`, `reachable`, `except`, `filter`) then terminate (`first()`, `nearest()`, `list()`, `take()`). Every entity exposes `raw()` for the underlying RuneLite object and implements `core/Interactable`.
- `service/` — *static or global* game systems: bank, prayer, magic, dialogue, camera, movement, pathfinding, grand exchange, map, tile, ui, actor, plus `service/util` (sleep, random, reflection, `TaskChain`, dps, price).

New behavior goes in the narrowest layer that owns it. Do not add a query for something singular (there is one camera, one bank interface), and do not add a service for something you filter and pick from.

### Interaction: `doAction` first, packets second

`core/interaction/` is the primary path. `InteractionManager` takes RuneLite objects (`NPC`, `Player`, `TileObject`, `Widget`, container items, widget→target combinations), resolves them through `MenuActionResolverRegistry` → a per-type `ActionResolver` → a `ResolvedMenuAction`, and dispatches via `DoActionInvoker` (reflective call into the client's obfuscated `doAction` choke point). This covers ~85% of interactions and is what most entity `interact(...)` calls bottom out in.

`core/packet/` handles the remainder — movement, spoofed mouse clicks, some dialogue — by building raw client packets (`PacketFactory`, `PacketClient`, `BufferUtils`, `entity/{Mouse,Movement,Widget}Packets`).

### `hooks.json` — the only client-revision-sensitive file

`src/main/resources/hooks.json` holds `reflectionHooks`, `loginHooks`, `securityHooks`, and `packets`. `core/hooks/HooksLoader` parses it in a static initializer into `GameHooks` and friends; everything obfuscation-dependent reads from there. After an OSRS client revision, it will be manually updated by a human. Never update this file directly. Never hardcode obfuscated names elsewhere in the codebase.

### `core/script/` — long-running automation

`Script` (abstract, implements `Scriptable`) drives a `loop()` returning a delay in ms, ticked off RuneLite's `GameTick` with lifecycle (`start`/`stop`/`pause`/`resume`, `onStart`/`onStop`) and break handling (`script/breakhandler`). `loop()` runs off the client thread — anything touching client state must go through `Context.runOnClientThread(...)`. Reusable steps belong behind `Task` / `AbstractTask` / `PriorityTask` / `RunnableTask` or a service, not inside plugin event handlers.

### Simulation — two separate engines

- `src/main/java/com/kraken/api/simulation/colosim/` — a Java port of the community Colosseum line-of-sight simulator, shipped in the published jar, with its own GUI.
- `src/test/java/plugins/colosseum/simulation/` — the real-time bit-packed Fortis Colosseum tick engine and budgeted `Planner` (waves 1–11). It was deliberately moved *out* of the published API into the test source set (commits `10129331`, `fa1d9bdd`); do not move it back. `docs/SIMULATION.md` documents its design, and `unit/com/kraken/api/simulation/colosseum/TickParityTest` parity-checks it against `colosim`.

### Test source layout

`src/test/java` mixes two unrelated things:
- `unit/**` — real JUnit 5 tests, the only thing `./gradlew test` runs.
- `plugins/**` — RuneLite plugins used as the manual harness and as reference examples for API consumers (`plugins/api/` is the canonical usage example; `plugins/api/tests/BaseApiTest` with `@Before`/`@After` is a hand-rolled in-client harness whose tests are registered in `ApiTestPlugin.registerTest(...)` and toggled from `ApiTestConfig`).

`build.gradle` adds `tests/sim` as an extra test srcDir (currently absent) and excludes `unit/plugins/colosseumv2/**` (orphaned tests for a removed plugin).

## Dependency rules

RuneLite, guice, guava, gson, slf4j, and lombok are `compileOnly` on purpose — RuneLite supplies them at runtime, and bundling them causes classloader/version conflicts. Only `org.benf:cfr` is shaded in. Do not promote a `compileOnly` dependency to `implementation`, and think hard before adding any new runtime dependency.

## Conventions

- Java 11 toolchain; Lombok is used where surrounding code already uses it.
- Descriptive names, no abbreviations. Search for an existing helper before writing a new one — refactor to generalize rather than duplicate.
- Never hand-edit `build/**` or `docs/kraken-api/**` (generated).
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
