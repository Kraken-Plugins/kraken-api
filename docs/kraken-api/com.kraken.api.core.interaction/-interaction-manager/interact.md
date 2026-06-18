//[kraken-api](../../../index.md)/[com.kraken.api.core.interaction](../index.md)/[InteractionManager](index.md)/[interact](interact.md)

# interact

[Kraken API]\
open fun [interact](interact.md)(npc: NPC, action: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html))

open fun [interact](interact.md)(player: Player, action: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html))

open fun [interact](interact.md)(object: TileObject, action: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html))

open fun [interact](interact.md)(item: [GroundItem](../../com.kraken.api.query.groundobject/-ground-item/index.md))

open fun [interact](interact.md)(item: [GroundItem](../../com.kraken.api.query.groundobject/-ground-item/index.md), action: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html))

open fun [interact](interact.md)(widget: Widget, action: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html))

open fun [interact](interact.md)(item: [BankItemWidget](../../com.kraken.api.query.container.bank/-bank-item-widget/index.md), action: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html))

open fun [interact](interact.md)(item: [ContainerItem](../../com.kraken.api.query.container/-container-item/index.md), action: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html))

open fun [interact](interact.md)(item: [ContainerItem](../../com.kraken.api.query.container/-container-item/index.md), actions: [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)&gt;)

open fun [interact](interact.md)(packedWidgetId: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), option: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html))

open fun [interact](interact.md)(widget: Widget, option: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html))

open fun [interact](interact.md)(heading: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html))

open fun [interact](interact.md)(item: Widget, menu: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html), action: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html))

open fun [interact](interact.md)(src: Widget, dest: Widget)

open fun [interact](interact.md)(src: Widget, dest: TileObject)

open fun [interact](interact.md)(src: Widget, dest: NPC)

open fun [interact](interact.md)(src: Widget, dest: [GroundItem](../../com.kraken.api.query.groundobject/-ground-item/index.md))

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
