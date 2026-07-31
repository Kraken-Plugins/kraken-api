//[kraken-api](../../../index.md)/[com.kraken.api.simulation.colosseum](../index.md)/[ColoState](index.md)/[setPlayer](set-player.md)

# setPlayer

[Kraken API]\
open fun [setPlayer](set-player.md)(pos: [Short](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-short/index.html), hp: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), prayer: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), runEnergy: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), spec: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), overheadCode: [Byte](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-byte/index.html), gearSetIndex: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html))

Sets player vitals; used by the snapshot builder.

#### Parameters

Kraken API

| | |
|---|---|
| pos | packed player position. |
| hp | hitpoints. |
| prayer | prayer points. |
| runEnergy | run energy units 0-10000. |
| spec | special attack energy 0-100. |
| overheadCode | active overhead (OVERHEAD_*). |
| gearSetIndex | current gear set index. |
