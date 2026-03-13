//[kraken-api](../../../index.md)/[com.kraken.api.core](../index.md)/[AbstractQuery](index.md)/[toRuneLite](to-rune-lite.md)

# toRuneLite

[Kraken API]\
open fun [toRuneLite](to-rune-lite.md)(): [Stream](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/stream/Stream.html)&lt;[R](index.md)&gt;

Returns the underlying RuneLite entities that have been wrapped by the API. For example: `ctx.gameObjects().toRuneLite()` returns a list of `TileObjects`. You will not be able to perform any interactions on these objects after calling `toRuneLite` as they lose their `Interactable` wrapping.

#### Return

Stream of RuneLite API objects
