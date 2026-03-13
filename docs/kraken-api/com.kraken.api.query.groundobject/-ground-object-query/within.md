//[kraken-api](../../../index.md)/[com.kraken.api.query.groundobject](../index.md)/[GroundObjectQuery](index.md)/[within](within.md)

# within

[Kraken API]\
open fun [within](within.md)(anchor: WorldPoint, distance: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [GroundObjectQuery](index.md)

Filters for only objects whose location is within the specified distance from the anchor point.

#### Return

True if the object is within the specified distance from the anchor point, false otherwise.

#### Parameters

Kraken API

| | |
|---|---|
| anchor | The anchor local point. |
| distance | The maximum distance from the anchor point (in local units). |

[Kraken API]\
open fun [within](within.md)(distance: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [GroundObjectQuery](index.md)

Filters for only objects whose location is within the specified distance from the players current local point.

#### Return

True if the object is within the specified distance from the anchor point, false otherwise.

#### Parameters

Kraken API

| | |
|---|---|
| distance | The maximum distance from the anchor point (in world units). |
