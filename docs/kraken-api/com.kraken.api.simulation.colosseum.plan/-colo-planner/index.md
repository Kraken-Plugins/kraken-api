//[kraken-api](../../../index.md)/[com.kraken.api.simulation.colosseum.plan](../index.md)/[ColoPlanner](index.md)

# ColoPlanner

[Kraken API]\
class [ColoPlanner](index.md)

Real-time, time-budgeted plan search over the colosseum tick engine. 

The planner searches over **macro plans** - a movement destination paired with an attack target - and rolls each plan out for a fixed horizon with deterministic in-rollout policies handling the &quot;forced&quot; decisions every tick:

- **Prayer policy**: predicts which NPCs launch an attack next tick (cooldowns, manticore orb sequences, warband metronome, post-move line-of-sight gains) and prays against the largest expected damage - exactly what a strong flicker does.
- **Consumable policy**: eats/sips on HP and prayer thresholds, respecting the engine's food/combo/potion timers (food beats offence - survival is priced into the scorer, so a plan that skips a needed eat simply scores worse).
- **Retargeting policy**: when the plan's target dies mid-rollout, retargets by kill-order priority and swaps to the gear set with the best expected damage.

Candidate destinations combine the player's immediate neighbourhood, the nearest zero-exposure cover tiles from the [DangerMap](../-danger-map/index.md), and low-danger attack positions with line of sight to the primary target. The best first-stage plans then get a second stage of follow-up destinations (&quot;duck behind the pillar, then swing out&quot;), all under a hard wall-clock budget - the planner returns its best-so-far at the deadline.

Everything is deterministic and allocation-free after warmup: states are pooled, commands are packed longs, and rollouts reuse one scratch. A full pass is typically a few thousand simulated ticks, well inside a 15 ms budget.

## Constructors

| | |
|---|---|
| [ColoPlanner](-colo-planner.md) | [Kraken API]<br>constructor()<br>Creates a planner. |

## Functions

| Name | Summary |
|---|---|
| [dangerMap](danger-map.md) | [Kraken API]<br>open fun [dangerMap](danger-map.md)(): [DangerMap](../-danger-map/index.md) |
| [plan](plan.md) | [Kraken API]<br>open fun [plan](plan.md)(root: [ColoState](../../com.kraken.api.simulation.colosseum/-colo-state/index.md), options: [PlannerOptions](../-planner-options/index.md)): [ColoDecision](../-colo-decision/index.md)<br>Searches for the best command for the upcoming tick. |
