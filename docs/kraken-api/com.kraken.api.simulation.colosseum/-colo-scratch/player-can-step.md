//[kraken-api](../../../index.md)/[com.kraken.api.simulation.colosseum](../index.md)/[ColoScratch](index.md)/[playerCanStep](player-can-step.md)

# playerCanStep

[Kraken API]\
open fun [playerCanStep](player-can-step.md)(grid: [ColoGrid](../-colo-grid/index.md), x: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), y: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), dx: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), dy: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)

Player single-step legality: destination tile must be free; diagonal steps additionally require both flanking cardinal tiles to be free (standard player movement rule).

#### Return

true when the player may take the step.

#### Parameters

Kraken API

| | |
|---|---|
| grid | collision grid. |
| x | from x. |
| y | from y. |
| dx | step x, -1..1. |
| dy | step y, -1..1. |
