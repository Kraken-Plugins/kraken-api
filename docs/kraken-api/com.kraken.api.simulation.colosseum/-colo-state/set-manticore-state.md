//[kraken-api](../../../index.md)/[com.kraken.api.simulation.colosseum](../index.md)/[ColoState](index.md)/[setManticoreState](set-manticore-state.md)

# setManticoreState

[Kraken API]\
open fun [setManticoreState](set-manticore-state.md)(slot: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), patternCode: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), chargingStarted: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html), orbsRemaining: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html))

Marks a manticore slot's charge state and attack pattern (from live tracking).

#### Parameters

Kraken API

| | |
|---|---|
| slot | slot index. |
| patternCode | pattern code, 0 when unknown (see [ColoTick](../-colo-tick/index.md)). |
| chargingStarted | true when the charge has begun. |
| orbsRemaining | orbs still to be launched from an in-progress triple. |
