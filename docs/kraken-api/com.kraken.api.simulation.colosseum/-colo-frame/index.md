//[kraken-api](../../../index.md)/[com.kraken.api.simulation.colosseum](../index.md)/[ColoFrame](index.md)

# ColoFrame

[Kraken API]\
class [ColoFrame](index.md)

Immutable per-decision context shared by every [ColoState](../-colo-state/index.md) branched from one snapshot: the collision grid, per-slot NPC identity data and the player loadout. 

Splitting the immutable identity data out of [ColoState](../-colo-state/index.md) keeps state copies down to a handful of small primitive-array copies; the frame itself is shared by reference across the whole search tree.

## Constructors

| | |
|---|---|
| [ColoFrame](-colo-frame.md) | [Kraken API]<br>constructor(grid: [ColoGrid](../-colo-grid/index.md), loadout: [LoadoutConfig](../-loadout-config/index.md), npcTypes: [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[ColoNpcType](../-colo-npc-type/index.md)&gt;, npcRuneliteIndices: [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)&gt;, npcMaxHp: [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[Short](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-short/index.html)&gt;, playerMaxHp: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), prayerPointCap: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), waveStartGates: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html), warbandCyclePhase: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), mantimayhemTier: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html))<br>Creates a frame. |

## Functions

| Name | Summary |
|---|---|
| [size](size.md) | [Kraken API]<br>open fun [size](size.md)(slot: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) |
| [slotByRuneliteIndex](slot-by-runelite-index.md) | [Kraken API]<br>open fun [slotByRuneliteIndex](slot-by-runelite-index.md)(runeliteIndex: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)<br>Finds a slot by RuneLite npc index. |
| [type](type.md) | [Kraken API]<br>open fun [type](type.md)(slot: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [ColoNpcType](../-colo-npc-type/index.md) |
