//[kraken-api](../../../index.md)/[com.kraken.api.simulation.colosseum](../index.md)/[ColoGrid](index.md)/[fromCollisionFlags](from-collision-flags.md)

# fromCollisionFlags

[Kraken API]\
open fun [fromCollisionFlags](from-collision-flags.md)(flags: [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)&gt;&gt;, sceneOriginX: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), sceneOriginY: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), width: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), height: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), baseX: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), baseY: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), plane: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [ColoGrid](index.md)

Builds a grid from a RuneLite scene collision map.

#### Return

immutable grid.

#### Parameters

Kraken API

| | |
|---|---|
| flags | scene collision flags indexed as [sceneX][sceneY]. |
| sceneOriginX | scene x of the grid's south-west corner. |
| sceneOriginY | scene y of the grid's south-west corner. |
| width | grid width in tiles. |
| height | grid height in tiles. |
| baseX | world x of the grid's south-west corner. |
| baseY | world y of the grid's south-west corner. |
| plane | capture plane. |
