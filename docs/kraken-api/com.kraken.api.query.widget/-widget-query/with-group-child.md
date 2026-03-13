//[kraken-api](../../../index.md)/[com.kraken.api.query.widget](../index.md)/[WidgetQuery](index.md)/[withGroupChild](with-group-child.md)

# withGroupChild

[Kraken API]\
open fun [withGroupChild](with-group-child.md)(group: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), child: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [WidgetQuery](index.md)

Filters for a widget with a specific group ID and child ID. 

This method evaluates the group and child ID of each widget by decomposing its packed ID. The packed ID is a combination where the group ID occupies the higher 16 bits, and the child ID occupies the lower 16 bits.

For example, in the packed id format:`packedId = (groupId << 16) | childId` The group ID and child ID are derived as follows:

- 
   `groupId = packedId >>> 16`
- 
   `childId = packedId & 0xFFFF`

Only the widgets that match the specified group and child ID will be included in the resulting query.

#### Return

WidgetQuery A new query filtered for widgets with the specified group and child IDs.

#### Parameters

Kraken API

| | |
|---|---|
| group | The group identifier of the widget. |
| child | The child identifier of the widget. |
