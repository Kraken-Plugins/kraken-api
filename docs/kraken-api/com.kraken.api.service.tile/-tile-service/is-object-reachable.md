//[kraken-api](../../../index.md)/[com.kraken.api.service.tile](../index.md)/[TileService](index.md)/[isObjectReachable](is-object-reachable.md)

# isObjectReachable

[Kraken API]\
open fun [isObjectReachable](is-object-reachable.md)(obj: GameObject): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)

Checks if a GameObject is reachable. This considers the object's size and checks if the player can reach any tile touching the object's boundary (the &quot;Interactable Halo&quot;).

#### Return

true if the game object is reachable and false otherwise

#### Parameters

Kraken API

| | |
|---|---|
| obj | The game object to determine reachability for |
