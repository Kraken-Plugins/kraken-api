//[kraken-api](../../../index.md)/[com.kraken.api.simulation.snapshot](../index.md)/[SimulationPlayerSnapshot](index.md)

# SimulationPlayerSnapshot

[Kraken API]\
class [SimulationPlayerSnapshot](index.md)

Immutable player metadata included in a simulation snapshot.

## Constructors

| | |
|---|---|
| [SimulationPlayerSnapshot](-simulation-player-snapshot.md) | [Kraken API]<br>constructor(worldPoint: WorldPoint, hitpoints: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), maxHitpoints: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), activeProtectionPrayer: Prayer, inventoryItemQuantities: [Map](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/Map.html)&lt;[Integer](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/Integer.html), [Integer](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/Integer.html)&gt;, equippedItemIds: [Set](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/Set.html)&lt;[Integer](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/Integer.html)&gt;)<br>Creates immutable player combat/action metadata used by simulation actions. |

## Functions

| Name | Summary |
|---|---|
| [empty](empty.md) | [Kraken API]<br>open fun [empty](empty.md)(worldPoint: WorldPoint): [SimulationPlayerSnapshot](index.md)<br>Creates an empty player snapshot with no inventory items or equipped item ids. |
| [getWorldPoint](get-world-point.md) | [Kraken API]<br>open fun [getWorldPoint](get-world-point.md)(): WorldPoint |
