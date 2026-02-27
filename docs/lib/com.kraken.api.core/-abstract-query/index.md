//[lib](../../../index.md)/[com.kraken.api.core](../index.md)/[AbstractQuery](index.md)

# AbstractQuery

abstract class [AbstractQuery](index.md)&lt;[T](index.md) : [Interactable](../-interactable/index.md)&lt;[R](index.md)&gt;?, [Q](index.md) : [AbstractQuery](index.md)&lt;[T](index.md), [Q](index.md), [R](index.md)&gt;?, [R](index.md)&gt;

Forms the base class for all game client queries. This class defines generic actions which can be taken on streams of game objects like NPC's, Ground Items, Tile Objects, Players and Widgets.

#### Parameters

Kraken API

| | |
|---|---|
| &lt;T&gt; | The type of object being queried (e.g., NpcEntity, WidgetEntity) |
| &lt;Q&gt; | The concrete query class (e.g., NpcQuery) |
| &lt;R&gt; | The raw RuneLite type (NPC, Widget, TileObject, etc.) |

#### Inheritors

| |
|---|
| [BankInventoryQuery](../../com.kraken.api.query.container.bank/-bank-inventory-query/index.md) |
| [BankQuery](../../com.kraken.api.query.container.bank/-bank-query/index.md) |
| [InventoryQuery](../../com.kraken.api.query.container.inventory/-inventory-query/index.md) |
| [EquipmentQuery](../../com.kraken.api.query.equipment/-equipment-query/index.md) |
| [GameObjectQuery](../../com.kraken.api.query.gameobject/-game-object-query/index.md) |
| [GroundObjectQuery](../../com.kraken.api.query.groundobject/-ground-object-query/index.md) |
| [NpcQuery](../../com.kraken.api.query.npc/-npc-query/index.md) |
| [PlayerQuery](../../com.kraken.api.query.player/-player-query/index.md) |
| [WidgetQuery](../../com.kraken.api.query.widget/-widget-query/index.md) |
| [WorldQuery](../../com.kraken.api.query.world/-world-query/index.md) |

## Constructors

| | |
|---|---|
| [AbstractQuery](-abstract-query.md) | [Kraken API]<br>constructor(ctx: [Context](../../com.kraken.api/-context/index.md)) |

## Functions

| Name | Summary |
|---|---|
| [count](count.md) | [Kraken API]<br>open fun [count](count.md)(): [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html)<br>Returns a count of objects in the stream |
| [distinct](distinct.md) | [Kraken API]<br>open fun [distinct](distinct.md)(keyExtractor: ([T](index.md)) -&gt; [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-any/index.html)): [Q](index.md)<br>Filters the stream to only include elements that are distinct based on a property. |
| [empty](empty.md) | [Kraken API]<br>open fun [empty](empty.md)(): [Q](index.md)<br>Returns an empty stream. |
| [except](except.md) | [Kraken API]<br>open fun [except](except.md)(predicate: [Predicate](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/function/Predicate.html)&lt;[T](index.md)&gt;): [Q](index.md)<br>Filters out elements that match the given predicate. |
| [filter](filter.md) | [Kraken API]<br>open fun [filter](filter.md)(predicate: [Predicate](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/function/Predicate.html)&lt;[T](index.md)&gt;): [Q](index.md)<br>Applies a predicate to the stream to filter elements of the stream. |
| [first](first.md) | [Kraken API]<br>open fun [first](first.md)(): [T](index.md)<br>Returns the first type of object being queried (e.g., NpcEntity, WidgetEntity) from the stream. |
| [list](list.md) | [Kraken API]<br>open fun [list](list.md)(): [List](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/List.html)&lt;[T](index.md)&gt;<br>Returns the stream of entities as a list of objects |
| [map](map.md) | [Kraken API]<br>open fun [map](map.md)(): [Map](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/Map.html)&lt;[Integer](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/Integer.html), [T](index.md)&gt;<br>Collects the stream of entities into a map keyed by the id of the element in the map. |
| [nameContains](name-contains.md) | [Kraken API]<br>open fun [nameContains](name-contains.md)(name: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)): [Q](index.md)<br>Filters for entities whose name contains the substring or a portion of the name parameter. |
| [random](random.md) | [Kraken API]<br>open fun [random](random.md)(): [T](index.md)<br>Returns a random element from the filtered list. |
| [result](result.md) | [Kraken API]<br>open fun [result](result.md)(): [List](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/List.html)&lt;[T](index.md)&gt;<br>An alias for `list()`. |
| [reverse](reverse.md) | [Kraken API]<br>open fun [reverse](reverse.md)(): [Stream](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/stream/Stream.html)&lt;[T](index.md)&gt;<br>Reverses the order of elements in the stream and returns a new stream with the reversed order. |
| [shuffle](shuffle.md) | [Kraken API]<br>open fun [shuffle](shuffle.md)(): [Stream](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/stream/Stream.html)&lt;[T](index.md)&gt;<br>Randomizes the order of elements in the stream and returns a new stream with the shuffled elements. |
| [sorted](sorted.md) | [Kraken API]<br>open fun [sorted](sorted.md)(comparator: [Comparator](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/Comparator.html)&lt;[T](index.md)&gt;): [Q](index.md)<br>Applies a comparator to the stream for sorting elements within the stream. |
| [stream](stream.md) | [Kraken API]<br>open fun [stream](stream.md)(): [Stream](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/stream/Stream.html)&lt;[T](index.md)&gt;<br>Returns the raw stream of elements in the query so filters and matching can be manually applied. |
| [take](take.md) | [Kraken API]<br>open fun [take](take.md)(n: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [List](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/List.html)&lt;[T](index.md)&gt;<br>Takes the first N elements from the stream and returns them as a list. |
| [toRuneLite](to-rune-lite.md) | [Kraken API]<br>open fun [toRuneLite](to-rune-lite.md)(): [Stream](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/stream/Stream.html)&lt;[R](index.md)&gt;<br>Returns the underlying RuneLite entities that have been wrapped by the API. |
| [withId](with-id.md) | [Kraken API]<br>open fun [withId](with-id.md)(id: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [Q](index.md)<br>Filters the stream of game entities for ones where the ID matches a provided id |
| [withName](with-name.md) | [Kraken API]<br>open fun [withName](with-name.md)(name: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)): [Q](index.md)<br>Filters for only entities whose name matches the provided name. |
