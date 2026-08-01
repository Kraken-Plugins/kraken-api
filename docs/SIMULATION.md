# Simulation

Kraken contains two related but separate pieces of simulation code:

| What | Where | Shipped in the published jar? |
|---|---|---|
| **Colosim port** — a Java port of the community [Colosseum line-of-sight simulator](https://los.colosim.com/), with its own Swing GUI (`ColoSimApp`) | `src/main/java/com/kraken/api/simulation/colosim/` | Yes |
| **Fortis Colosseum engine** — a real-time tick simulation, planner, and the *Auto Colosseum* debug plugin | `src/test/java/plugins/colosseum/` | No — test source set only |

The colosim port is a faithful reimplementation of a community tool that has been
validated against the live game. It is kept mainly as a *reference implementation*: the
Fortis Colosseum engine is parity-tested against it tick-for-tick (see
[Tests](#tests) below).

The rest of this page — and the linked deep dives — cover the Fortis Colosseum engine,
which is what powers the Auto Colosseum plugin. It was deliberately moved out of the
published API into the test source set; do not move it back.

## The problem it solves

Old School RuneScape advances in 0.6-second game ticks. In the Fortis Colosseum, every
tick you can do several things at once: switch an overhead protection prayer, eat food,
sip a potion, swap gear, click a tile to move, click an NPC to attack, and toggle a
special attack. Playing the Colosseum well means picking the right *combination* of those
actions, every tick, while several NPCs with different attack styles, ranges, and timers
are all converging on you.

The engine answers one question each tick: **"given everything on screen right now, what
is the best thing to do on the next tick?"** It does this by simulating the fight forward
in time — thousands of simulated ticks per real tick — and picking the plan whose
simulated future scores best. Survival is weighted far above everything else, so the
system eats and prays before it ever optimises damage output.

Scope: **waves 1–11 only**. Sol Heredit (wave 12's boss) is intentionally out of scope.

## How it works — the big picture

Four parts run in a loop, once per game tick:

```
 RuneLite events                 client state
 (spawns, animations)                 |
        |                             |
        v                             v
  +-------------+             +-------------+
  | WaveTracker |------------>|   Capture   |   1. OBSERVE + SNAPSHOT
  |  (memory)   |             | (snapshot)  |      freeze the fight into a
  +-------------+             +------+------+      compact simulation State
                                     |
                                     v
                              +-------------+
                              |   Planner   |   2. SEARCH
                              | (simulates  |      roll many candidate plans
                              |  futures)   |      forward ~12 ticks each,
                              +------+------+      score them, keep the best
                                     |
                                     v
                              +-------------+
                              |  Executor   |   3. ACT
                              | (clicks via |      pray -> eat -> gear ->
                              | Kraken API) |      attack or move
                              +-------------+
                                     |
                     next game tick: repeat from a fresh snapshot
```

- **[`WaveTracker`](../src/test/java/plugins/colosseum/simulation/live/WaveTracker.java)**
  is the system's memory. A single screenshot of the game can't tell you how long ago a
  manticore last fired; the tracker watches RuneLite events over time and keeps estimates
  (attack cooldowns, manticore charge patterns, the wave clock).
- **[`Capture`](../src/test/java/plugins/colosseum/simulation/live/Capture.java)** freezes
  the live game — arena collision, every NPC, the player's HP/prayer/supplies — into a
  compact, fast-to-copy simulation [`State`](../src/test/java/plugins/colosseum/simulation/State.java).
- **[`Planner`](../src/test/java/plugins/colosseum/simulation/plan/Planner.java)** searches
  over candidate plans ("move to that tile and attack that NPC") by rolling each one
  forward through the [`Tick`](../src/test/java/plugins/colosseum/simulation/Tick.java)
  engine under a hard time budget (15 ms by default), and returns a
  [`Decision`](../src/test/java/plugins/colosseum/simulation/plan/Decision.java).
- **[`Executor`](../src/test/java/plugins/colosseum/simulation/live/Executor.java)** turns
  the decision into real game actions through the Kraken API.

A key property: **everything is re-derived every tick.** Every estimate the tracker makes
can be slightly wrong, and it doesn't matter much, because the planner replans from a
fresh snapshot on the next tick. That is also what makes the system adapt to surprises —
reinforcement spawns, missed observations, lag.

## Package map

All paths are under `src/test/java/plugins/colosseum/`:

| Package / file | Role |
|---|---|
| `AutoColosseumPlugin.java` | The debug/automation RuneLite plugin gluing everything together |
| `AutoColosseumConfig.java` | Plugin config: budgets, thresholds, scoring weights, gear ids, overlay toggles |
| `overlay/` | Scene overlay (danger map, tile scores, predicted paths) and info panel |
| `simulation/` | The core engine: `State`, `Tick`, `Grid`, `LineOfSight`, `Coords`, `Frame`, `PlayerCommand`, `Scratch`, `NpcType`, `LoadoutConfig`, `Constants` |
| `simulation/plan/` | The search layer: `Planner`, `PlannerOptions`, `Scorer`, `DangerMap`, `Decision` |
| `simulation/live/` | The live-game bridge: `WaveTracker`, `Capture`, `Executor` |

## Deep dives

The details are split across three pages:

1. **[The tick engine](simulation/ENGINE.md)** — how the fight is modelled: the
   bit-packed state, the collision grid, line of sight, the exact order of one simulated
   tick, every NPC's behaviour, and the player model (movement, eating, prayer drain).
2. **[The planner](simulation/PLANNER.md)** — how the best action is found: candidate
   plans, the per-tick policies (prayer "oracle" flicking, threshold eating), two-stage
   search, and the survival-dominated scoring.
3. **[The live plugin](simulation/LIVE.md)** — how simulation meets the real game:
   `WaveTracker`, `Capture`, `Executor`, the Auto Colosseum plugin, its overlays, and a
   full config reference.

## Quick start

Using the engine from your own plugin looks like this (the Auto Colosseum plugin is the
complete, working version of exactly this pattern):

```java
// Once per plugin:
private final WaveTracker tracker = new WaveTracker();
private final Planner planner = new Planner();
private LoadoutConfig loadout = LoadoutConfig.defaults();   // tune gear + supplies

@Inject private Context ctx;
@Inject private Executor executor;   // Guice-injected (Context, PrayerService, MovementService)

// Feed RuneLite events into the tracker:
@Subscribe public void onNpcSpawned(NpcSpawned e)     { tracker.onNpcSpawned(e.getNpc()); }
@Subscribe public void onNpcDespawned(NpcDespawned e) { tracker.onNpcDespawned(e.getNpc()); }
@Subscribe public void onAnimationChanged(AnimationChanged e) {
    tracker.onAnimationChanged(e.getActor(), ctx.getClient().getLocalPlayer());
}

// Every game tick: observe -> snapshot -> plan -> act.
@Subscribe public void onGameTick(GameTick e) {
    tracker.onGameTick();
    Capture capture = Capture.capture(ctx, tracker, loadout, currentGearSet);
    if (capture == null || capture.getFrame().getNpcSlotCount() == 0) {
        return;   // no wave running
    }
    Decision decision = planner.plan(capture.getState(), PlannerOptions.defaults());
    executor.execute(decision, loadout);   // pray -> eat -> gear -> attack/move
}
```

`Decision` also carries visualization data — candidate tiles with scores, the predicted
player path, predicted end-of-horizon HP, and a human-readable reasoning string — which is
what the Auto Colosseum overlays render.

## Running the debug plugin

Run the main class `src/test/java/PluginRunnerTest.java` from your IDE with:

- Program arguments: `plugins.colosseum.AutoColosseumPlugin --developer-mode`
- VM argument: `-ea`

Then enable **Auto Colosseum** in the plugin list. By default it only *visualizes*: it
captures, plans, and draws its reasoning every tick. Turning on **Execute Simulation
Decisions** makes it actually perform the planned actions. See
[the live plugin page](simulation/LIVE.md) for setup (gear ids, thresholds) and what each
overlay element means.

## Accuracy and data sources

- NPC stats (HP, max hits, attack speed/range/style, footprint size) come from the OSRS
  Wiki monster pages, encoded in [`NpcType`](../src/test/java/plugins/colosseum/simulation/NpcType.java).
- Behaviour (manticore charge/stagger, wave-start gating, chase stepping, pillar
  stacking) is parity-modelled on the community colosim simulator and locked in by
  tick-for-tick parity tests.
- Anything that is an estimate is called out explicitly in
  [`Constants`](../src/test/java/plugins/colosseum/simulation/Constants.java) — e.g. the
  minotaur heal amount per cycle, and the 0.30 expected-damage factor for unprayed hits.
  The [engine page](simulation/ENGINE.md#model-simplifications) lists all known model
  simplifications.

## Tests

All tests live in `src/test/java/unit/com/kraken/api/simulation/colosseum/` and run with
the normal JUnit suite:

```bash
./gradlew test                                  # everything
./gradlew test --tests '*TickParityTest'        # one class
```

| Test | What it locks in |
|---|---|
| `TickParityTest` | Tick-for-tick parity with the community simulator on identical (coordinate-flipped) geometry: wave-start gates, manticore stagger and orb order, pillar-stacking attack ticks |
| `MechanicsTest` | Engine behaviours beyond colosim's scope: prayer-at-launch timing, combo eating, sky-javelin dodging, minotaur healing rules, warband route-finding, player movement and kill credit, per-orb manticore flicking |
| `PlannerTest` | Planner behaviour (emergency eating, pre-praying imminent launches, no redundant attack clicks) and the real-time budget |
| `PerfProbeTest` | Writes throughput numbers to `build/colo-perf.txt` (not an enforced bound) |
| `TestArenas` | Shared fixtures; rebuilds the colosim arena geometry, flipped from colosim's y-down screen coordinates into the engine's y-up world coordinates |
