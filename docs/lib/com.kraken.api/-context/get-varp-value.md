//[lib](../../../index.md)/[com.kraken.api](../index.md)/[Context](index.md)/[getVarpValue](get-varp-value.md)

# getVarpValue

[Kraken API]\
open fun [getVarpValue](get-varp-value.md)(varp: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)

Returns a var player value from the RuneLite client. This method is thread-safe and runs on the client thread to retrieve the value.

#### Return

The varp value (either 0 for false/unset or 1 for true/set).

#### Parameters

Kraken API

| | |
|---|---|
| varp | The varp value to retrieve. |
