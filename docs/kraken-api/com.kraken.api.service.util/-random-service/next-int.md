//[kraken-api](../../../index.md)/[com.kraken.api.service.util](../index.md)/[RandomService](index.md)/[nextInt](next-int.md)

# nextInt

[Kraken API]\
open fun [nextInt](next-int.md)(min: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), max: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), skewFactor: [Double](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-double/index.html), useGaussian: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)

Generates a random integer between min (inclusive) and max (inclusive) with options for skewing the distribution towards either the lower or higher bound.

#### Return

A random integer between min and max, possibly skewed based on the parameters.

#### Parameters

Kraken API

| | |
|---|---|
| min | The minimum value (inclusive). |
| max | The maximum value (inclusive). |
| skewFactor | The skew factor. A value greater than 1 will skew the distribution towards the higher end, while a value less than 1 will skew it towards the lower end. A value of 1 produces a standard Gaussian distribution centered around the midpoint. |
| useGaussian | If true, the method will use a Gaussian distribution instead of a uniform one. |
