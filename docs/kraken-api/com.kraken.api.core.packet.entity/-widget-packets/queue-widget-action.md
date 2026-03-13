//[kraken-api](../../../index.md)/[com.kraken.api.core.packet.entity](../index.md)/[WidgetPackets](index.md)/[queueWidgetAction](queue-widget-action.md)

# queueWidgetAction

[Kraken API]\
open fun [queueWidgetAction](queue-widget-action.md)(widget: Widget, actionlist: [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)&gt;)

Queues a widget action by searching for a specific action string (e.g., &quot;Withdraw-1&quot;, &quot;Bank&quot;). 

 This is a higher-level convenience method. Instead of needing to know the exact action number (1-10), you can provide the human-readable text of the action. The method will find the corresponding action number and send the correct packet.

#### Parameters

Kraken API

| | |
|---|---|
| widget | The Widget object to interact with. |
| actionlist | A varargs list of action strings to search for. The method will use the *first* match it finds. The search is case-insensitive and ignores color tags. |
