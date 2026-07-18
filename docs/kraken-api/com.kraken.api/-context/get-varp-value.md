//[kraken-api](../../../index.md)/[com.kraken.api](../index.md)/[Context](index.md)/[getVarpValue](get-varp-value.md)

# getVarpValue

[Kraken API]\
open fun [getVarpValue](get-varp-value.md)(varp: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)

Returns a var player value from the RuneLite client. These are values that the server controls. The client can pre-emptively update these values for the next server tick but will not be able to coerce the server into reconciling to a specific state. I.e. Client cannot change these values permanently. This method is thread-safe and runs on the client thread to retrieve the value.

#### Return

The varp value (either 0 for false/unset or 1 for true/set).

#### Parameters

Kraken API

| | |
|---|---|
| varp | The varp value to retrieve. |
