//[kraken-api](../../../index.md)/[com.kraken.api.service.ui](../index.md)/[UIService](index.md)/[getClickbox](get-clickbox.md)

# getClickbox

[Kraken API]\
open fun [getClickbox](get-clickbox.md)(actor: Actor): [Point](https://docs.oracle.com/en/java/javase/11/docs/api/java.desktop/java/awt/Point.html)

Gets the clickbox for an Actor with randomization enabled by default.

#### Return

a randomized point within the actor's clickbox

#### Parameters

Kraken API

| | |
|---|---|
| actor | the actor to get the clickbox for |

[Kraken API]\
open fun [getClickbox](get-clickbox.md)(item: [ContainerItem](../../com.kraken.api.query.container/-container-item/index.md)): [Point](https://docs.oracle.com/en/java/javase/11/docs/api/java.desktop/java/awt/Point.html)

Gets the clickbox for an inventory item.

#### Return

The canvas point for the inventory items clickbox (randomizes the point).

#### Parameters

Kraken API

| | |
|---|---|
| item | The inventory item |

[Kraken API]\
open fun [getClickbox](get-clickbox.md)(item: [ContainerItem](../../com.kraken.api.query.container/-container-item/index.md), randomize: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)): [Point](https://docs.oracle.com/en/java/javase/11/docs/api/java.desktop/java/awt/Point.html)

Gets the clickbox for an inventory item with optional randomization

#### Return

Center point or random point within the bounds of the inventory item.

#### Parameters

Kraken API

| | |
|---|---|
| item | The item to get the clickbox for |
| randomize | True if the point should be randomized. If false it will return the center point. |

[Kraken API]\
open fun [getClickbox](get-clickbox.md)(actor: Actor, randomize: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)): [Point](https://docs.oracle.com/en/java/javase/11/docs/api/java.desktop/java/awt/Point.html)

Gets the clickbox for an Actor with optional randomization.

#### Return

a point within the actor's clickbox (randomized or centered)

#### Parameters

Kraken API

| | |
|---|---|
| actor | the actor to get the clickbox for |
| randomize | whether to randomize the point within the clickbox |

[Kraken API]\
open fun [getClickbox](get-clickbox.md)(object: TileObject): [Point](https://docs.oracle.com/en/java/javase/11/docs/api/java.desktop/java/awt/Point.html)

Gets the clickbox for a TileObject with randomization enabled by default.

#### Return

a randomized point within the object's clickbox

#### Parameters

Kraken API

| | |
|---|---|
| object | the tile object to get the clickbox for |

[Kraken API]\
open fun [getClickbox](get-clickbox.md)(object: TileObject, randomize: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)): [Point](https://docs.oracle.com/en/java/javase/11/docs/api/java.desktop/java/awt/Point.html)

Gets the clickbox for a TileObject with optional randomization.

#### Return

a point within the object's clickbox (randomized or centered)

#### Parameters

Kraken API

| | |
|---|---|
| object | the tile object to get the clickbox for |
| randomize | whether to randomize the point within the clickbox |

[Kraken API]\
open fun [getClickbox](get-clickbox.md)(tile: Tile): [Point](https://docs.oracle.com/en/java/javase/11/docs/api/java.desktop/java/awt/Point.html)

Gets the clickbox for a Tile with randomization enabled by default.

#### Return

a randomized point within the tile's clickbox

#### Parameters

Kraken API

| | |
|---|---|
| tile | the tile to get the clickbox for |

[Kraken API]\
open fun [getClickbox](get-clickbox.md)(tile: Tile, randomize: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)): [Point](https://docs.oracle.com/en/java/javase/11/docs/api/java.desktop/java/awt/Point.html)

Gets the clickbox for a Tile with optional randomization.

#### Return

a point within the tile's clickbox (randomized or centered)

#### Parameters

Kraken API

| | |
|---|---|
| tile | the tile to get the clickbox for |
| randomize | whether to randomize the point within the clickbox |

[Kraken API]\
open fun [getClickbox](get-clickbox.md)(widget: Widget): [Point](https://docs.oracle.com/en/java/javase/11/docs/api/java.desktop/java/awt/Point.html)

Gets the clickbox for a Widget with randomization enabled by default.

#### Return

a randomized point within the widget's clickbox

#### Parameters

Kraken API

| | |
|---|---|
| widget | the widget to get the clickbox for |

[Kraken API]\
open fun [getClickbox](get-clickbox.md)(widget: Widget, randomize: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)): [Point](https://docs.oracle.com/en/java/javase/11/docs/api/java.desktop/java/awt/Point.html)

Gets the clickbox for a Widget with optional randomization.

#### Return

a point within the widget's clickbox (randomized or centered)

#### Parameters

Kraken API

| | |
|---|---|
| widget | the widget to get the clickbox for |
| randomize | whether to randomize the point within the clickbox |

[Kraken API]\
open fun [getClickbox](get-clickbox.md)(worldPoint: WorldPoint): [Point](https://docs.oracle.com/en/java/javase/11/docs/api/java.desktop/java/awt/Point.html)

Gets the clickbox for a WorldPoint with randomization enabled by default.

#### Return

a randomized point within the world point's clickbox

#### Parameters

Kraken API

| | |
|---|---|
| worldPoint | the world point to get the clickbox for |

[Kraken API]\
open fun [getClickbox](get-clickbox.md)(worldPoint: WorldPoint, randomize: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)): [Point](https://docs.oracle.com/en/java/javase/11/docs/api/java.desktop/java/awt/Point.html)

Gets the clickbox for a WorldPoint with optional randomization.

#### Return

a point within the world point's clickbox (randomized or centered)

#### Parameters

Kraken API

| | |
|---|---|
| worldPoint | the world point to get the clickbox for |
| randomize | whether to randomize the point within the clickbox |

[Kraken API]\
open fun [getClickbox](get-clickbox.md)(localPoint: LocalPoint): [Point](https://docs.oracle.com/en/java/javase/11/docs/api/java.desktop/java/awt/Point.html)

Gets the clickbox for a LocalPoint with randomization enabled by default.

#### Return

a randomized point within the local point's clickbox

#### Parameters

Kraken API

| | |
|---|---|
| localPoint | the local point to get the clickbox for |

[Kraken API]\
open fun [getClickbox](get-clickbox.md)(localPoint: LocalPoint, randomize: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)): [Point](https://docs.oracle.com/en/java/javase/11/docs/api/java.desktop/java/awt/Point.html)

Gets the clickbox for a LocalPoint with optional randomization.

#### Return

a point within the local point's clickbox (randomized or centered)

#### Parameters

Kraken API

| | |
|---|---|
| localPoint | the local point to get the clickbox for |
| randomize | whether to randomize the point within the clickbox |

[Kraken API]\
open fun [getClickbox](get-clickbox.md)(localPoint: LocalPoint, plane: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [Point](https://docs.oracle.com/en/java/javase/11/docs/api/java.desktop/java/awt/Point.html)

Gets the clickbox for a LocalPoint at a specific plane with randomization enabled by default.

#### Return

a randomized point within the local point's clickbox

#### Parameters

Kraken API

| | |
|---|---|
| localPoint | the local point to get the clickbox for |
| plane | the plane/height level |

[Kraken API]\
open fun [getClickbox](get-clickbox.md)(localPoint: LocalPoint, plane: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), randomize: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)): [Point](https://docs.oracle.com/en/java/javase/11/docs/api/java.desktop/java/awt/Point.html)

Gets the clickbox for a LocalPoint at a specific plane with optional randomization.

#### Return

a point within the local point's clickbox (randomized or centered)

#### Parameters

Kraken API

| | |
|---|---|
| localPoint | the local point to get the clickbox for |
| plane | the plane/height level |
| randomize | whether to randomize the point within the clickbox |
