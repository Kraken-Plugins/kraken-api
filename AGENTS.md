# Agent Guide: Kraken API

## Purpose

- **Who this is for**: AI agents and developers working inside this repo.
- **What you get**: The minimum set of facts, files, and commands to navigate, modify, and run Kraken API locally.

### Document metadata

- Last updated: 2026-08-05
- Scope: Kraken API main library (`com.kraken.api`)

### Maintenance (agents and contributors)

- If you change commands, file paths, Gradle tasks, environment variables, generated docs, or workflows in this repo, update this guide in the relevant sections.
- When you add or change generated files, update the `NEVER EDIT DIRECTLY (Generated files)` section with sources and regeneration commands.
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
  - runtime hooks and interceptors for packet/mouse behavior
  - client-thread helpers so callers do not need to manage RuneLite thread rules manually
- `Script` is the main long-running automation primitive. It handles lifecycle, game-tick execution, pause/resume, and break management.

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

### Common mapping rules for AI tools

- Start from `ctx.<domain>()`, not from raw client objects, unless you need `raw()` for an explicit edge case.
- Filter before you select: `withName`, `withId`, `nameContains`, `within`, `reachable`, `alive`, and similar query methods.
- Use `.nearest()` or `.first()` only when the code needs a single target.
- Use `bankInventory()` only while the bank interface is open, and you are depositing items from the inventory to the bank.
- Use `bank()` only while the bank interface is open and you are withdrawing items from the bank into your inventory.
- Use `inventory()` for ordinary inventory work and `depositBox()` for deposit box actions.
- Use `shop()` only while a shop is open and you are reading or buying from its shelves, and `shopInventory()` only while a shop is open and you are selling to it.
- Prefer dedicated entity methods over generic `interact()` when a helper already exists.

## Packages and naming

- Main source root: `src/main/java/com/kraken/api/`
- Core package groups:
  - `core` for script, packet, interaction, and interceptor infrastructure
  - `query` for fluent entity queries and wrappers
  - `service` for higher-level game actions and system helpers
  - `input` for mouse and keyboard handling
  - `overlay` for reusable RuneLite overlays
  - `simulation` for the Java port of the community Colosseum line-of-sight simulator
    (`simulation.colosim`); the Fortis Colosseum real-time engine (bit-packed tick
    simulation, budgeted planner, live capture/executor) lives in the test source set
    under `plugins.colosseum.simulation` - see `docs/SIMULATION.md`
  - `util` for shared math, random, string, and helper utilities
- Naming expectations:
  - Keep service names specific to the domain they own, for example `BankService`, `PrayerService`, `MovementService`.
  - Keep query/entity pairs parallel, for example `NpcQuery` with `NpcEntity`, `WorldQuery` with `WorldEntity`.
  - Prefer clear method names that match the RuneLite concept or the game action they represent.

## Local development setup

- Use Java 11. The Gradle toolchain is configured for Java 11.
- Use the Gradle wrapper from the repo root.

### NEVER EDIT DIRECTLY (Generated files)

The following files or directories are generated or derived; edit their sources and regenerate them instead:

  - `docs/kraken-api/**`
  - Source: `src/main/java/com/kraken/api/**`
  - `build/**`
  - Source: Gradle build outputs

Do not treat these as hand-edited sources.

## Documentation

- `docs/API.md` explains the service/query split and should be kept aligned with the code.
- `docs/INTERACTION.md` covers packet-based interaction and runtime hook behavior.
- `docs/SHOPS.md` covers `ShopService`, shop price discovery, and buying/selling under limits.
- `docs/MOUSE.md` covers mouse movement strategies.
- `docs/SCRIPTING.md` covers `Script`, `Task`, and break management.
- `docs/SIMULATION.md` is the simulation overview; deep dives live in `docs/simulation/`
  (tick engine, planner, live plugin).
- `docs/TESTS.md` documents the client-based test harness and environment requirements.
- `docs/UPDATING.md` is the reference for packet and reflection updates after client revisions.
- `docs/ai-integration.md` redirects here.

## Code style and formatting

- Follow the existing Java style in the surrounding package.
- Prefer small, focused classes over large monoliths.
- Keep package boundaries clean and avoid moving unrelated logic across layers.
- Use Lombok where the surrounding code already uses it, but do not add it just to avoid straightforward boilerplate in a small patch.

### Key environment variables

- `VERSION=...`: Overrides the published artifact version for local and CI builds
- `GITHUB_ACTOR` / `GITHUB_TOKEN`: Used by publishing tasks that target GitHub Packages