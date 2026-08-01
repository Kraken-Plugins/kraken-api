# The live plugin: Auto Colosseum

> Part of the [Fortis Colosseum simulation docs](../SIMULATION.md). Source:
> `src/test/java/plugins/colosseum/`. Builds on [the tick engine](ENGINE.md) and
> [the planner](PLANNER.md).

The simulation and planner are pure in-memory code — they know nothing about RuneLite
events or clicking. Three classes in `simulation/live/` bridge them to the real game, and
[`AutoColosseumPlugin`](../../src/test/java/plugins/colosseum/AutoColosseumPlugin.java)
wires everything into a RuneLite plugin with debug overlays.

## Running it

Run the main class `src/test/java/PluginRunnerTest.java` from your IDE with:

- Program arguments: `plugins.colosseum.AutoColosseumPlugin --developer-mode`
- VM argument: `-ea`

Enable **Auto Colosseum** in the plugin list. Out of the box it is *observe-only*: every
game tick it captures, plans, and draws its reasoning, but performs no actions. The
**Execute Simulation Decisions** toggle turns on actual execution.

## The per-tick loop

`onGameTick` in the plugin is the whole system in one method:

1. `tracker.onGameTick()` — advance the wave clock, refresh per-NPC observations.
2. Detect the current gear set from the equipped weapon (matched against the configured
   weapon ids).
3. `Capture.capture(...)` — snapshot the client into a `Frame` + `State`. If there is no
   capture or no tracked NPCs, clear the overlays and stop — no wave is running.
4. `planner.plan(...)` — run the budgeted search; keep the `Decision` for the overlays.
5. Predict NPC paths for the overlay (advance a *copy* of the captured state a few ticks
   with no player input, recording where each NPC steps).
6. If **Execute Simulation Decisions** is on, `executor.execute(decision, loadout)`.

Any exception is caught and shown in the info overlay rather than crashing the client.

Note that all of this — including the planner's 15 ms search — runs on the client thread
inside the game-tick handler. That is by design (capture must read client state on that
thread, and 15 ms fits comfortably in a 600 ms tick), but it is why the budget is a hard
deadline rather than a target.

## `WaveTracker` — the memory

A single snapshot cannot tell you how long ago a manticore fired or which orb pattern it
charged. [`WaveTracker`](../../src/test/java/plugins/colosseum/simulation/live/WaveTracker.java)
accumulates that over time from the events the plugin forwards to it:

| Event | What the tracker learns |
|---|---|
| `onNpcSpawned` | Registers colosseum NPCs (by id, via `NpcType.fromNpcId`). The first spawn into an empty arena starts the wave clock at tick 0; when the last NPC despawns the clock stops. |
| `onGameTick` | Advances the wave clock; reads each manticore's charge spot-animation to learn its orb pattern (ranged-first vs magic-first); updates last-known NPC HP from health bars. |
| `onAnimationChanged` | Treated as an attack launch (colosseum NPCs only animate when attacking or dying): records the tick for cooldown estimation, counts javelin attacks toward the every-5th special, and learns the warband's 6-tick cycle phase from observed warband attack ticks. The local player's animations feed a player attack-cooldown estimate. |

From those observations it derives the estimates `Capture` bakes into the snapshot:
per-NPC cooldowns, manticore charge state / pattern / orbs remaining mid-triple, javelin
autos since the last sky special, and the player's attack and food delays. Hosting
plugins can also call `onPlayerAte()` and `markWaveStart()` to tighten estimates from
inventory/chat events.

**Every value here is an estimate, and that is fine.** The planner replans from a fresh
capture every tick, so a wrong cooldown guess self-corrects within a tick or two — this
is the same property that absorbs reinforcement spawns and lag.

