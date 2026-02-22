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
