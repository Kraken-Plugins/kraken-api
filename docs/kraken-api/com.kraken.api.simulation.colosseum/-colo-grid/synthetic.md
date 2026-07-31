//[kraken-api](../../../index.md)/[com.kraken.api.simulation.colosseum](../index.md)/[ColoGrid](index.md)/[synthetic](synthetic.md)

# synthetic

[Kraken API]\
open fun [synthetic](synthetic.md)(width: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), height: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), baseX: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), baseY: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), plane: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), blockedTiles: [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[Short](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-short/index.html)&gt;): [ColoGrid](index.md)

Builds a synthetic grid from explicit blocked tiles, used by tests and offline tooling. Every blocked tile blocks both movement and line of sight, matching colosseum obstacles.

#### Return

immutable grid.

#### Parameters

Kraken API

| | |
|---|---|
| width | grid width. |
| height | grid height. |
| baseX | world x of the south-west corner. |
| baseY | world y of the south-west corner. |
| plane | plane. |
| blockedTiles | packed local positions of fully blocked tiles. |
