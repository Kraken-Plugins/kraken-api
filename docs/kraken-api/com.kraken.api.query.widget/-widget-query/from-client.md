//[kraken-api](../../../index.md)/[com.kraken.api.query.widget](../index.md)/[WidgetQuery](index.md)/[fromClient](from-client.md)

# fromClient

[Kraken API]\
open fun [fromClient](from-client.md)(packedId: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [WidgetEntity](../-widget-entity/index.md)

Returns a widget directly from the client. This can return widgets which may not be visible but are known to the client. i.e. a logout button widget without being on the logout tab. 

 This method wraps the widget in a [WidgetEntity](../-widget-entity/index.md) class allowing the widget to be interacted with.

#### Return

WidgetEntity or null if no widget is found.

#### Parameters

Kraken API

| | |
|---|---|
| packedId | The packed widget component id to find. |

[Kraken API]\
open fun [fromClient](from-client.md)(groupId: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), childId: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [WidgetEntity](../-widget-entity/index.md)

Returns a widget directly from the client. This can return widgets which may not be visible but are known to the client. i.e. a logout button widget without being on the logout tab. 

 This method wraps the widget in a [WidgetEntity](../-widget-entity/index.md) class allowing the widget to be interacted with.

#### Return

WidgetEntity or null if no widget is found.

#### Parameters

Kraken API

| | |
|---|---|
| groupId | The widgets group id |
| childId | The widgets child id |
