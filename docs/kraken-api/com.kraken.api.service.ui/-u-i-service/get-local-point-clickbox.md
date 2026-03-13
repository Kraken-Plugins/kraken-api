//[kraken-api](../../../index.md)/[com.kraken.api.service.ui](../index.md)/[UIService](index.md)/[getLocalPointClickbox](get-local-point-clickbox.md)

# getLocalPointClickbox

[Kraken API]\
open fun [getLocalPointClickbox](get-local-point-clickbox.md)(localPoint: LocalPoint): [Rectangle](https://docs.oracle.com/en/java/javase/11/docs/api/java.desktop/java/awt/Rectangle.html)

Gets the clickbox for a LocalPoint by converting it to screen coordinates. Creates a small rectangle around the point to allow for clicking.

#### Return

a small clickbox around the local point's screen location, or default rectangle if unavailable

#### Parameters

Kraken API

| | |
|---|---|
| localPoint | the local point to get the clickbox for |

[Kraken API]\
open fun [getLocalPointClickbox](get-local-point-clickbox.md)(localPoint: LocalPoint, plane: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [Rectangle](https://docs.oracle.com/en/java/javase/11/docs/api/java.desktop/java/awt/Rectangle.html)

Gets the clickbox for a LocalPoint at a specific plane by converting it to screen coordinates. Creates a small rectangle around the point to allow for clicking.

#### Return

a small clickbox around the local point's screen location, or default rectangle if unavailable

#### Parameters

Kraken API

| | |
|---|---|
| localPoint | the local point to get the clickbox for |
| plane | the plane/height level |