The manticore spot-animation ids are set by the plugin (`2683` = ranged-first, `2681` =
magic-first). Until a manticore's pattern has been observed, the planner treats its first
two orbs conservatively (see [the engine page](ENGINE.md#manticores-the-charged-triple-attack)),
and the info overlay flags the manticore in red.

## `Capture` — the snapshot

[`Capture.capture(ctx, tracker, loadout, currentGearSet)`](../../src/test/java/plugins/colosseum/simulation/live/Capture.java)
runs on the client thread and builds the immutable `Frame` + mutable `State` pair without
touching the game. It reads:

- **The grid:** a 64×64 collision window anchored on the player's current map region —
  the colosseum arena is exactly one region, so the whole arena fits the engine's 6-bit
  coordinate packing.
- **NPCs:** every tracked wave NPC on the player's plane inside the grid (up to 16),
  with position, last-known HP, cooldown estimate, and manticore/javelin extras from the
  tracker.
- **The player:** position, boosted HP and prayer, run energy, special energy, active
  overhead prayer, current gear set, and supply counts (matching the loadout's item ids
  against the inventory).
- **The current interaction:** if the player is already attacking a tracked NPC, that
  engagement is carried into the state so plans that keep fighting it don't issue a
  redundant attack click.

It returns `null` when there is nothing to capture (not logged in, no collision data,
player outside the grid).

## `Executor` — acting on a decision

[`Executor.execute(decision, loadout)`](../../src/test/java/plugins/colosseum/simulation/live/Executor.java)
performs the decision through the Kraken API on the client thread, in priority order —
most tick-critical first:

1. **Prayer** — activate the decided overhead (or deactivate all), via `PrayerService`.
2. **Consumables** — eat/sip by item id, via inventory interactions.
3. **Gear** — equip every item of the decided set (weapon ids first, so attack-speed
   changes land before armour).
4. **Special attack** toggle, if decided.
5. **Attack _or_ move** — mutually exclusive, mirroring both the engine model and the
   real client (a new click replaces the current interaction).

Two redundancy guards keep it from wasting clicks: an attack is skipped when the client
says the player is already interacting with that exact NPC, and a movement click is
skipped when the client is already pathing to that exact tile.

## Setting up gear sets

The loadout ships with three sets — melee, ranged, magic — with weakness-aware expected
damage per NPC type. What you must supply are the *item ids* to swap between:

1. Wear your full melee setup in game.
2. In the plugin config's **Gear** section, click **Copy Weapon** — the equipped weapon's
   id is copied to the clipboard. Paste it into **Melee weapon**.
3. Click **Copy Gear** — every other equipped item's ids are copied. Paste into **Melee
   gear**.
4. Repeat for the ranged and magic setups.

The weapon ids double as the *detector*: each tick, the plugin identifies which set the
player is currently wearing by matching the equipped weapon against the configured weapon
ids, and feeds that into the capture (attack range and speed depend on it).

Supply item ids (food, karambwan, brews, restores) come from `LoadoutConfig.defaults()`
— anglerfish, cooked karambwan, Saradomin brew and Super restore doses.

## Config reference

Group `krakenColosseum`, defined in
[`AutoColosseumConfig`](../../src/test/java/plugins/colosseum/AutoColosseumConfig.java).

### Simulation

| Item | Default | Meaning |
|---|---|---|
| Enable Simulation | on | Capture, plan and visualize every game tick |
| Execute Simulation Decisions | **off** | Actually perform the planned actions |
| Simulation Budget | 15 ms | Planner wall-clock deadline (2–40 ms) |
| Search Horizon | 12 ticks | First-stage rollout depth |
| S2 Search Horizon | 12 ticks | Second-stage rollout depth |
| S2 Top Plans | 12 | First-stage plans refined in stage two (engine caps this at 8) |
| Max Destinations / Safe Tiles / Attack Tiles | 24 / 8 / 6 | Candidate caps |
| Max Targets | 3 | Attack targets considered per destination |

### Simulation Decision

Direct pass-throughs to the [scorer weights](PLANNER.md#scoring): death penalty, lethal
risk per HP, HP weight, damage taken/dealt weights, kill weight, supply weight, prayer
weight, end-exposure weight, run-energy weight.

### Food & Potions

Thresholds for the in-rollout consumable policy: eat at 48 HP, combo-eat at 34 HP, brew
at 55 HP (when out of food), restore at 15 prayer.

### Gear

The copy buttons and the six weapon/gear id fields described
[above](#setting-up-gear-sets).

### Overlay

Toggles for each overlay element (below), the danger-map radius (12), and how many ticks
of NPC movement to predict (6).

There is also a **License Key** field at the top of the config; the debug plugin itself
does not check it.

## The overlays

**Scene overlay** ([`AutoColosseumSceneOverlay`](../../src/test/java/plugins/colosseum/overlay/AutoColosseumSceneOverlay.java))
draws on the game world:

- **Danger tiles** — red shading over every tile at least one NPC can currently hit,
  intensity scaled by expected damage per tick. Gaps in the red are cover.
- **Candidate tiles** — the planner's evaluated destinations on a green (best) to red
  (worst) scale, each labelled 0–100 (score normalized within this tick). The chosen
  destination gets a white border.
- **Planned path** — the winning plan's predicted player route in cyan.
- **NPC paths** — each NPC's predicted next steps in a per-NPC colour.

**Info panel** ([`AutoColosseumInfoOverlay`](../../src/test/java/plugins/colosseum/overlay/AutoColosseumInfoOverlay.java))
is the "why did it do that" view: wave tick, the decision's reasoning line, score,
rollouts and planning time (turns red when planning ran long), current and predicted HP,
the worst-case HP floor (red when a death was possible within the horizon), predicted
damage/kills, supply counts, and one line per manticore showing its observed pattern and
charge state — red while the pattern is still unknown.

## Notes and limitations

- The engine covers waves 1–11 only; Sol Heredit is out of scope.
- The Mantimayhem tier is currently captured as 0, so melee-first manticore patterns are
  never assumed live.
- Capture estimates the food and attack delays but assumes karambwan/potion delays of
  zero at snapshot time; like all tracker estimates, errors self-correct on the next
  tick's replan.
- The plugin lives in the test source set and is not part of the published jar — it is a
  debug harness and reference consumer for the engine, in the same spirit as
  `plugins/api/` for the query/service API.
