# Fortis Colosseum Engine (`tests.plugins.colosseum.simulation`)

A colosseum-specific, real-time simulation and planning engine. It is purpose-built for
waves 1-11 of the Fortis Colosseum (Sol Heredit is out of scope) and is fast enough to 
evaluate thousands of candidate actions inside a single game tick.

## Design

- **Arena-local bit-packed state.** Positions pack into 12 bits (`x | y << 6`); the whole
  wave state (`State`) is primitive scalars plus small primitive arrays. Branch copies
  are `System.arraycopy`; the search loop allocates nothing after warmup. Measured:
  ~0.65 microseconds per simulated tick with 9 NPCs.
- **Bitmask collision.** `Grid` stores one 64-bit mask per row (movement + line of
  sight) with precomputed eroded masks per NPC footprint size, so large-NPC step legality
  is a single bit test. Built from the live collision map, anchored on the arena region.
- **Validated tick pipeline.** `Tick` replicates the engine order (client input, NPC
  turns, player turn) and is parity-tested against the community colosseum wave simulator
  (`simulation.colosim`): wave-start gates (no move tick 0, no LoS until tick 2, no attacks
  until tick 3), naive diagonal-then-cardinal chase stepping, manticore 10-tick charges
  with shared patterns, the one-triple-per-tick stagger (+5), per-orb prayer checks at
  launch, javelin sky specials every 5th attack (dodge by moving), minotaur heal scans
  (<75% HP, 7 tiles, LoS) with 1-tick-delayed tick-eatable melee, and warband route-finding
  on the fixed 6-tick cycle.
- **Player model.** Walk/run BFS-field movement, gear sets with per-NPC expected damage,
  attack cooldowns with real hit-delay formulas (+1 receiver rule), food/combo/potion
  timers with same-tick combos, prayer drain, run energy and spec regeneration.
- **Planner.** `Planner` searches macro plans (destination x target) under a hard
  wall-clock budget (default 15 ms), rolling each out with forced per-tick policies:
  oracle prayer flicking (predicts next-tick launches including step-into-LoS attackers and
  manticore orb sequences), threshold eating, retargeting and gear swaps, and attack-move
  weaving. Top plans get a second-stage follow-up destination. Scoring is
  survival-dominated (death and worst-case burst floors dwarf kill progress). Typical:
  150+ twelve-tick rollouts per decision in budget.

## Usage

```java
// Once per plugin:
WaveTracker tracker = new WaveTracker();      // feed RuneLite events into this
Planner planner = new Planner();
LoadoutConfig loadout = LoadoutConfig.defaults();     // tune gear sets + supplies

// Every game tick:
tracker.onGameTick();
Capture capture = Capture.capture(ctx, tracker, loadout, currentGearSet);
Decision decision = planner.plan(capture.getState(), PlannerOptions.defaults());
executor.execute(decision, loadout);                  // pray -> eat -> gear -> attack/move
```

`Decision` also carries visualization data: candidate tiles with scores, the predicted
player path, predicted end-of-horizon HP/worst-case floor, and a human-readable reasoning
string. See `../src/test/java/plugins/colosseum/AutoColosseumPlugin.java` for the full
debug plugin with scene + info overlays.

## Live tracking notes

- Set the manticore charge spot-anim ids on the tracker (plugin config) so patterns are
  known before the first triple; unknown patterns are planned conservatively (expected
  damage split across ranged/magic).
- All tracker estimates (cooldowns, orb counts, javelin specials) self-correct because the
  planner replans from a fresh capture every tick - this is also what adapts to
  reinforcements and other surprises.

## Tests

- `unit/com/kraken/api/simulation/colosseum/TickParityTest` - tick-for-tick parity
  with the community simulator scenarios (pillar stacks, manticore stagger, wave gates).
- `MechanicsTest` - prayer-at-launch timing, combo eating, sky javelin dodging,
  minotaur healing, warband route-finding, player movement/kills, per-orb flicking.
- `PlannerTest` - policy behaviours (emergency eating, pre-praying imminent
  launches) and the real-time budget.
- `PerfProbeTest` - writes throughput numbers to `build/colo-perf.txt`.
