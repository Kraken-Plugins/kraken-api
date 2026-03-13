//[kraken-api](../../../index.md)/[com.kraken.api.input.mouse.strategy.replay](../index.md)/[PathLibrary](index.md)/[getSimilarPath](get-similar-path.md)

# getSimilarPath

[Kraken API]\
open fun [getSimilarPath](get-similar-path.md)(candidates: [List](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/List.html)&lt;[NormalizedPath](../../com.kraken.api.input.mouse.model/-normalized-path/index.md)&gt;, targetDistance: [Double](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-double/index.html)): [NormalizedPath](../../com.kraken.api.input.mouse.model/-normalized-path/index.md)

Selects a [NormalizedPath](../../com.kraken.api.input.mouse.model/-normalized-path/index.md) from a provided list of candidate paths that most closely matches a target distance, considering tolerance thresholds and randomization within a subset of viable matches. 

The method evaluates the &quot;fitness&quot; of each candidate based on the absolute difference between its original distance and the target distance. Viable paths are those whose distance is within a calculated tolerance of the target distance.

If no viable paths exist, a fallback strategy is employed to select randomly from the top three closest matches. Otherwise, a standard strategy randomly selects a path from the top 50% of the viable matches, ensuring some degree of diversity and non-determinism in the selection.

#### Return

a [NormalizedPath](../../com.kraken.api.input.mouse.model/-normalized-path/index.md) object selected as the best match to the target distance, or null if no candidates are provided in the list.

#### Parameters

Kraken API

| | |
|---|---|
| candidates | a list of [NormalizedPath](../../com.kraken.api.input.mouse.model/-normalized-path/index.md) objects representing potential matches. This list must not be null but can be empty, in which case the method will log a warning and return null. |
| targetDistance | the target distance to match with the candidates' original distance values. This value is used for calculating fitness and tolerance thresholds. |
