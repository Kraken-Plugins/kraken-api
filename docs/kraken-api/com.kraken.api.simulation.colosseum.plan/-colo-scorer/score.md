//[kraken-api](../../../index.md)/[com.kraken.api.simulation.colosseum.plan](../index.md)/[ColoScorer](index.md)/[score](score.md)

# score

[Kraken API]\
open fun [score](score.md)(end: [ColoState](../../com.kraken.api.simulation.colosseum/-colo-state/index.md), endLosThreats: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [Double](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-double/index.html)

Scores a rollout end state.

#### Return

score, larger is better.

#### Parameters

Kraken API

| | |
|---|---|
| end | state after the rollout horizon. |
| endLosThreats | NPCs with line of sight to the player in the end state. |
