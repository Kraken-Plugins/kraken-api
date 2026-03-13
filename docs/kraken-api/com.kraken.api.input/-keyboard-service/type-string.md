//[kraken-api](../../../index.md)/[com.kraken.api.input](../index.md)/[KeyboardService](index.md)/[typeString](type-string.md)

# typeString

[Kraken API]\
open fun [typeString](type-string.md)(text: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html), minSleep: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), maxSleep: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html))

Types a string with a customizable sleep between characters.

#### Parameters

Kraken API

| | |
|---|---|
| text | the string to type |
| minSleep | The minimum the thread should be slept between key strokes |
| maxSleep | The max the thread should be slept between key strokes |

[Kraken API]\
open fun [typeString](type-string.md)(word: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html))

Types out a string character-by-character using KEY_TYPED events. Each character is sent with a short randomized delay and sleep between characters.

#### Parameters

Kraken API

| | |
|---|---|
| word | the string to type into the game |
