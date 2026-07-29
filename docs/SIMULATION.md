# Simulation Engine

The refactored simulation API is centered around four objects:

1. `SimulationSnapshot`: immutable player/NPC positions + collision map.
2. `SimulationScenario`: snapshot + `npcId -> SimulationNpcProfile` mapping.
3. `SimulationTreeOptions`: depth, movement expansion mode, and node/action limits.
4. `SimulationEngine`: tree generation and tick simulation.

Search is done with `DecisionTreeSearch` over the generated `SimulationTree`.

## Why This Design

- Snapshot input is compact and immutable.
- NPC behavior is explicit and controlled by a single mapping type (`SimulationNpcProfile`).
- Movement expansion supports both:
  - every reachable tile in radius (`RADIUS`)
  - every reachable tile for the remaining horizon (`REACHABLE`)
- World points are packed into ints in state/snapshot internals for fast copies.
- You can search deep trees (15+ ticks) with hard caps (`maxNodes`, `maxActionsPerNode`).

## Core Types

- `SimulationSnapshotService`: capture snapshot from live client.
- `SimulationSnapshot`: collision map + player snapshot + NPC snapshots.
- `SimulationNpcProfile`: NPC attack range/style/speed/max-hit/intelligent-pathing.
- `SimulationScenario`: snapshot + profile map.
- `SimulationTreeOptions`: tree depth and expansion controls.
- `SimulationEngine`: tick simulation and tree generation.
- `SimulationTree`: generated tree of outcomes.
- `DecisionTreeSearch`: finds the best root action from a tree.
- `SimulationDecisionAdapter`: converts best action into executable API steps.

## Snapshot + NPC Mapping

```java
SimulationSnapshot snapshot = SimulationSnapshotService.capture(
    new SimulationSnapshotService.CaptureOptions()
        .withNpcRadius(24)
);

Map<Integer, SimulationNpcProfile> npcProfiles = Map.of(
    415,  new SimulationNpcProfile(1,  NpcAttackStyle.MELEE,  4, 30, true),
    3129, new SimulationNpcProfile(10, NpcAttackStyle.MAGIC,  4, 20, true),
    3130, new SimulationNpcProfile(10, NpcAttackStyle.RANGED, 4, 22, true)
);

SimulationScenario scenario = new SimulationScenario(snapshot, npcProfiles);
```

`SimulationNpcProfile` fields are exactly the per-NPC mapping contract:

- `attackRange`
- `attackStyle` (`MELEE`, `RANGED`, `MAGIC`, `UNKNOWN`)
- `attackSpeed`
- `maxHit`
- `intelligentPathing`

`intelligentPathing = true` means the NPC uses collision-aware pathing and will route around obstacles.

## Build A Tree

```java
SimulationEngine engine = new SimulationEngine();

SimulationTreeOptions options = SimulationTreeOptions.defaults()
    .withTicks(18)                 // 15+ tick planning
    .withMovementMode(SimulationMovementMode.RADIUS)
    .withMovementRadius(7)
    .withMovementTypes(true, true) // walk + run
    .withMaxNodes(20000)
    .withMaxActionsPerNode(120)
    .withMaxMovementTargets(80);

SimulationTree tree = engine.generateOutcomeTree(
    scenario,
    options,
    (state, depthRemaining) -> List.of(
        SimulationAction.switchPrayer(engine.recommendProtectionPrayer(state))
    )
);
```

## Search The Tree

```java
DecisionTreeSearch search = new DecisionTreeSearch();

DecisionTreeSearch.Result result = search.search(
    tree,
    node -> {
        SimulationState state = node.getState();
        int los = engine.countNpcsWithLineOfSightToPlayer(state);
        int attacks = engine.countNpcsAbleToAttackPlayer(state);
        int unprotected = engine.countUnprotectedNpcThreats(state);
        return (state.getPlayerHitpoints() * 1.1)
            - (los * 18.0)
            - (attacks * 24.0)
            - (unprotected * 40.0);
    }
);
```

`result.getBestAction()` is directly actionable and can be:

- `MOVE` to a specific tile (including far tiles)
- prayer switch
- inventory click/eat
- gear equip
- spell cast
- npc interaction

## Execute The Best Action

```java
SimulationDecisionAdapter.ExecutableAction executable = decisionAdapter.adapt(
    result,
    rootState,
    new SimulationDecisionAdapter.AdaptOptions("Attack", 1, 10)
);

decisionAdapter.execute(
    executable,
    Set.of(
        SimulationDecisionAdapter.ExecutableStepType.MOVE,
        SimulationDecisionAdapter.ExecutableStepType.SWITCH_PRAYER
    )
);
```

## Example: Movement-Only Escape

```java
SimulationTreeOptions options = SimulationTreeOptions.defaults()
    .withTicks(16)
    .withMovementMode(SimulationMovementMode.REACHABLE)
    .withMaxNodes(15000)
    .withMaxActionsPerNode(120)
    .withMaxMovementTargets(80);

SimulationTree tree = engine.generateOutcomeTree(scenario, options, (s, d) -> List.of());
DecisionTreeSearch.Result result = search.search(
    tree,
    node -> {
        SimulationState s = node.getState();
        int threats = engine.countNpcsAbleToAttackPlayer(s);
        return -threats * 25.0;
    }
);
```

