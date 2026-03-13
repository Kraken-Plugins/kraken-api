//[kraken-api](../../../index.md)/[com.kraken.api.simulation.snapshot](../index.md)/[SimulationSnapshot](index.md)/[SimulationSnapshot](-simulation-snapshot.md)

# SimulationSnapshot

[Kraken API]\
constructor(gameTick: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), plane: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), baseX: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), baseY: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), collisionFlags: [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)&gt;&gt;, player: [SimulationPlayerSnapshot](../-simulation-player-snapshot/index.md), npcs: [List](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/List.html)&lt;[SimulationNpcSnapshot](../-simulation-npc-snapshot/index.md)&gt;)

Creates a snapshot from collision, player, and npc input.

#### Parameters

Kraken API

| | |
|---|---|
| gameTick | client game tick at capture. |
| plane | current plane. |
| baseX | world base x for the collision array. |
| baseY | world base y for the collision array. |
| collisionFlags | scene collision map indexed as [sceneX][sceneY]. |
| player | player snapshot. |
| npcs | npc position snapshots. |
