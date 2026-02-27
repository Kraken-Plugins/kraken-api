//[lib](../../../index.md)/[com.kraken.api.service.util](../index.md)/[RandomService](index.md)/[wait](wait.md)

# wait

[Kraken API]\
open fun [wait](wait.md)(min: [Double](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-double/index.html), max: [Double](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-double/index.html), weight: RandomService.EWaitDir)

Simulates a wait with a random duration, biased towards the mean, left, or right of the given range. This method is useful for introducing randomness in bot actions to reduce predictability.

#### Parameters

Kraken API

| | |
|---|---|
| min | The minimum wait time in milliseconds. |
| max | The maximum wait time in milliseconds. |
| weight | The direction of bias for the wait time (left, mean, or right skew). |

[Kraken API]\
open fun [wait](wait.md)(min: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), max: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html))

Simulates a wait with a random duration, biased towards the left side of the given range.

#### Parameters

Kraken API

| | |
|---|---|
| min | The minimum wait time in milliseconds. |
| max | The maximum wait time in milliseconds. |
