# Agent Guide: Kraken API

## Purpose

- **Who this is for**: AI agents and developers working inside this repo.
- **What you get**: The minimum set of facts, files, and commands to navigate, modify, and run Kraken API locally.

### Document metadata

- Last updated: 2026-04-23
- Scope: Kraken API main library (`com.kraken.api`) and the bundled `shortest-path` subproject

### Maintenance (agents and contributors)

- If you change commands, file paths, Gradle tasks, environment variables, generated docs, or workflows in this repo, update this guide in the relevant sections.
- When you add or change generated files, update the `🚫 NEVER EDIT DIRECTLY (Generated files)` section with sources and regeneration commands.
- If you change packet mappings, reflection hooks, or client-version-sensitive code, update the packet/reflection maintenance notes and any affected docs under `docs/`.
- If you come across new common errors or fixes, extend `Common error patterns and quick fixes`.
- Always bump the `Last updated` date above when you make substantive changes.

### Code reuse policy (agents and contributors)

- Always reuse existing functions, helpers, and utilities before writing new code. Search the codebase for existing implementations that accomplish the same goal.
- Do not duplicate logic that already exists elsewhere in the repo. If a function, method, or pattern is already implemented, import and call it rather than reimplementing it.
- When adding new functionality, check related packages and modules for shared code that can be leveraged.
- If existing code needs slight modifications to be reusable, prefer refactoring the existing code to be more general over duplicating it with changes.
- Use descriptive variable and method names. Avoid abbreviations or single-letter names unless they are conventional loop variables or math symbols.

### Architectural boundary policy (agents and contributors)

- Keep `Context` lean. It should act as the DI-backed facade for core services, queries, packet initialization, and client-thread helpers, not a dumping ground for feature-specific logic.
- Respect the layer split between `service`, `query`, `input`, `packet`, `simulation`, and `core.script`. Put behavior in the narrowest layer that owns it.
- Keep packet and reflection logic localized. Do not spread obfuscated client lookups, packet method resolution, or runtime hook patching into unrelated services or queries.
- Preserve the service/query distinction. Use `service` for static or global game systems, and `query` for dynamic entities that are filtered and interacted with fluently.
- Keep `Script` orchestration separate from plugin wiring. Put reusable task logic behind `Task`, `AbstractTask`, or service classes instead of embedding everything in plugin event handlers.
- Do not mutate game client details directly from arbitrary layers when a dedicated helper already exists in `core.packet`, `core.interceptor`, or `service.util.reflect`.

### Testing policy (agents and contributors)

