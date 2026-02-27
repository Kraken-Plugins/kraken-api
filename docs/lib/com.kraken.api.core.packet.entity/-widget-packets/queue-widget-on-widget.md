//[lib](../../../index.md)/[com.kraken.api.core.packet.entity](../index.md)/[WidgetPackets](index.md)/[queueWidgetOnWidget](queue-widget-on-widget.md)

# queueWidgetOnWidget

[Kraken API]\
open fun [queueWidgetOnWidget](queue-widget-on-widget.md)(srcWidget: Widget, destWidget: Widget)

Queues a packet simulating the use of one widget item (source) on another widget item (destination). This is typically used for &quot;Use&quot; menu actions like using a potion on a bank slot or an item on a piece of equipment. 

 Delegates to the overloaded method using the raw IDs and indices.

#### Parameters

Kraken API

| | |
|---|---|
| srcWidget | The source widget (e.g., the item being &quot;used&quot;). |
| destWidget | The destination widget (e.g., the item or slot being used &quot;on&quot;). |

[Kraken API]\
open fun [queueWidgetOnWidget](queue-widget-on-widget.md)(sourceWidgetId: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), sourceSlot: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), sourceItemId: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), destinationWidgetId: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), destinationSlot: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), destinationItemId: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html))

Queues the raw IF_BUTTONT packet, simulating using an item/slot from a source widget on an item/slot of a destination widget.

#### Parameters

Kraken API

| | |
|---|---|
| sourceWidgetId | The ID of the source widget (interface). |
| sourceSlot | The slot/index within the source widget. |
| sourceItemId | The item ID within the source slot. |
| destinationWidgetId | The ID of the destination widget (interface). |
| destinationSlot | The slot/index within the destination widget. |
| destinationItemId | The item ID within the destination slot. |
