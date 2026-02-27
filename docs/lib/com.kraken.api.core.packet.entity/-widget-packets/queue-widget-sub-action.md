//[lib](../../../index.md)/[com.kraken.api.core.packet.entity](../index.md)/[WidgetPackets](index.md)/[queueWidgetSubAction](queue-widget-sub-action.md)

# queueWidgetSubAction

[Kraken API]\
open fun [queueWidgetSubAction](queue-widget-sub-action.md)(widget: Widget, menu: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html), action: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html))

Queues a widget sub-action packet by identifying the specific sub-action and menu options associated with a given widget. 

 This method identifies the indices of both a sub-action (from item definitions) and a specific menu option (from the widget's actions). If matches for both the sub-action and menu option are found, it sends a low-level packet to perform the action. 

 Only executes if the widget and its associated item ID are valid, while the sub-actions and menu options must contain the desired action and menu option.

#### Parameters

Kraken API

| | |
|---|---|
| widget | The Widget instance on which the action is to be performed. This is the target widget for the queued action. |
| menu | A case-insensitive @&lt;String&gt; representing the menu action text to search for (e.g., &quot;Use&quot;, &quot;Examine&quot;). |
| action | A case-insensitive @&lt;String&gt; representing the sub-action text to search for (e.g., &quot;Clean&quot;, &quot;Equip&quot;). |