- Every new non-trivial function, method, or exported API must have accompanying unit tests before merging. Trivial helpers and glue code may be excluded when testing adds no meaningful value.
- All existing tests must pass locally before pushing changes. Run the relevant test suites listed in the [Local testing](#local-testing) and [Quick reference](#quick-reference) sections.
- When modifying existing functions, verify that existing tests still pass and add new test cases if the behavior changes.
- Do not submit changes that break existing tests. If a test failure is pre-existing and unrelated to your changes, note it explicitly in the PR description.

### Commit policy (agents and contributors)

- Always sign off on commits with `git commit -s` (adds a `Signed-off-by:` trailer).
- Never include AI agents as co-authors on commits. The human author is responsible for the work.

## Baseline architecture

- Start with `docs/API.md` for the service/query model, then `docs/INTERACTION.md` for packet-based interactions, and `docs/SCRIPTING.md` for script lifecycle and task orchestration.
- The main entry point for plugin consumers is `com.kraken.api.Context`.
- `Context` wires together:
  - query accessors for players, NPCs, objects, inventory, equipment, widgets, worlds, bank, and deposit box
  - high-level services for bank, dialogue, movement, camera, prayer, magic, UI, grand exchange, map, and utility behavior
  - packet initialization via `PacketMethodLocator`
  - runtime hooks and interceptors for packet/mouse behavior
  - client-thread helpers so callers do not need to manage RuneLite thread rules manually
- `Script` is the main long-running automation primitive. It handles lifecycle, game-tick execution, pause/resume, and break management.
- `shortest-path` is a separate included build that provides the pathfinding plugin and transport/pathing data used by the API.

## End-to-end flow

- **Plugin / Script**:
  - A RuneLite plugin injects `Context`, services, overlays, or a `Script`.
  - `Script.start()` registers the script on the event bus and begins tick-driven execution.
- **Context**:
  - `Context` exposes the query/service facade, client-thread execution helpers, and packet/interceptor setup.
  - `Context.initializePackets()` must run before packet sending features are used.
- **Query layer**:
  - `AbstractQuery` derivatives discover and filter dynamic entities such as NPCs, players, objects, widgets, inventory, equipment, bank, and worlds.
  - Wrapped entities expose `raw()` to reach the underlying RuneLite object when needed.
- **Service layer**:
  - Services handle static or global game systems such as bank, prayer, dialogue, camera, movement, magic, UI, and map/path utilities.
- **Packet / interaction layer**:
  - `PacketMethodLocator` resolves the obfuscated client packet-sending method.
  - `PacketFactory`, `PacketClient`, and the entity packet helpers build and send actions to the client.
  - `InteractionManager` and the resolver classes map high-level interactions to the right packet operations.
- **Runtime hooks**:
  - `packets.json` and `ObfuscatedNames.java` keep obfuscated client lookups aligned with the current RuneLite/client revision.
  - Interceptors patch runtime behavior where needed, but they should stay localized and guarded.
- **Execution**:
  - The same API supports direct plugin use, scripted automation, and the `shortest-path` plugin for movement/pathfinding support.

## AI integration and plugin authoring

This section is the working guide for AI systems and humans asking AI systems to generate, explain, or modify Kraken API plugin code.

### Source of truth

- For API mechanics and package layout, use this file first, then `docs/API.md`, `docs/INTERACTION.md`, `docs/SCRIPTING.md`, and `docs/TESTS.md`.
- Working examples live under `src/test/java/plugins/api/`.
- `docs/ai-integration.md` is a short redirect to this guide so we do not maintain two different AI-facing docs.

### Standards for AI-generated changes

- Prefer the smallest possible change that reuses existing helpers, services, and query/entity wrappers.
- Do not invent new abstraction layers when a service or query already exists.
- Keep behavior in the narrowest owning layer.
- Preserve the naming and patterns already used in the surrounding package.
- Add or update tests for any non-trivial behavior change.
- Avoid touching generated files unless you are regenerating them from source.
- State assumptions explicitly when the API or client behavior is ambiguous.

### Mental model of the API

- `Context` is the main entry point injected into plugins and scripts.
- `query` is for dynamic entities in the world: NPCs, players, game objects, ground items, inventory, bank, equipment, widgets, and worlds.
- `service` is for global or static systems: bank control, movement, prayer, magic, dialogue, camera, UI, GE, and related helpers.
- Queries are fluent filters that end in selection with `first()`, `nearest()`, `take()`, `list()`, or similar terminal operations.
- Entity wrappers expose actions such as `interact()`, `attack()`, `take()`, `withdraw()`, `depositOne()`, `wield()`, `wear()`, and `logout()`.
- Thread-sensitive work is handled by `Context.runOnClientThread(...)`, so query and service use is safe from normal plugin callbacks.
- All queryable entities support the `raw()` method which will return the underlying RuneLite API object for the corresponding entity. i.e. `ctx.npcs().first().raw()` will return RuneLite's `NPC` object.

### Plugin authoring pattern

- Declare a RuneLite plugin with `@PluginDescriptor`.
- Inject `Context` with `@Inject`.
- Use event callbacks such as `GameTick`, `GameStateChanged`, `ConfigChanged`, or menu events to drive behavior.
- Use `Script` when the work is long-running or stateful automation rather than a single event handler.
- Call `Context.initializePackets()` before using packet-driven interactions or hooks that depend on packet metadata.
- Keep reusable automation behind services, `Task`, `AbstractTask`, or `Script` instead of embedding it in plugin event handlers.

### Working examples

1. Attack an NPC:

```java
@PluginDescriptor(name = "Example", description = "Example plugin")
public class ExamplePlugin extends Plugin {
    @Inject
    private Context ctx;

    @Subscribe
    private void onGameTick(GameTick event) {
        NpcEntity goblin = ctx.npcs().withName("Goblin").nearest();
        if (goblin == null || goblin.isNull()) {
            return;
        }

        goblin.interact("Attack");
    }
}
```

2. Withdraw from the bank:

```java
if (!ctx.bank().isOpen()) {
    GameObjectEntity bankBooth = ctx.gameObjects().withName("Bank booth").nearest();
    if (bankBooth != null) {
        bankBooth.interact("Bank");
    }
}

BankEntity lobsters = ctx.bank().withName("Lobster").first();
if (lobsters != null) {
    lobsters.withdraw(10);
}
```

3. Use a service directly:

```java
MovementService movement = ctx.getService(MovementService.class);
movement.moveTo(new WorldPoint(x, y, 0));
```

4. Pick up a ground item:

```java
GroundObjectEntity bones = ctx.groundItems().withName("Bones").within(5).nearest();
if (bones != null) {
    bones.take();
}
```

### Common mapping rules for AI tools

- Start from `ctx.<domain>()`, not from raw client objects, unless you need `raw()` for an explicit edge case.
- Filter before you select: `withName`, `withId`, `nameContains`, `within`, `reachable`, `alive`, and similar query methods.
- Use `.nearest()` or `.first()` only when the code needs a single target.
- Use `bankInventory()` only while the bank interface is open, and you are depositing items from the inventory to the bank.
- Use `bank()` only while the bank interface is open and you are withdrawing items from the bank into your inventory.
- Use `inventory()` for ordinary inventory work and `depositBox()` for deposit box actions.
- Prefer dedicated entity methods over generic `interact()` when a helper already exists.

### How to explain the API to users

- Tell users to think in terms of "find with a query" and "act with a service or entity method".
- For dynamic targets, show a chained query example.
- For global systems, show a service call or a dedicated query wrapper.
- Point users to `src/test/java/plugins/api/` for runnable examples and `docs/API.md` for the full service/query breakdown.

## Packages and naming

- Main source root: `src/main/java/com/kraken/api/`
- Core package groups:
  - `core` for script, packet, interaction, and interceptor infrastructure
  - `query` for fluent entity queries and wrappers
  - `service` for higher-level game actions and system helpers
  - `input` for mouse and keyboard handling
  - `overlay` for reusable RuneLite overlays
  - `simulation` for decision-making, snapshotting, and combat/path evaluation
  - `util` for shared math, random, string, and helper utilities
- Naming expectations:
  - Keep service names specific to the domain they own, for example `BankService`, `PrayerService`, `MovementService`.
  - Keep query/entity pairs parallel, for example `NpcQuery` with `NpcEntity`, `WorldQuery` with `WorldEntity`.
  - Prefer clear method names that match the RuneLite concept or the game action they represent.

## Local development setup

- Use Java 11. The Gradle toolchain is configured for Java 11.
- Use the Gradle wrapper from the repo root.
- Typical bootstrap:

```bash
./gradlew clean build
./gradlew publishToMavenLocal
```

- Use `VERSION` if you need a local artifact version override:

```bash
VERSION=1.0.0-SNAPSHOT-local ./gradlew clean build shadowJar
```

### Required CLI tools

- Java 11 JDK
- Git
- Gradle wrapper is included, so no system Gradle install is required
- RuneLite runtime dependencies are resolved from `https://repo.runelite.net`

## Local testing

- Root library tests:

```bash
./gradlew test
```

- Full root verification, including docs generation and shaded jar build:

```bash
./gradlew clean build shadowJar
```

- Shortest-path subproject tests:

```bash
cd shortest-path
./gradlew test
```

- Shortest-path coverage report:

```bash
cd shortest-path
./gradlew jacocoTestReport
```

Notes:

- `src/test/java/PluginRunnerTest.java` is the entry point for launching RuneLite with an API test plugin loaded.
- Many API behaviors only make sense inside a RuneLite client, so test results from plain unit tests are only part of the verification story.
- The `shortest-path` project has its own test suite and heap settings, so run it from that subdirectory when debugging its failures.

## Local execution

- Run the root API test plugin:

```bash
./gradlew runelite
```

- Run the `shortest-path` plugin:

```bash
cd shortest-path
./gradlew runelite
```

Notes:

- `PluginRunnerTest` installs the ByteBuddy agent before launching RuneLite so class reloading can work when available.
- If the launcher cannot install the agent, the client can still start, but reloading and some hook-based behavior will be degraded.
- If you need a different test plugin, run `PluginRunnerTest` from the IDE or adjust the Gradle task in `build.gradle` rather than layering extra `--args` on the existing task.

## Regenerate docs and derived outputs

- Kraken API reference docs are generated from `src/main/java` with Dokka and written to `docs/kraken-api/`.
- Run the root build or the Dokka task after API surface changes:

```bash
./gradlew dokkaGfm
```

- `./gradlew build` also regenerates the docs because the build depends on `dokkaGfm`.

### 🚫 NEVER EDIT DIRECTLY (Generated files)

The following files or directories are generated or derived; edit their sources and regenerate them instead:

- `docs/kraken-api/**`
  - Source: `src/main/java/com/kraken/api/**`
  - Generate: `./gradlew dokkaGfm` or `./gradlew build`
- `build/**`
  - Source: Gradle build outputs
  - Generate: `./gradlew build`, `./gradlew shadowJar`, or `./gradlew test`
- `shortest-path/build/**`
  - Source: `shortest-path/src/main/java/**`, `shortest-path/src/test/java/**`, and the subproject build
  - Generate: `cd shortest-path && ./gradlew build`

Do not treat these as hand-edited sources.

## Key paths and files

- Main API entry point: `src/main/java/com/kraken/api/Context.java`
- Script base classes: `src/main/java/com/kraken/api/core/script/`
- Packet layer: `src/main/java/com/kraken/api/core/packet/`
- Packet entity helpers: `src/main/java/com/kraken/api/core/packet/entity/`
- Interaction infrastructure: `src/main/java/com/kraken/api/core/interaction/`
- Query layer: `src/main/java/com/kraken/api/query/`
- Service layer: `src/main/java/com/kraken/api/service/`
- Global pathfinding service: `src/main/java/com/kraken/api/service/pathfinding/GlobalPathfinder.java`
- Input layer: `src/main/java/com/kraken/api/input/`
- Simulation layer: `src/main/java/com/kraken/api/simulation/`
- Utility helpers: `src/main/java/com/kraken/api/util/`
- Runtime mappings and packet definitions: `src/main/resources/{packets.json,map.dat}`
- API docs: `docs/API.md`, `docs/INTERACTION.md`, `docs/MOUSE.md`, `docs/SCRIPTING.md`, `docs/SIMULATION.md`, `docs/TESTS.md`, `docs/UPDATING.md`, `docs/UTILITIES.md`
- Generated reference docs: `docs/kraken-api/`
- Root test launcher: `src/test/java/PluginRunnerTest.java`
- Root API tests and examples: `src/test/java/plugins/api/`
- Simulation and plugin examples: `src/test/java/plugins/`
- Shortest-path plugin and tests: `shortest-path/src/main/java/`, `shortest-path/src/test/java/`
- GitHub workflows: `.github/workflows/`

## Documentation

- `docs/API.md` explains the service/query split and should be kept aligned with the code.
- `docs/INTERACTION.md` covers packet-based interaction and runtime hook behavior.
- `docs/MOUSE.md` covers mouse movement strategies.
- `docs/SCRIPTING.md` covers `Script`, `Task`, and break management.
- `docs/SIMULATION.md` covers the simulation engine and snapshot-based planning helpers.
- `docs/TESTS.md` documents the client-based test harness and environment requirements.
- `docs/UPDATING.md` is the reference for packet and reflection updates after client revisions.
- `docs/ai-integration.md` redirects here.

## CI/CD (GitHub Actions)

- Workflows live under `.github/workflows/`.
- The build workflow uses JDK 11, the Gradle wrapper, recursive submodules, and runs `./gradlew clean build shadowJar`.
- Release workflows publish the library artifact and should be treated as version-sensitive.
- If you change build inputs, generated docs, or release outputs, verify the workflow still matches the repo layout.

## Code style and formatting

- Follow the existing Java style in the surrounding package.
- Prefer small, focused classes over large monoliths.
- Keep package boundaries clean and avoid moving unrelated logic across layers.
- Use Lombok where the surrounding code already uses it, but do not add it just to avoid straightforward boilerplate in a small patch.

## Common agent workflows

- **Add or modify a query**:
  1. Update the relevant `query` package class and its entity wrapper if needed.
  2. Reuse existing `AbstractQuery` helpers before adding new filters or collection logic.
  3. Add or update tests under `src/test/java/plugins/api/tests/query/`.
- **Add or modify a service**:
  1. Update the service in `service/`.
  2. Keep packet or interaction details behind the service boundary.
  3. Add or update tests under `src/test/java/plugins/api/tests/service/`.
- **Update packet or reflection behavior**:
  1. Edit the packet or hook source.
  2. Verify `packets.json` and `ObfuscatedNames.java` still match the current client revision.
  3. Run the root build and launcher tests.
- **Update scripting behavior**:
  1. Edit `core.script` classes and any dependent services.
  2. Keep `Script` lifecycle changes explicit and covered by tests where practical.
- **Update shortest-path behavior**:
  1. Make changes in `shortest-path/`.
  2. Run the subproject tests and the RuneLite launcher if the plugin behavior changed.

## Quick reference

### Essential commands

- Build and test the root library: `./gradlew clean build`
- Run unit tests only: `./gradlew test`
- Build the shaded jar: `./gradlew shadowJar`
- Publish to local Maven: `./gradlew publishToMavenLocal`
- Run the RuneLite test launcher: `./gradlew runelite`
- Generate Dokka docs: `./gradlew dokkaGfm`
- Test the shortest-path subproject: `cd shortest-path && ./gradlew test`
- Run the shortest-path plugin: `cd shortest-path && ./gradlew runelite`

### Key environment variables

- `VERSION=...`: Overrides the published artifact version for local and CI builds
- `GITHUB_ACTOR` / `GITHUB_TOKEN`: Used by publishing tasks that target GitHub Packages

## Troubleshooting and pitfalls

- `Context.initializePackets()` must run before packet-based actions are expected to work.
- If packet interactions fail after a RuneLite or client revision update, check `PacketMethodLocator`, `ObfuscatedNames.java`, and the `reflectionHooks` / `loginHooks` sections in `packets.json` together.
- If `./gradlew runelite` launches but class reloading behavior is missing, verify the ByteBuddy agent installed successfully.
- If docs look stale, rerun `./gradlew dokkaGfm`.
- If `publishToMavenLocal` does not produce the expected artifact version, check the `VERSION` environment variable.
- If the `shortest-path` tests OOM or run slowly, run them from the subproject directory so its own heap settings apply.

### Common error patterns and quick fixes

- Gradle cannot resolve RuneLite classes: verify `https://repo.runelite.net` is reachable and that you are using the repo wrapper from the project root.
- Packet sending fails immediately after a client revision change: update the mapping sources first, then rebuild and retest.
- Runtime hooks behave inconsistently: make sure the `reflectionHooks` and `loginHooks` sections in `packets.json` match the current client revision and that the client has been restarted after changes.
- RuneLite launcher starts but `PluginRunnerTest` logs a ByteBuddy agent error: the API can still launch, but class reloading and some runtime patches will be limited.
- Generated docs changed unexpectedly: check whether a public signature change in `src/main/java` caused Dokka output updates.
