//[kraken-api](../../../index.md)/[com.kraken.api.simulation.colosseum](../index.md)/[ColoFrame](index.md)/[ColoFrame](-colo-frame.md)

# ColoFrame

[Kraken API]\
constructor(grid: [ColoGrid](../-colo-grid/index.md), loadout: [LoadoutConfig](../-loadout-config/index.md), npcTypes: [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[ColoNpcType](../-colo-npc-type/index.md)&gt;, npcRuneliteIndices: [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)&gt;, npcMaxHp: [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[Short](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-short/index.html)&gt;, playerMaxHp: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), prayerPointCap: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), waveStartGates: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html), warbandCyclePhase: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), mantimayhemTier: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html))

Creates a frame.

#### Parameters

Kraken API

| | |
|---|---|
| grid | collision grid. |
| loadout | player loadout. |
| npcTypes | per-slot npc types. |
| npcRuneliteIndices | per-slot RuneLite npc indices. |
| npcMaxHp | per-slot max hitpoints. |
| playerMaxHp | player max hitpoints. |
| prayerPointCap | player max prayer points. |
| waveStartGates | true when tick 0 is the wave start (movement/LoS/attack gating). |
| warbandCyclePhase | warband 6-tick cycle phase. |
| mantimayhemTier | mantimayhem modifier tier, 0 when absent. |
