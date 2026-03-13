//[kraken-api](../../../index.md)/[com.kraken.api.query.widget](../index.md)/[WidgetQuery](index.md)/[withText](with-text.md)

# withText

[Kraken API]\
open fun [withText](with-text.md)(text: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)): [WidgetQuery](index.md)

Filters for widgets containing the specified text in name, text, or actions.

#### Return

WidgetQuery

#### Parameters

Kraken API

| | |
|---|---|
| text | The text to search for |

[Kraken API]\
open fun [withText](with-text.md)(text: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html), exact: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)): [WidgetQuery](index.md)

Filters for widgets containing the specified text in name, text, or actions.

#### Return

WidgetQuery

#### Parameters

Kraken API

| | |
|---|---|
| text | The text to search for |
| exact | True if the text should match exactly otherwise a substring is used. |
