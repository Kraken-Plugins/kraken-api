# Simulation

The simulation module is a deterministic, allocation free tick model of an OSRS encounter: NPC movement, line of
sight, attack cadence, hits in flight, the player's movement, attacks, prayers and supplies. A plugin builds a
snapshot of the world, advances it tick by tick under candidate commands, and reads what happens. It holds
mechanics only; planners, scoring and decisions stay in the plugins.

The module is a Gradle subproject, `simulation/`, whose compile classpath holds Gson and Lombok and nothing else,
so an import of a RuneLite, Guice or Kraken type fails its build. The root project shades it into `kraken-api.jar`,
so plugins compile against `kraken-api` as before and the bootstrap needs no new artifact. The RuneLite side, which
captures snapshots and records traces from a live client, is `com.kraken.api.simulation.live` in the root project.

Encounter specific mechanics come from a content pack: data files plus a few classes that hook into the tick. The
core knows no encounter. The first pack is the Fortis Colosseum, owned by Auto Colosseum V2; a parity copy of it
lives in this repository's test tree (see [Tests and gates](#tests-and-gates)).

## Packages

| Package | What it holds |
|---|---|
| `geom` | `Coords` (12 bit packed tiles in a 64x64 region local window), `Grid` (movement and sight bitboards, eroded footprint masks), `Los` (the OSRS sight ray, NPC and player reach), `Footprints` |
| `world` | `SimState` (mutable, struct of arrays, pooled), `Frame` (immutable per snapshot identity), `Scratch` (per thread search memory), `PackFrame` |
| `move` | `MoveNaive` (the non route finding step that makes pillar stacking work), `MoveSmart` (route finding descent), `Routes` (short cached searches) |
| `hits` | `Hits` (the packed in flight hit and the two damage channels), `Style`, `DamageModifiers` |
| `player` | `Command` (one tick of player intent in a long), `PlayerTurn`, `Consumables` |
| `npc` | `NpcBehavior` and `PackRules`, the hooks a pack implements |
| `tick` | `Engine` (the pipeline), `TickContext` (what hooks see), `AttackListener`, `TickObserver` |
| `data` | `NpcDef`, `PlayerData`, `PipelineFlags`, `EquipSet`, `MechanicsData` (the loaded tables), `PackBinder`, `ContentPack` |
| `query` | `Threats.whoCanAttack(state, tile, ticksAhead)` |
| `trace` | the wave trace format: writer, reader, `SimTraceRecorder`, `WaveTraceReplayer`, `TraceDiff`, `StepFit`, `TraceRanges`, `TraceTool`, `TracePack` |

## The tick

`Engine.advanceInPlace(state, command)` simulates one server tick in the OSRS processing order: client input, then
NPCs in NPC index order, then the player. Identical (frame, state, command) gives an identical result, and the
steady state tick allocates nothing. The order, with the hook points:

| Phase | What happens |
|---|---|
| P0 | Per tick channels reset; in flight hits age; tile targeted hits due to pick their tile take the player's tile; `rules.onTickStart` |
| P1 | The command applies: overhead, gear, food then potion then karambwan, a move click or an attack click, the spec arm |
| P2 | Player hits land on NPCs (`rules.onNpcDamaged` for survivors, deaths handled with `onDeath` and `rules.onBodyDeleted`). Then each NPC: a dead one counts its corpse down (`onDeadTurn`); a living one runs `onTurnStart`, ticks its cooldown down, reads its spawn gates, then either takes its own `turn` or moves with the core and attacks (`attack`, by default the standard cadence attack). `rules.afterNpcTurns` when a hook set the tick's scratch |
| P3 | Hits land on the player (`rules.onTileHitLanded` for tile targeted ones), the player moves, ground contact (molten sand, then `rules.contact`), the player attacks |
| P4 | Timers, prayer drain, spec regen, venom, the accumulators, the `TickObserver`, the worst case hp floor, the wave clear check, `rules.endOfTick` |

