//[kraken-api](../../../index.md)/[com.kraken.api.simulation.tree](../index.md)/[SimulationTreeOptions](index.md)

# SimulationTreeOptions

[Kraken API]\
class [SimulationTreeOptions](index.md)

Controls simulation tree depth, movement expansion, and node limits.

## Constructors

| | |
|---|---|
| [SimulationTreeOptions](-simulation-tree-options.md) | [Kraken API]<br>constructor(ticks: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), movementMode: [SimulationMovementMode](../../com.kraken.api.simulation/-simulation-movement-mode/index.md), movementRadius: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), includeWalkActions: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html), includeRunActions: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html), maxNodes: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), maxActionsPerNode: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), maxMovementTargets: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html))<br>Creates tree options. |

## Functions

| Name | Summary |
|---|---|
| [defaults](defaults.md) | [Kraken API]<br>open fun [defaults](defaults.md)(): [SimulationTreeOptions](index.md)<br>Creates default tree options tuned for deeper future planning. |
