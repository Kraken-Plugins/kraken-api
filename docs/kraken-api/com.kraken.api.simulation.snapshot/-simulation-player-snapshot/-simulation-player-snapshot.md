//[kraken-api](../../../index.md)/[com.kraken.api.simulation.snapshot](../index.md)/[SimulationPlayerSnapshot](index.md)/[SimulationPlayerSnapshot](-simulation-player-snapshot.md)

# SimulationPlayerSnapshot

[Kraken API]\
constructor(worldPoint: WorldPoint, hitpoints: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), maxHitpoints: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), activeProtectionPrayer: Prayer, inventoryItemQuantities: [Map](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/Map.html)&lt;[Integer](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/Integer.html), [Integer](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/Integer.html)&gt;, equippedItemIds: [Set](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/Set.html)&lt;[Integer](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/Integer.html)&gt;)

Creates immutable player combat/action metadata used by simulation actions.

#### Parameters

Kraken API

| | |
|---|---|
| worldPoint | player world point at capture time. |
| hitpoints | player hitpoints at capture time. |
| maxHitpoints | player max hitpoints at capture time. |
| activeProtectionPrayer | active overhead protection prayer at capture time. |
| inventoryItemQuantities | stack size by item id for inventory items. |
| equippedItemIds | equipped item ids. |
