//[kraken-api](../../../index.md)/[com.kraken.api.simulation.colosseum](../index.md)/[ColoGrid](index.md)/[isFootprintFree](is-footprint-free.md)

# isFootprintFree

[Kraken API]\
open fun [isFootprintFree](is-footprint-free.md)(x: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), y: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), size: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)

Checks whether a size-`size` footprint anchored south-west on `(x, y)` fits without touching blocked tiles or leaving the grid.

#### Return

true when the footprint placement is legal.

#### Parameters

Kraken API

| | |
|---|---|
| x | anchor local x. |
| y | anchor local y. |
| size | footprint size, 1-[MAX_NPC_SIZE](-m-a-x_-n-p-c_-s-i-z-e.md). |
