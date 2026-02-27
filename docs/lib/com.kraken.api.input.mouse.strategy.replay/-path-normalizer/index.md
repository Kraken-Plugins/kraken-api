//[lib](../../../index.md)/[com.kraken.api.input.mouse.strategy.replay](../index.md)/[PathNormalizer](index.md)

# PathNormalizer

[Kraken API]\
open class [PathNormalizer](index.md)

 The PathNormalizer class is responsible for standardizing a raw mouse gesture path into a normalized and reusable format. Normalized paths simplify comparisons of different gestures by transforming them into a generic dataset that operates in a normalized coordinate and time space. 

 This allows the resulting path to be device-independent and facilitates further analysis, validation, and categorization of gesture behavior. 

 The normalize() method returns a NormalizedPath, which encapsulates relevant metadata and the transformed points representing the gesture.

## Constructors

| | |
|---|---|
| [PathNormalizer](-path-normalizer.md) | [Kraken API]<br>constructor() |

## Functions

| Name | Summary |
|---|---|
| [normalize](normalize.md) | [Kraken API]<br>open fun [normalize](normalize.md)(raw: [MouseGesture](../../com.kraken.api.input.mouse.model/-mouse-gesture/index.md)): [NormalizedPath](../../com.kraken.api.input.mouse.model/-normalized-path/index.md)<br>Normalizes a @link MouseGesture into a unit-scaled @link NormalizedPath representation. |
