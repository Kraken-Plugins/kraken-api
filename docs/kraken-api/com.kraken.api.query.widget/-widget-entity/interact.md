//[kraken-api](../../../index.md)/[com.kraken.api.query.widget](../index.md)/[WidgetEntity](index.md)/[interact](interact.md)

# interact

[Kraken API]\
open fun [interact](interact.md)(action: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)

Interacts with the entity using the given action verb.

#### Return

true if the interaction packet was successfully queued/sent

#### Parameters

Kraken API

| | |
|---|---|
| action | The menu action to trigger (e.g., &quot;Attack&quot;, &quot;Talk-to&quot;, &quot;Take&quot;) |

[Kraken API]\
open fun [interact](interact.md)(menu: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html), action: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)

Interacts with a widget by invoking a specified menu and action. 

This method attempts to perform an interaction on a widget identified by its underlying raw representation. If the raw widget is `null`, the method will immediately return `false`. Otherwise, it delegates the interaction process to the interaction manager.

#### Return

`true` if the interaction process was initiated successfully; `false` if the underlying widget was `null` and no action was performed.

#### Parameters

Kraken API

| | |
|---|---|
| menu | The menu option to be selected during the interaction. For example, this could represent a contextual menu option like &quot;Use&quot; or &quot;Examine&quot;. |
| action | The specific action to be invoked within the selected menu option. This typically represents the intended effect of the interaction, such as &quot;Wield&quot;. |

[Kraken API]\
open fun [interact](interact.md)(action: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), packedId: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), childId: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), itemId: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)

Interacts with a widget using the specified action index.

#### Return

True if the interaction was successful and false otherwise

#### Parameters

Kraken API

| | |
|---|---|
| action | The action index to take. |
| packedId | The packed widget id |
| childId | The child id of the widget |
| itemId | The item id of the widget. |
