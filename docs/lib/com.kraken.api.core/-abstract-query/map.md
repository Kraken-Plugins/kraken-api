//[lib](../../../index.md)/[com.kraken.api.core](../index.md)/[AbstractQuery](index.md)/[map](map.md)

# map

[Kraken API]\
open fun [map](map.md)(): [Map](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/Map.html)&lt;[Integer](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/Integer.html), [T](index.md)&gt;

Collects the stream of entities into a map keyed by the id of the element in the map. Generally this will be the item id for objects like `ContainerItem`, `EquipmentEntity`, and `GroundObjectEntity` but can take on other ids for things like Game objects, NPC's and widgets.

#### Return

Map of entities keyed by their id.
