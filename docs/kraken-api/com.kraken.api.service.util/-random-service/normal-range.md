//[kraken-api](../../../index.md)/[com.kraken.api.service.util](../index.md)/[RandomService](index.md)/[normalRange](normal-range.md)

# normalRange

[Kraken API]\
open fun [normalRange](normal-range.md)(min: [Double](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-double/index.html), max: [Double](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-double/index.html), cutoff: [Double](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-double/index.html)): [Double](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-double/index.html)

Generates a random number within the specified range, biased towards the mean. The distribution has a higher likelihood of generating numbers closer to the midpoint of the range.

#### Return

A random double value within the specified range, biased towards the middle.

#### Parameters

Kraken API

| | |
|---|---|
| min | The minimum bound of the range. |
| max | The maximum bound of the range. |
| cutoff | The cutoff value to restrict extreme values. Defaults to GAUSS_CUTOFF(4) if less than or equal to 0. |

[Kraken API]\
open fun [normalRange](normal-range.md)(min: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html), max: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html), cutoff: [Double](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-double/index.html)): [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html)

Generates a random long value within the specified range, biased towards the mean.

#### Return

A random long value within the specified range, biased towards the middle.

#### Parameters

Kraken API

| | |
|---|---|
| min | The minimum bound of the range. |
| max | The maximum bound of the range. |
| cutoff | The cutoff value to restrict extreme values. Defaults to GAUSS_CUTOFF(4) if less than or equal to 0. |
