//[kraken-api](../../index.md)/[com.kraken.api.simulation.colosseum.plan](index.md)

# Package-level declarations

## Types

| Name | Summary |
|---|---|
| [ColoDecision](-colo-decision/index.md) | [Kraken API]<br>class [ColoDecision](-colo-decision/index.md)<br>The planner's output for one tick: everything the executor should do right now, plus the evidence (scores, predicted path, reasoning) the debug overlay renders. |
| [ColoPlanner](-colo-planner/index.md) | [Kraken API]<br>class [ColoPlanner](-colo-planner/index.md)<br>Real-time, time-budgeted plan search over the colosseum tick engine. |
| [ColoScorer](-colo-scorer/index.md) | [Kraken API]<br>class [ColoScorer](-colo-scorer/index.md)<br>Scores a rolled-out future. |
| [DangerMap](-danger-map/index.md) | [Kraken API]<br>class [DangerMap](-danger-map/index.md)<br>Per-tile threat analysis around the player: for every walkable tile in a radius, which NPCs would have line of sight to it from where they stand now, and how much expected damage per tick that exposure represents. |
| [PlannerOptions](-planner-options/index.md) | [Kraken API]<br>class [PlannerOptions](-planner-options/index.md)<br>Tuning knobs for [ColoPlanner](-colo-planner/index.md): time budget, rollout horizon, candidate caps and the consumable policy thresholds used inside rollouts. |
