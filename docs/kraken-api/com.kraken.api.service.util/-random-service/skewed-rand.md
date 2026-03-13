//[kraken-api](../../../index.md)/[com.kraken.api.service.util](../index.md)/[RandomService](index.md)/[skewedRand](skewed-rand.md)

# skewedRand

[Kraken API]\
open fun [skewedRand](skewed-rand.md)(mode: [Double](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-double/index.html), lo: [Double](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-double/index.html), hi: [Double](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-double/index.html), cutoff: [Double](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-double/index.html)): [Double](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-double/index.html)

Generates a random number skewed towards the specified mode within a specified range. This allows for a biased distribution where the values tend to cluster around the mode.

#### Return

A random double value skewed towards the mode.

#### Parameters

Kraken API

| | |
|---|---|
| mode | The central value around which the distribution is skewed. |
| lo | The lower bound of the range. |
| hi | The upper bound of the range. |
| cutoff | The cutoff value to restrict extreme values. Defaults to GAUSS_CUTOFF(4) if less than or equal to 0. |

[Kraken API]\
open fun [skewedRand](skewed-rand.md)(mode: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html), lo: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html), hi: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html), cutoff: [Double](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-double/index.html)): [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html)

Generates a random long value skewed towards the specified mode within a specified range.

#### Return

A random long value skewed towards the mode.

#### Parameters

Kraken API

| | |
|---|---|
| mode | The central value around which the distribution is skewed. |
| lo | The lower bound of the range. |
| hi | The upper bound of the range. |
| cutoff | The cutoff value to restrict extreme values. |
