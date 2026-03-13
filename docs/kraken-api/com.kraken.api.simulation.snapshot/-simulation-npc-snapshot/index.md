//[kraken-api](../../../index.md)/[com.kraken.api.simulation.snapshot](../index.md)/[SimulationNpcSnapshot](index.md)

# SimulationNpcSnapshot

[Kraken API]\
class [SimulationNpcSnapshot](index.md)

Immutable NPC position snapshot used as simulation input.

## Constructors

| | |
|---|---|
| [SimulationNpcSnapshot](-simulation-npc-snapshot.md) | [Kraken API]<br>constructor(index: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), id: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), size: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), worldPoint: WorldPoint)<br>Creates an npc snapshot.<br>constructor(index: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), id: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), size: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), packedWorldPoint: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), name: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html))<br>Creates an npc snapshot with packed coordinates. |

## Functions

| Name | Summary |
|---|---|
| [getWorldPoint](get-world-point.md) | [Kraken API]<br>open fun [getWorldPoint](get-world-point.md)(): WorldPoint |
