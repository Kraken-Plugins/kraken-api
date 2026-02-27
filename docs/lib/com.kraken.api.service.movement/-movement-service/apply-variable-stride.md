//[lib](../../../index.md)/[com.kraken.api.service.movement](../index.md)/[MovementService](index.md)/[applyVariableStride](apply-variable-stride.md)

# applyVariableStride

[Kraken API]\
open fun [applyVariableStride](apply-variable-stride.md)(densePath: [List](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/List.html)&lt;WorldPoint&gt;): [List](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/List.html)&lt;WorldPoint&gt;

Converts a dense path (every tile) into a strided path of &quot;waypoint&quot; tiles. Tiles are based around a variable stride configuration which computes a Gaussian distribution for the length of each stride to take.

#### Return

A list of WorldPoint representing the strided path.

#### Parameters

Kraken API

| | |
|---|---|
| densePath | The dense path with which to apply variable strides. |

[Kraken API]\
open fun [applyVariableStride](apply-variable-stride.md)(densePath: [List](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/List.html)&lt;WorldPoint&gt;, config: [VariableStrideConfig](../-variable-stride-config/index.md)): [List](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/List.html)&lt;WorldPoint&gt;

Converts a dense path (every tile) into a strided path of &quot;waypoint&quot; tiles. Tiles are based around a variable stride configuration which computes a Gaussian distribution for the length of each stride to take.

#### Return

A list of WorldPoint representing the strided path.

#### Parameters

Kraken API

| | |
|---|---|
| densePath | The dense path with which to apply variable strides. |
| config | A variable stride configuration where the mean, min, max, and std dev can be configured to produce unique strides when traversing paths. |
