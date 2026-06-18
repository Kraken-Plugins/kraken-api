//[kraken-api](../../../index.md)/[com.kraken.api.core.interaction](../index.md)/[InteractionManager](index.md)/[interact](interact.md)

# interact

[Kraken API]\
open fun [interact](interact.md)(npc: NPC, action: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html))

Interacts with an NPC using a specified action.

#### Parameters

Kraken API

| | |
|---|---|
| npc | The NPC to interact with. |
| action | The action to perform (e.g., &quot;Attack&quot;, &quot;Pickpocket&quot;). |

[Kraken API]\
open fun [interact](interact.md)(player: Player, action: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html))

Interacts with another player using a specified action.

#### Parameters

Kraken API

| | |
|---|---|
| player | The player to interact with. |
| action | The action to perform (e.g., &quot;Trade&quot;, &quot;Follow&quot;). |

[Kraken API]\
open fun [interact](interact.md)(object: TileObject, action: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html))

Interacts with a game object using a specified action.

#### Parameters

Kraken API

| | |
|---|---|
| object | The TileObject (game object) to interact with. |
| action | The action to perform (e.g., &quot;Chop down&quot;, &quot;Mine&quot;). |

[Kraken API]\
open fun [interact](interact.md)(item: [GroundItem](../../com.kraken.api.query.groundobject/-ground-item/index.md))

Picks up a ground item. Defaults to the &quot;Take&quot; action.

#### Parameters

Kraken API

| | |
|---|---|
| item | The ground item to pick up. |

[Kraken API]\
open fun [interact](interact.md)(item: [GroundItem](../../com.kraken.api.query.groundobject/-ground-item/index.md), action: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html))

Interacts with a ground item using a specified action.

#### Parameters

Kraken API

| | |
|---|---|
| item | The ground item to interact with. |
| action | The action to perform (e.g., &quot;Take&quot;, &quot;Cast&quot;). |

[Kraken API]\
open fun [interact](interact.md)(widget: Widget, action: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html))

Interacts with a UI widget using a specified action.

#### Parameters

Kraken API

| | |
|---|---|
| widget | The widget to interact with. |
| action | The action to perform. |

[Kraken API]\
open fun [interact](interact.md)(item: [BankItemWidget](../../com.kraken.api.query.container.bank/-bank-item-widget/index.md), action: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html))

Interacts with an item inside the bank interface.

#### Parameters

Kraken API

| | |
|---|---|
| item | The bank item widget to interact with. |
| action | The action to perform (e.g., &quot;Withdraw-1&quot;, &quot;Withdraw-All&quot;). |

[Kraken API]\
open fun [interact](interact.md)(item: [ContainerItem](../../com.kraken.api.query.container/-container-item/index.md), action: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html))

Interacts with an item within a container (such as the player's inventory).

#### Parameters

Kraken API

| | |
|---|---|
| item | The container item to interact with. |
| action | The action to perform (e.g., &quot;Drop&quot;, &quot;Wield&quot;). |

[Kraken API]\
open fun [interact](interact.md)(item: [ContainerItem](../../com.kraken.api.query.container/-container-item/index.md), actions: [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)&gt;)

Attempts to interact with a container item by trying a sequence of actions. Dispatches the first action that successfully resolves.

#### Parameters

Kraken API

| | |
|---|---|
| item | The container item to interact with. |
| actions | An array of actions to attempt, in order of priority. |

[Kraken API]\
open fun [interact](interact.md)(packedWidgetId: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), option: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html))

Handles dialogue progression or selection using a widget's packed ID.

#### Parameters

Kraken API

| | |
|---|---|
| packedWidgetId | The packed ID of the dialogue widget. |
| option | The dialogue option to select (-1 for continue, 1-5 for choices). |

[Kraken API]\
open fun [interact](interact.md)(widget: Widget, option: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html))

Handles dialogue progression or selection using a specific widget.

#### Parameters

Kraken API

| | |
|---|---|
| widget | The dialogue widget. |
| option | The dialogue option to select (-1 for continue, 1-5 for choices). |

[Kraken API]\
open fun [interact](interact.md)(heading: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html))

Sets the player's heading/camera direction.

#### Parameters

Kraken API

| | |
|---|---|
| heading | The heading value to set. |

[Kraken API]\
open fun [interact](interact.md)(item: Widget, menu: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html), action: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html))

Performs a nested or sub-action on a widget, such as right-click equipment teleports.

#### Parameters

Kraken API

| | |
|---|---|
| item | The widget item to interact with. |
| menu | The parent menu name (e.g., &quot;Rub&quot;). |
| action | The specific sub-action to perform (e.g., &quot;Grand Exchange&quot;). |

[Kraken API]\
open fun [interact](interact.md)(src: Widget, dest: Widget)

Uses one widget on another widget (e.g., casting High Alchemy on an inventory item, or using a chisel on a gem).

#### Parameters

Kraken API

| | |
|---|---|
| src | The source widget (the item or spell being used). |
| dest | The destination widget (the item being targeted). |

[Kraken API]\
open fun [interact](interact.md)(src: Widget, dest: TileObject)

Uses a widget on a game object (e.g., using a bucket on a fountain or a spell on an object).

#### Parameters

Kraken API

| | |
|---|---|
| src | The source widget (the item or spell). |
| dest | The destination TileObject in the game world. |

[Kraken API]\
open fun [interact](interact.md)(src: Widget, dest: NPC)

Uses a widget on an NPC (e.g., casting Crumble Undead on a Vorkath spawn, or using an item on an NPC).

#### Parameters

Kraken API

| | |
|---|---|
| src | The source widget (the item or spell). |
| dest | The destination NPC. |

[Kraken API]\
open fun [interact](interact.md)(src: Widget, dest: [GroundItem](../../com.kraken.api.query.groundobject/-ground-item/index.md))

Uses a widget on a ground item (e.g., casting Telekinetic Grab on dropped loot).

#### Parameters

Kraken API

| | |
|---|---|
| src | The source widget (the spell or item). |
| dest | The destination ground item. |

[Kraken API]\
open fun [interact](interact.md)(widgetId: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), childId: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), itemId: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), action: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html))

Interacts with a widget by resolving its context and dispatching the specified interaction. This method performs the following tasks: 

- Ensures that packets are loaded before proceeding.
- Resolves the widget using the provided `widgetId`.
- Determines the display name for the action based on the specified `action` index.
- Dispatches the interaction to the game's input system.

 If the widget cannot be resolved or the action name cannot be determined, appropriate warnings will be logged.

#### Parameters

Kraken API

| | |
|---|---|
| widgetId | the unique identifier of the widget to be interacted with. This corresponds to the parent widget in the user interface. |
| childId | the identifier of the child element within the widget being targeted. If no child element is specified, this is typically `-1`. |
| itemId | the identifier of a specific item within the widget, if applicable. If not targeting an item, this is typically `-1`. |
| action | the zero-based index of the action to be performed from the widget's action list. This determines the type of interaction executed (e.g., click, examine). |
