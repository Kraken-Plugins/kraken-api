//[kraken-api](../../../../index.md)/[com.kraken.api.simulation.colosseum](../../index.md)/[ColoTick](../index.md)/[AttackListener](index.md)/[onAttack](on-attack.md)

# onAttack

[Kraken API]\
abstract fun [onAttack](on-attack.md)(slot: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), styleCode: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), tick: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), special: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html))

#### Parameters

Kraken API

| | |
|---|---|
| slot | attacking npc slot. |
| styleCode | launched style (STYLE_*; typeless for sky javelins/unknown orbs). |
| tick | engine tick of the launch. |
| special | true for manticore orbs and sky javelins. |
