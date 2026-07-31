//[kraken-api](../../../index.md)/[com.kraken.api.simulation.colosseum](../index.md)/[ColoState](index.md)/[setCooldowns](set-cooldowns.md)

# setCooldowns

[Kraken API]\
open fun [setCooldowns](set-cooldowns.md)(food: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), combo: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), potion: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), attack: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html))

Sets consumption/attack cooldowns; used by the snapshot builder to carry live timers.

#### Parameters

Kraken API

| | |
|---|---|
| food | ticks until food can be eaten. |
| combo | ticks until combo food can be eaten. |
| potion | ticks until a potion can be sipped. |
| attack | ticks until the player can attack. |
