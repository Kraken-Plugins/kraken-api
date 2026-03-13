//[kraken-api](../../../index.md)/[com.kraken.api.query](../index.md)/[InteractionManager](index.md)/[interact](interact.md)

# interact

[Kraken API]\
open fun [interact](interact.md)(npc: NPC, action: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html))

Interacts with an NPC using the specified action i.e. &quot;Attack&quot;, &quot;Talk-To&quot;, or &quot;Examine&quot;.

#### Parameters

Kraken API

| | |
|---|---|
| npc | the NPC to interact with |
| action | The action to take, &quot;Attack&quot;, &quot;Talk-To&quot;, or &quot;Examine&quot;. |

[Kraken API]\
open fun [interact](interact.md)(player: Player, action: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html))

Interacts with a Player using the specified action i.e. &quot;Attack&quot;, &quot;Trade&quot;, or &quot;Follow&quot;

#### Parameters

Kraken API

| | |
|---|---|
| player | the Player to interact with |
| action | The action to take, &quot;Attack&quot;, &quot;Trade&quot;, or &quot;Follow&quot; |

[Kraken API]\
open fun [interact](interact.md)(item: [ContainerItem](../../com.kraken.api.query.container/-container-item/index.md), action: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html))

Interacts with an item with the specified ID in an item container (inventory, inventory while banking, equipment, etc...) using the specified action. 

#### Parameters

Kraken API

| | |
|---|---|
| item | The Container Item to interact with. A container item is an item stored in a container like an inventory, a inventory while banking or the equipment interface. |
| action | The action to take. i.e. &quot;Eat&quot;, &quot;Remove&quot;, &quot;Wield&quot;, &quot;Wear&quot;, or &quot;Use&quot; |

[Kraken API]\
open fun [interact](interact.md)(item: [ContainerItem](../../com.kraken.api.query.container/-container-item/index.md), actions: [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)&gt;)

Interacts with an item with the specified ID in an item container (inventory, inventory while banking, equipment, etc...) using the first matching specified action. For example, passing &quot;wield&quot; and &quot;wear&quot; as actions would result in wielding weapons and wearing armor when invoked on the given container item. 

#### Parameters

Kraken API

| | |
|---|---|
| item | The Container Item to interact with. A container item is an item stored in a container like an inventory, a inventory while banking or the equipment interface. |
| actions | A variable number of actions to take. i.e. &quot;Eat&quot;, &quot;Remove&quot;, &quot;Wield&quot;, &quot;Wear&quot;, or &quot;Use&quot; The first action which matches the list of actions on the container item will be used. |

[Kraken API]\
open fun [interact](interact.md)(item: [BankItemWidget](../../com.kraken.api.query.container.bank/-bank-item-widget/index.md), action: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html))

Interacts with a widget in the players bank using the specific action.

#### Parameters

Kraken API

| | |
|---|---|
| item | The bank item widget to interact with |
| action | The action to take i.e. Withdraw-1, Withdraw-X, Examine |

[Kraken API]\
open fun [interact](interact.md)(item: Widget, action: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html))

Interacts with a widget using the specific action.

#### Parameters

Kraken API

| | |
|---|---|
| item | The widget to interact with |
| action | The action to take i.e. Wield, Use or Examine |

[Kraken API]\
open fun [interact](interact.md)(item: Widget, menu: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html), action: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html))

Interacts with a widget using the specific sub action.

#### Parameters

Kraken API

| | |
|---|---|
| item | The widget to interact with |
| menu | The menu to select |
| action | The action to take i.e. Wield, Use or Examine |

[Kraken API]\
open fun [interact](interact.md)(src: Widget, dest: Widget)

Uses a source widget on a destination widget (i.e. High Alchemy)

#### Parameters

Kraken API

| | |
|---|---|
| src | The source widget to use on the destination widget |
| dest | The destination widget |

[Kraken API]\
open fun [interact](interact.md)(action: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), packedWidgetId: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), childId: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), itemId: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html))

Interacts with a widget using the specific action index

#### Parameters

Kraken API

| | |
|---|---|
| action | The action index to take |
| packedWidgetId | The packed widget id |
| childId | The child id of the widget to interact with |
| itemId | The item id of the widget to interact with |

[Kraken API]\
open fun [interact](interact.md)(src: Widget, npc: NPC)

Uses a source widget on a destination NPC (i.e. Crumble Undead spell on Vorkath Spawn)

#### Parameters

Kraken API

| | |
|---|---|
| src | The source widget to use on the destination widget |
| npc | The NPC to use the widget on |

[Kraken API]\
open fun [interact](interact.md)(src: Widget, gameObject: GameObject)

Uses a source widget on a destination Game Object (i.e. &quot;Bones&quot; on the &quot;Chaos Altar&quot;)

#### Parameters

Kraken API

| | |
|---|---|
| src | The source widget to use on the destination widget |
| gameObject | The Game Object to use the widget on |

[Kraken API]\
open fun [interact](interact.md)(object: TileObject, action: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html))

Interacts with a GameObject (`TileObject`) using the specified action i.e. &quot;Chop&quot;, &quot;Mine&quot;, or &quot;Examine&quot;. GameObject's are objects that exist on a tile like walls, trees, ore, or fishing spots.

#### Parameters

Kraken API

| | |
|---|---|
| object | the `TileObject` to interact with |
| action | The action to take on the game object, i.e. &quot;Chop&quot;, &quot;Mine&quot;, or &quot;Examine&quot;. |

[Kraken API]\
open fun [interact](interact.md)(item: [GroundItem](../../com.kraken.api.query.groundobject/-ground-item/index.md))

Interacts with a ground item (`GroundItem`) using the specified action i.e. &quot;Take&quot; or &quot;Examine&quot;. A Ground item is an actual item that is on the ground like coins dropped from a boss or logs a player has dropped on a tile. This differs from GameObjects like trees, ore, or fish which exist on a tile but are not &quot;takeable&quot; into the players inventory.

#### Parameters

Kraken API

| | |
|---|---|
| item | the `GroundItem` to interact with |
