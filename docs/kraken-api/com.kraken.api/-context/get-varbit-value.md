//[kraken-api](../../../index.md)/[com.kraken.api](../index.md)/[Context](index.md)/[getVarbitValue](get-varbit-value.md)

# getVarbitValue

[Kraken API]\
open fun [getVarbitValue](get-varbit-value.md)(varbit: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)

Returns a varbit value from the RuneLite client. This method is thread-safe and runs on the client thread to retrieve the value.

#### Return

The varbit value (either 0 for false/unset or 1 for true/set).

#### Parameters

Kraken API

| | |
|---|---|
| varbit | The varbit value to retrieve. |