## Example: Prayer + Eat + Spell Hybrid

```java
SimulationTree tree = engine.generateOutcomeTree(
    scenario,
    options,
    (state, depthRemaining) -> {
        List<SimulationAction> actions = new ArrayList<>();
        Prayer recommended = engine.recommendProtectionPrayer(state);
        if (recommended != null && recommended != state.getActiveProtectionPrayer()) {
            actions.add(SimulationAction.switchPrayer(recommended));
        }
        if (state.getPlayerHitpoints() <= 45 && state.hasInventoryItem(385)) {
            actions.add(SimulationAction.eat(385, state.getFoodHealAmount(385)));
        }
        actions.add(SimulationAction.castSpell(Standard.WIND_STRIKE));
        return actions;
    }
);
```

## Example: Manual Snapshot Input (Without Live Capture)

```java
int[][] collision = new int[10][10];
WorldPoint playerPoint = new WorldPoint(3203, 3203, 0);

SimulationPlayerSnapshot player = new SimulationPlayerSnapshot(
    playerPoint,
    99,
    99,
    null,
    Map.of(385, 2),
    Set.of(),
    Map.of(385, 20)
);

SimulationSnapshot snapshot = new SimulationSnapshot(
    0,
    0,
    3200,
    3200,
    collision,
    player,
    List.of(
        new SimulationNpcSnapshot(1, 415, 1, new WorldPoint(3205, 3205, 0))
    )
);

SimulationScenario scenario = new SimulationScenario(
    snapshot,
    Map.of(415, new SimulationNpcProfile(1, NpcAttackStyle.MELEE, 4, 20, true))
);
```

## Performance Guidance

- Keep `maxNodes` bounded.
- Keep `maxActionsPerNode` and `maxMovementTargets` realistic.
- Use `RADIUS` for tighter local fights.
- Use `REACHABLE` when planning long repositioning routes.
- Prefer cheap scoring functions because they run for many nodes.

## Plugin Reference

See `lib/src/test/java/plugins/simulation/SimulationPlugin.java` for a complete tick loop:

- capture snapshot
- build NPC profile mapping
- generate tree
- search best action
- adapt to executable steps
- optionally execute

---

# Fortis Colosseum Engine (`simulation.colosseum`)

A colosseum-specific, real-time simulation and planning engine. Unlike the generic tree
engine above, it is purpose-built for waves 1-11 of the Fortis Colosseum (Sol Heredit is
out of scope) and is fast enough to evaluate thousands of candidate actions inside a
single game tick.

## Design

- **Arena-local bit-packed state.** Positions pack into 12 bits (`x | y << 6`); the whole
  wave state (`ColoState`) is primitive scalars plus small primitive arrays. Branch copies
  are `System.arraycopy`; the search loop allocates nothing after warmup. Measured:
  ~0.65 microseconds per simulated tick with 9 NPCs.
- **Bitmask collision.** `ColoGrid` stores one 64-bit mask per row (movement + line of
  sight) with precomputed eroded masks per NPC footprint size, so large-NPC step legality
  is a single bit test. Built from the live collision map, anchored on the arena region.
- **Validated tick pipeline.** `ColoTick` replicates the engine order (client input, NPC
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
- **Planner.** `ColoPlanner` searches macro plans (destination x target) under a hard
  wall-clock budget (default 15 ms), rolling each out with forced per-tick policies:
  oracle prayer flicking (predicts next-tick launches including step-into-LoS attackers and
  manticore orb sequences), threshold eating, retargeting and gear swaps, and attack-move
  weaving. Top plans get a second-stage follow-up destination. Scoring is
  survival-dominated (death and worst-case burst floors dwarf kill progress). Typical:
  150+ twelve-tick rollouts per decision in budget.

## Usage

```java
// Once per plugin:
ColoWaveTracker tracker = new ColoWaveTracker();      // feed RuneLite events into this
ColoPlanner planner = new ColoPlanner();
LoadoutConfig loadout = LoadoutConfig.defaults();     // tune gear sets + supplies

// Every game tick:
tracker.onGameTick();
ColoCapture capture = ColoCapture.capture(ctx, tracker, loadout, currentGearSet);
ColoDecision decision = planner.plan(capture.getState(), PlannerOptions.defaults());
executor.execute(decision, loadout);                  // pray -> eat -> gear -> attack/move
```

`ColoDecision` also carries visualization data: candidate tiles with scores, the predicted
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

- `unit/com/kraken/api/simulation/colosseum/ColoTickParityTest` - tick-for-tick parity
  with the community simulator scenarios (pillar stacks, manticore stagger, wave gates).
- `ColoMechanicsTest` - prayer-at-launch timing, combo eating, sky javelin dodging,
  minotaur healing, warband route-finding, player movement/kills, per-orb flicking.
- `ColoPlannerTest` - policy behaviours (emergency eating, pre-praying imminent
  launches) and the real-time budget.
- `ColoPerfProbeTest` - writes throughput numbers to `build/colo-perf.txt`.
