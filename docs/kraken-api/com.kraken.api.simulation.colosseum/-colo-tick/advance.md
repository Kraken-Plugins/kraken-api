//[kraken-api](../../../index.md)/[com.kraken.api.simulation.colosseum](../index.md)/[ColoTick](index.md)/[advance](advance.md)

# advance

[Kraken API]\
open fun [advance](advance.md)(s: [ColoState](../-colo-state/index.md), cmd: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html), scratch: [ColoScratch](../-colo-scratch/index.md))

Advances the state by one tick.

#### Parameters

Kraken API

| | |
|---|---|
| s | mutable state (modified in place). |
| cmd | packed player command for this tick (see [PlayerCommand](../-player-command/index.md)). |
| scratch | reusable working memory. |
