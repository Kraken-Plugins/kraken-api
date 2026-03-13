//[kraken-api](../../../index.md)/[com.kraken.api.simulation.snapshot](../index.md)/[SimulationSnapshot](index.md)

# SimulationSnapshot

[Kraken API]\
class [SimulationSnapshot](index.md)

Immutable snapshot used as the input payload for simulation.

## Constructors

| | |
|---|---|
| [SimulationSnapshot](-simulation-snapshot.md) | [Kraken API]<br>constructor(gameTick: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), plane: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), baseX: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), baseY: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), collisionFlags: [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)&gt;&gt;, player: [SimulationPlayerSnapshot](../-simulation-player-snapshot/index.md), npcs: [List](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/List.html)&lt;[SimulationNpcSnapshot](../-simulation-npc-snapshot/index.md)&gt;)<br>Creates a snapshot from collision, player, and npc input. |

## Functions

| Name | Summary |
|---|---|
| [collisionFlagsUnsafe](collision-flags-unsafe.md) | [Kraken API]<br>open fun [collisionFlagsUnsafe](collision-flags-unsafe.md)(): [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)&gt;&gt; |
| [copyCollisionFlags](copy-collision-flags.md) | [Kraken API]<br>open fun [copyCollisionFlags](copy-collision-flags.md)(): [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)&gt;&gt; |
| [createState](create-state.md) | [Kraken API]<br>open fun [createState](create-state.md)(): [SimulationState](../../com.kraken.api.simulation/-simulation-state/index.md)<br>Creates a mutable root state using default npc profiles. |
| [getCollisionFlagAtScene](get-collision-flag-at-scene.md) | [Kraken API]<br>open fun [getCollisionFlagAtScene](get-collision-flag-at-scene.md)(sceneX: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), sceneY: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)<br>Reads collision flags by scene coordinate. |
| [getCollisionFlagAtWorld](get-collision-flag-at-world.md) | [Kraken API]<br>open fun [getCollisionFlagAtWorld](get-collision-flag-at-world.md)(worldX: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), worldY: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)<br>Reads collision flags by world coordinate. |
| [getPlayerWorldPoint](get-player-world-point.md) | [Kraken API]<br>open fun [getPlayerWorldPoint](get-player-world-point.md)(): WorldPoint |
| [getSceneHeight](get-scene-height.md) | [Kraken API]<br>open fun [getSceneHeight](get-scene-height.md)(): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) |
| [getSceneWidth](get-scene-width.md) | [Kraken API]<br>open fun [getSceneWidth](get-scene-width.md)(): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) |
| [isSceneInBounds](is-scene-in-bounds.md) | [Kraken API]<br>open fun [isSceneInBounds](is-scene-in-bounds.md)(sceneX: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), sceneY: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>Checks whether scene coordinates are in the captured collision area. |
| [isWorldInBounds](is-world-in-bounds.md) | [Kraken API]<br>open fun [isWorldInBounds](is-world-in-bounds.md)(worldX: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), worldY: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>Checks whether a world tile is in the captured collision area. |
