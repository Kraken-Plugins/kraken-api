//[kraken-api](../../../index.md)/[com.kraken.api.input.mouse.strategy.replay](../index.md)/[PathNormalizer](index.md)/[normalize](normalize.md)

# normalize

[Kraken API]\
open fun [normalize](normalize.md)(raw: [MouseGesture](../../com.kraken.api.input.mouse.model/-mouse-gesture/index.md)): [NormalizedPath](../../com.kraken.api.input.mouse.model/-normalized-path/index.md)

Normalizes a @link MouseGesture into a unit-scaled @link NormalizedPath representation. 

 This method processes the raw motion data of a gesture to create a scaled, translated, and rotated form that aligns with the X-axis and maps all data points into a 0.0 to 1.0 range for spatial and temporal dimensions. The normalization ensures geometric and temporal consistency for gesture comparison. 

#### Return

A normalized @link NormalizedPath object containing: 

- The gesture's original label.
- The original distance and duration.
- A list of @link UnitPoint instances, each representing a normalized spatial and temporal data point.

If the gesture distance is near zero, `null` is returned.

#### Parameters

Kraken API

| | |
|---|---|
| raw | The @link MouseGesture instance containing raw input data to be normalized. Must include all point data, start and end coordinates, duration, and other metadata. If the input gesture has zero or near-zero distance, `null` is returned. |
