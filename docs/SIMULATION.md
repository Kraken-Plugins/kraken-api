# Simulation Engine

The API provides two simulation areas:

- `com.kraken.api.sim` and `com.kraken.api.sim.colosim`: existing simulation/visualizer tooling, including Colosseum-specific examples.
- `com.kraken.api.simulation`: generic, RuneLite-compatible simulation for decision-tree search and in-game action execution.

This is intended for plugins that need to evaluate many candidate actions per game tick (0.6s) and choose an actionable outcome.

![sim-example-image](../images/sim.png)

## Generic Simulation Package

The generic simulation package is `com.kraken.api.simulation`.

Core classes:

- `SimulationSnapshotService`: captures immutable snapshots directly from live RuneLite state.
- `SimulationSnapshot`: immutable snapshot model (RuneLite `WorldPoint`, local collision flags, base coords, plane, NPCs).
- `SimulationState`: mutable state optimized for fast copying/branching (`copy()`).
- `SimulationEngine`: simulates ticks, movement, collision, and line-of-sight.
- `DecisionTreeSearch`: depth-limited tree search over candidate player actions.
- `SimulationDecisionAdapter`: converts tree-search result into executable movement + interaction actions.

### RuneLite Compatibility

Snapshot and simulation data stay in RuneLite-compatible coordinate space:

- Uses `WorldPoint` for entities/decisions.
- Uses copied RuneLite local collision flags (`CollisionData` flags).
- Uses `baseX/baseY/plane` from the active `WorldView`.

This allows direct translation from simulated decisions back to in-game packets/interactions.

## Quick Start (Programmatic)

```java
import com.kraken.api.simulation.*;

// 1) Capture snapshot from live game state
SimulationSnapshot snapshot = SimulationSnapshotService.capture(24); // NPC radius

// 2) Create mutable state + engine
SimulationEngine engine = new SimulationEngine();
SimulationState root = snapshot.createState();

// 3) Run decision search
DecisionTreeSearch search = new DecisionTreeSearch(engine, 4000); // max nodes
DecisionTreeSearch.Result result = search.search(
    root,
    2, // depth
    (state, depthRemaining) -> SimulationAction.standardWalkActions(),
    state -> {
        int threats = engine.countNpcsWithLineOfSightToPlayer(state);
        return -threats * 25.0;
    }
);

// 4) Optionally inspect resulting state
SimulationState bestAfterOneTick = engine.simulateTickCopy(root, result.getBestAction());
```

## Action Adapter (Simulation -> Executable Game Action)

Use `SimulationDecisionAdapter` to translate search result into real movement and optional NPC interaction.

```java
import com.kraken.api.simulation.*;

// Injected singleton in your plugin/service:
// @Inject private SimulationDecisionAdapter decisionAdapter;

SimulationDecisionAdapter.ExecutableAction executable = decisionAdapter.adapt(
    result,
    root,
    "Attack", // null to disable interactions
    1         // interaction distance
);

// Execute movement + optional interaction on client thread
decisionAdapter.execute(executable);
```

What the adapter does:

- Converts chosen `SimulationAction` into destination `WorldPoint` movement.
- Optionally selects an NPC target by simulated proximity.
- Executes via `MovementService` and NPC interaction APIs.

## RuneLite Plugin Example

A full plugin using this simulation is included:

- `com.kraken.api.simulation.plugin.SimulationPlugin`
- `com.kraken.api.simulation.plugin.SimulationPluginConfig`
- `com.kraken.api.simulation.plugin.SimulationSceneOverlay`
- `com.kraken.api.simulation.plugin.SimulationInfoOverlay`

Plugin features:

- Captures a fresh simulation snapshot each game tick.
- Runs decision-tree search and computes best action.
- Visualizes best tile, predicted NPC paths, and optional NPC LoS tiles.
- Optional automatic execution of adapted movement/interaction action.

### Plugin Config Highlights

- Search: snapshot radius, depth, max nodes, include run actions.
- Execution: auto-execute best action, interaction action text, interaction distance.
- Overlay: scene/info overlays, best move tile, path/LoS visualization options.

## Running the Plugin

In this repository setup, you can launch RuneLite with the bundled plugin classes using the existing test harness (`ExamplePluginTest`) and then enable the plugin in RuneLite:

1. Run the main test harness class that launches RuneLite (see `docs/TESTS.md`).
2. In RuneLite plugin list, enable `Kraken Simulation Sandbox`.
3. Configure search depth/node cap and overlay options.
4. Optionally enable `Auto Execute Best Action` once overlays/search output looks correct.

For consumption in your own plugin repo, include this API as a dependency and wire the same simulation classes in your plugin loop.

## Performance Notes

- Keep search depth and node cap bounded (`depth 1-3`, moderate `maxNodes`) for stable per-tick runtime.
- Restrict snapshot NPC radius and overlay NPC counts.
- Prefer immutable snapshot + mutable state copies (already provided) for branch expansion.

## ColoSim

`com.kraken.api.sim.colosim` is a Java port of the Colosseum LoS simulator and remains useful as a domain-specific reference:

- [OSRS Colosseum LoS Simulator](https://los.colosim.com/)

It is intentionally specialized; use `com.kraken.api.simulation` for generic content. 