Two damage channels run side by side. The expected channel books a share of an unprayed hit's max
(`PipelineFlags.unprayedExpectedFactor1000`, adjusted by the frame's `DamageModifiers`) and moves simulated hp; the
worst case channel adds every max hit to the tick's burst and keeps the worst hp floor, which a survival gate reads.
Prayer is read at launch, so a fully blocked launch never enters the ring; a hit marked check at land must be prayed
again when it lands.

## Writing a content pack

### Tables

A pack ships three core tables under one resource root, plus any files of its own:

- `npcs.json`: `{"npcs": [...], ...pack keys}`. Each entry holds the core fields of `NpcDef` (key, ids, size,
  attackRange, attackSpeed, moveSpeed, maxHit, rolls, hp, smartPath, style, hitDelay, prayerCheck, corpseTicks,
  passThrough, noBody, countsForWaveClear, attackAnims, defendAnims, gateMoveTick, src), `behavior` naming the pack
  behaviour the type binds to (`none` for the core's standard turn), and an optional block named after the pack
  holding its own settings, read through `NpcDef.block(name)`. The hit delay is `fixed` (with `hitDelayTicks`) or
  `projectile` (with `projectileBase`, `projectilePerTile`, `projectileMin`, `projectileOffset`: the hit lands
  `(max(min, base + perTile * d) + offset) / 30` ticks after launch).
- `player.json`: run energy, prayer drain, spec regen and venom constants, the consumable table, and the consumables
  a frame starts with (`defaultFood`, `defaultBrew`, `defaultRestore`).
- `pipeline.json`: the pipeline flags (spawn gates, the hazard and move order, the escape walk, the animation lag,
  the unprayed expected damage factor).

`MechanicsData.load(owner, "/root", binder)` reads them through `owner`'s class loader (a plugin's resources live in
the plugin's loader, not kraken-api's), then calls the `PackBinder`, which binds a behaviour to each type that needs
one (`data.bindBehavior(type, behavior)`) and returns the pack's `PackRules`. A `ContentPack` wraps the loaded tables
with its name and its `TracePack`.

### Behaviours

`NpcBehavior` is an abstract class; a pack extends it once per behaviour and overrides only the hooks it needs:
`onDeadTurn`, `onTurnStart` (return true to end the turn before the cooldown ticks down, for swarms and totems),
`turn` (the NPC's own movement and attack; return true and the core does neither), `attack` (replaces only the
attack, after the core's movement and sight), and `onDeath`. Binding records which hooks a behaviour overrides, and
the engine calls only those, so a type without a behaviour pays nothing.

Behaviours are stateless and shared by every NPC of their type. What they keep lives in the state:

- `SimState.npcLane[4][16]`: four byte lanes per NPC slot (a volley pattern, a special counter, a respawn clock).
- `SimState.FLAG_PACK_0` to `FLAG_PACK_3`: four flag bits per NPC.
- `SimState.packLane[4]`: world state (a hazard clock). Lanes start at zero, so give zero the meaning "off".
- `SimState.accLane[8]`: the pack's own accumulators, zeroed by `resetAccumulators`.
- `TickContext.tickScratch`: an int free within one tick, for a rule across the NPCs of the tick.

Each pack documents what it keeps where. A hook reads the tick through its `TickContext`: the state, frame and data,
the tick number, the player's start of tick tile, the current NPC's gates and sight, and the core's helpers
(`standardAttack`, `launchStyled`, `hitDelay`, `healNpc`, `damagePlayer`, `missedFlickRisk`, the listener
notifications). The context is reused every tick; never keep it. The player's tile is set when the NPC turns begin, so
`onTickStart` and the hooks run while the player's hits land on NPCs read `SimState.playerPos` instead; the gates and
sight belong to one NPC and mean something only in its own `turn` and `attack`.

### World rules

`PackRules` (also an abstract class) carries the hooks that span NPC types: moving hazards, ground contact, a damage
rule, body deletion, the pack's accumulators and its spawn schedule. `Frame.packHooks` names the per tick and per hit
hooks a frame needs (`HOOK_TICK_START`, `HOOK_CONTACT`, `HOOK_NPC_DAMAGED`, `HOOK_END_OF_TICK`); it starts with all
of them, and a pack clears a bit only for a frame on which that hook would do nothing. Every hook is a virtual call
the JIT may not inline into the tick, so this gating is worth doing.

### Frames

A frame holds what does not change within a snapshot: the grid, slot typing and NPC indexes (which decide the turn
order, `finishOrder`), player bases, gear sets, consumables, the `DamageModifiers` (an NPC max hit bonus, the unprayed
factor, prayer drained by damage, stacks per hit), the player's range penalty, the sand damage, a max hp penalty, and
the pack's own `PackFrame` part (`Frame.pack`). Builders fill the fields, call `finishPlayer`, `finishEquip` and
`finishOrder`, then treat the frame as read only.

## Using it from a plugin

```java
ContentPack pack = MyPack.load();                 // once
Frame frame = ...;                                 // built from a capture (see live below)
SimState base = ...;
Engine engine = new Engine();                      // one per search thread

engine.beginPass();                                // per search pass: path and route caches reset
SimState work = new SimState();
work.copyFrom(base);
work.resetAccumulators();
for (long command : plan) {
    engine.advanceInPlace(work, command);
}
// read work: hpWorstFloor, accExpectedDamageTaken, accKills, tickStyleMask, ...
```

`Threats.whoCanAttack(state, tile, ticksAhead)` lists the NPCs that could hit a tile from where they stand within a
number of ticks, for danger maps and prayer plugins. A planner adds its own per tick measures through
`engine.tickObserver` (set on every engine it uses; an engine without one leaves those measures at zero). The kill
value and threat weighted damage accumulators read the planner's weights from `MechanicsData.killValue` and
`damageWeight1000`, which stay 0 and 1000 until the planner fills them once after loading.

## The live layer

`com.kraken.api.simulation.live` holds the capture pieces every pack shares; each takes the pack's tables through
`MechanicsData`:

- `RegionAnchor`: the 64x64 window on a region's base (instanced arenas through template points).
- `CollisionCapture.capture(client, anchor)`: movement and sight boards from the scene's full tile collision flags on
  the current plane. Thin walls are not captured, and an arena that crosses a region boundary is cut off at it.
- `LiveTiles.serverTile(client, actor)`: the server tile, not the interpolated render position.
- `NpcIndexer`: stable game NPC index to slot mapping. For every slot `releaseUnseen` frees, clear it on each tracker
  too (`clearSlot`), or the next NPC in the slot inherits the old one's state.
- `AnimationStamper` with `CooldownTracker`: attack animations buffered on the client thread and folded at the next
  tick; defend animations never stamp; a pack `Hook` keeps its own animations (a charge) and marks prayer ignoring
  launches.
- `HpLedger`: hp from hitsplats, re anchored on the health bar.
- `CorpseTracker`: dead NPCs still in the scene captured as blocking bodies.
- `LiveTraceRecorder` with `JsonlFileSink`: one JSONL trace per wave, with the pack's columns.

## Wave traces

A trace is one JSON object per line: `meta`, `grid`, `npc` declarations, `tick` records and an `end`. Tick records
hold the player (`p`), every living NPC (`npcs`) and the tick's events (`ev`: `atk`, `patk`, `psplat`, `phit`,
`nsplat`, `nheal`, `heal`, `die`, `gone`, `jspec` for a tile targeted launch, `shadow` for its observed pick, `sand`,
`gap`). Schema 2 adds the meta's `pack` and each NPC entry's `lanes` (the four NPC lanes, omitted while zero); schema 1
traces read the same. A pack's own columns sit beside the core's on the same objects, written and read through its
`TracePack` (`writeMeta`, `writeTick`, `writePlayer`, `writeNpc`; on read every record keeps its raw object in
`columns`).

- `WaveTraceReplayer.replay(live, pack, out, name)` re runs a live trace through the engine. The player, NPC hp,
  spawns and deaths, and the pack's observed state (`TracePack.forceObserved`, `forceSpawn`, `forcePlayer`) are
  mirrored from the live trace; NPC movement, sight, launches, heals and the hits booked on the player are
  simulated.
- `TraceDiff.diff(a, b, data, pack, out)` compares two traces tick by tick and ends with a machine readable
  `summary:` line (position mismatches, exact, shifted, live only and engine only attacks). The pack adds its own
  channels (`diffChannels`, `diffAdvisory`) and can group attack events (a volley) through `attackGroup`.
- `TraceTool <pack class> replay|diff|check|stepfit|ranges ...` runs them from the command line.

## Tests and gates

`./gradlew :simulation:test` runs the module's tests:

- `core/CoreContractTest`: the hook contract on a two type pack written in the test.
- `colosseum/`: the Colosseum content pack, a parity copy of Auto Colosseum V2 3.0.0's Colosseum content carved onto
  the core (V2 owns it), its tables under `src/test/resources/colosseum/`, and V2's simulation tests ported onto it:
  combat, engine, modifiers, the colosim oracle parity tests, determinism and zero allocation.
- `colosseum/trace/RecordedWaveGateTest`: the seven live waves V2 recorded on 2026-08-25
  (`src/test/resources/colosseum/traces/`) replayed and diffed. The ceilings are V2 3.0.0's: at most 3 NPC position
  ticks wrong of 2,783, 10 shifted, 0 live only and 2 engine only attacks, at least 232 exact. A change may not raise
  them.
- `colosseum/ThroughputGateTest`: horizon 10 rollouts on V2's wave 8 throughput board, floor 700 per 15 ms.

`./gradlew :simulation:simulationBenchmark` runs the JMH benchmarks (`RolloutBenchmark`, `AdvanceTickBenchmark`,
in `src/jmh`, not in the jar) on the same board; rollouts per 15 ms deadline = 15,000,000 / ns per op.
