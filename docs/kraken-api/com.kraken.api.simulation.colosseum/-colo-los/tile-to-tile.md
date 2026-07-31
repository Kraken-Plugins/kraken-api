//[kraken-api](../../../index.md)/[com.kraken.api.simulation.colosseum](../index.md)/[ColoLos](index.md)/[tileToTile](tile-to-tile.md)

# tileToTile

[Kraken API]\
open fun [tileToTile](tile-to-tile.md)(grid: [ColoGrid](../-colo-grid/index.md), x0: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), y0: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), x1: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), y1: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)

Raycast between two tiles. Source tile blocking is ignored; every stepped tile including the destination must be clear of line-of-sight blockers.

#### Return

true when unobstructed.

#### Parameters

Kraken API

| | |
|---|---|
| grid | collision grid. |
| x0 | source local x. |
| y0 | source local y. |
| x1 | target local x. |
| y1 | target local y. |
