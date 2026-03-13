//[kraken-api](../../../index.md)/[com.kraken.api.service.util](../index.md)/[RandomService](index.md)/[truncatedGauss](truncated-gauss.md)

# truncatedGauss

[Kraken API]\
open fun [truncatedGauss](truncated-gauss.md)(left: [Double](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-double/index.html), right: [Double](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-double/index.html), cutoff: [Double](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-double/index.html)): [Double](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-double/index.html)

Generates a random number within the given range using a truncated Gaussian distribution. This ensures that the value is within the bounds of the left and right range.

#### Return

A random double value within the specified range.

#### Parameters

Kraken API

| | |
|---|---|
| left | The minimum bound of the range. |
| right | The maximum bound of the range. |
| cutoff | The cutoff value to restrict extreme values. Defaults to GAUSS_CUTOFF(4) if less than or equal to 0. |

[Kraken API]\
open fun [truncatedGauss](truncated-gauss.md)(left: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html), right: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html), cutoff: [Double](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-double/index.html)): [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html)

Generates a random long value within the given range using a truncated Gaussian distribution.

#### Return

A random long value within the specified range.

#### Parameters

Kraken API

| | |
|---|---|
| left | The minimum bound of the range. |
| right | The maximum bound of the range. |
| cutoff | The cutoff value to restrict extreme values. |
