//[kraken-api](../../../index.md)/[com.kraken.api.query.container.inventory](../index.md)/[InventoryEntity](index.md)/[useOn](use-on.md)

# useOn

[Kraken API]\
open fun [useOn](use-on.md)(other: [ContainerItem](../../com.kraken.api.query.container/-container-item/index.md)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)

Uses one item in the inventory on the other. This is a shallow wrapper around `combineWith()`

#### Return

True if the use on item was successful and false otherwise

#### Parameters

Kraken API

| | |
|---|---|
| other | The other inventory item to be used on. |

[Kraken API]\
open fun [useOn](use-on.md)(npc: NPC): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)

Uses one item in the inventory on an NPC.

#### Return

True if the use on item was successful and false otherwise

#### Parameters

Kraken API

| | |
|---|---|
| npc | The NPC to use the inventory item on. |

[Kraken API]\
open fun [useOn](use-on.md)(gameObject: GameObject): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)

Uses one item in the inventory on a Game object.

#### Return

True if the use on item was successful and false otherwise

#### Parameters

Kraken API

| | |
|---|---|
| gameObject | The GameObject to use the inventory item on. |
