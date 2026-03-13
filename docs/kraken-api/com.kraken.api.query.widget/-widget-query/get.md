//[kraken-api](../../../index.md)/[com.kraken.api.query.widget](../index.md)/[WidgetQuery](index.md)/[get](get.md)

# get

[Kraken API]\
open fun [get](get.md)(packedId: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [WidgetEntity](../-widget-entity/index.md)

Retrieves the first [WidgetEntity](../-widget-entity/index.md) matching the specified packed ID. 

This method filters widgets by their packed ID, which is a composite of the group ID and child ID. It is useful when the exact packed ID of a widget is known.

#### Return

The [WidgetEntity](../-widget-entity/index.md) corresponding to the specified packed ID, or `null` if no matching widget is found.

#### Parameters

Kraken API

| | |
|---|---|
| packedId | The packed ID of the widget to retrieve. This ID encapsulates both the group and child ID into a single integer value. |

[Kraken API]\
open fun [get](get.md)(widgetInfo: WidgetInfo): [WidgetEntity](../-widget-entity/index.md)

Retrieves the first [WidgetEntity](../-widget-entity/index.md) that matches the specified WidgetInfo. 

This method filters widgets based on the packed ID retrieved from the WidgetInfo instance.

#### Return

The corresponding [WidgetEntity](../-widget-entity/index.md), or `null` if no match is found.

#### Parameters

Kraken API

| | |
|---|---|
| widgetInfo | The WidgetInfo instance containing the packed ID to search for. The packed ID incorporates both the group and child ID. |

[Kraken API]\
open fun [get](get.md)(groupId: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), childId: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [WidgetEntity](../-widget-entity/index.md)

Retrieves a [WidgetEntity](../-widget-entity/index.md) based on the specified group ID and child ID. 

This method accesses a widget using its hierarchical identifiers: 

- **groupId:** Represents the interface group to which the widget belongs.
- **childId:** Identifies the specific widget within the group.

The widget is created and returned as a [WidgetEntity](../-widget-entity/index.md) instance, allowing for further interaction or queries.

#### Return

A [WidgetEntity](../-widget-entity/index.md) representing the widget with the provided group ID and child ID, or `null` if no matching widget is found.

#### Parameters

Kraken API

| | |
|---|---|
| groupId | The group ID of the widget to retrieve. Represents the top-level container or interface. |
| childId | The child ID of the widget to retrieve. Represents the specific item within the group. |
