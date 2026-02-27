//[lib](../../../index.md)/[com.kraken.api.simulation.tree](../index.md)/[SimulationTreeOptions](index.md)/[SimulationTreeOptions](-simulation-tree-options.md)

# SimulationTreeOptions

[Kraken API]\
constructor(ticks: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), movementMode: [SimulationMovementMode](../../com.kraken.api.simulation/-simulation-movement-mode/index.md), movementRadius: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), includeWalkActions: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html), includeRunActions: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html), maxNodes: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), maxActionsPerNode: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), maxMovementTargets: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html))

Creates tree options.

#### Parameters

Kraken API

| | |
|---|---|
| ticks | simulation depth in game ticks. |
| movementMode | movement expansion mode. |
| movementRadius | movement radius used in [RADIUS](../../com.kraken.api.simulation/-simulation-movement-mode/-r-a-d-i-u-s/index.md). |
| includeWalkActions | true to include 1-step-per-tick movement destinations. |
| includeRunActions | true to include 2-steps-per-tick movement destinations. |
| maxNodes | hard cap for generated tree nodes. |
| maxActionsPerNode | hard cap for candidate actions per tree node. |
| maxMovementTargets | hard cap for movement destinations per node before walk/run variants. |
