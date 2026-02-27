//[lib](../../../index.md)/[com.kraken.api.core.packet.entity](../index.md)/[WidgetPackets](index.md)/[queueWidgetActionPacket](queue-widget-action-packet.md)

# queueWidgetActionPacket

[Kraken API]\
open fun [queueWidgetActionPacket](queue-widget-action-packet.md)(widgetId: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), childId: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), itemId: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), actionFieldNo: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html))

Queues a low-level widget action packet (IF_BUTTONX). 

 This method is a direct wrapper for the IF_BUTTONX packet, which is used for most widget interactions. It corresponds to one of the 10 &quot;IF_BUTTON&quot; opcodes (e.g., IF_BUTTON1, IF_BUTTON2, etc.).

#### Parameters

Kraken API

| | |
|---|---|
| widgetId | The parent widget ID (e.g., WidgetInfo.BANK_CONTAINER.getId()). |
| childId | The specific child widget index (slot) within the parent. -1 for no specific child. |
| itemId | The item ID associated with the slot, if any. -1 for no item. |
| actionFieldNo | The action number (1-10) to execute. This maps to the specific packet (e.g., 1 = IF_BUTTON1, 2 = IF_BUTTON2). |
