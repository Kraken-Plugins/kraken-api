//[kraken-api](../../../index.md)/[com.kraken.api.simulation.colosseum.plan](../index.md)/[ColoScorer](index.md)

# ColoScorer

[Kraken API]\
class [ColoScorer](index.md)

Scores a rolled-out future. Larger is better. 

The score is survival-dominated: dying (or a worst-case burst that could have killed) outweighs any amount of kill progress, matching the priority requirement that eating and defence beat offence. Within survivable futures the scorer rewards kills and damage output, and mildly punishes supply and prayer expenditure so the planner does not burn resources without need.

## Constructors

| | |
|---|---|
| [ColoScorer](-colo-scorer.md) | [Kraken API]<br>constructor() |

## Functions

| Name | Summary |
|---|---|
| [defaults](defaults.md) | [Kraken API]<br>open fun [defaults](defaults.md)(): [ColoScorer](index.md) |
| [score](score.md) | [Kraken API]<br>open fun [score](score.md)(end: [ColoState](../../com.kraken.api.simulation.colosseum/-colo-state/index.md), endLosThreats: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [Double](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-double/index.html)<br>Scores a rollout end state. |
