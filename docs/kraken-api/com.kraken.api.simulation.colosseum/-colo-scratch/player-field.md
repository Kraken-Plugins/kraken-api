//[kraken-api](../../../index.md)/[com.kraken.api.simulation.colosseum](../index.md)/[ColoScratch](index.md)/[playerField](player-field.md)

# playerField

[Kraken API]\
open fun [playerField](player-field.md)(grid: [ColoGrid](../-colo-grid/index.md), dest: [Short](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-short/index.html)): [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[Byte](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-byte/index.html)&gt;

Returns the BFS distance field toward a player movement destination.

#### Return

distance field indexed by `y << 6 | x`; [UNREACHABLE](-u-n-r-e-a-c-h-a-b-l-e.md) where no path.

#### Parameters

Kraken API

| | |
|---|---|
| grid | collision grid. |
| dest | packed destination tile. |
