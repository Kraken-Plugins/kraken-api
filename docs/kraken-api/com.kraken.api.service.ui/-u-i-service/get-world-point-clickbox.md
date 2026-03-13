//[kraken-api](../../../index.md)/[com.kraken.api.service.ui](../index.md)/[UIService](index.md)/[getWorldPointClickbox](get-world-point-clickbox.md)

# getWorldPointClickbox

[Kraken API]\
open fun [getWorldPointClickbox](get-world-point-clickbox.md)(worldPoint: WorldPoint): [Rectangle](https://docs.oracle.com/en/java/javase/11/docs/api/java.desktop/java/awt/Rectangle.html)

Gets the clickbox for a WorldPoint by converting it to screen coordinates. Creates a small rectangle around the point to allow for clicking.

#### Return

a small clickbox around the world point's screen location, or default rectangle if unavailable

#### Parameters

Kraken API

| | |
|---|---|
| worldPoint | the world point to get the clickbox for |
