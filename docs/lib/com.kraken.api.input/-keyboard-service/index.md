//[lib](../../../index.md)/[com.kraken.api.input](../index.md)/[KeyboardService](index.md)

# KeyboardService

[Kraken API]\
open class [KeyboardService](index.md)

## Constructors

| | |
|---|---|
| [KeyboardService](-keyboard-service.md) | [Kraken API]<br>constructor() |

## Functions

| Name | Summary |
|---|---|
| [enter](enter.md) | [Kraken API]<br>open fun [enter](enter.md)()<br>Simulates pressing the Enter key. |
| [holdShift](hold-shift.md) | [Kraken API]<br>open fun [holdShift](hold-shift.md)()<br>Simulates holding the Shift key using a KEY_PRESSED event. |
| [keyHold](key-hold.md) | [Kraken API]<br>open fun [keyHold](key-hold.md)(key: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html))<br>Simulates holding down a key using a KEY_PRESSED event. |
| [keyPress](key-press.md) | [Kraken API]<br>open fun [keyPress](key-press.md)(key: [Char](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-char/index.html))<br>Simulates pressing a single character using a KEY_TYPED event.<br>[Kraken API]<br>open fun [keyPress](key-press.md)(key: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html))<br>Simulates pressing and releasing a key in quick succession. |
| [keyRelease](key-release.md) | [Kraken API]<br>open fun [keyRelease](key-release.md)(key: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html))<br>Simulates releasing a key using a KEY_RELEASED event. |
| [releaseShift](release-shift.md) | [Kraken API]<br>open fun [releaseShift](release-shift.md)()<br>Simulates releasing the Shift key using a KEY_RELEASED event. |
| [typeChar](type-char.md) | [Kraken API]<br>open fun [typeChar](type-char.md)(c: [Char](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-char/index.html))<br>Types a single character. |
| [typeString](type-string.md) | [Kraken API]<br>open fun [typeString](type-string.md)(word: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html))<br>Types out a string character-by-character using KEY_TYPED events.<br>[Kraken API]<br>open fun [typeString](type-string.md)(text: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html), minSleep: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), maxSleep: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html))<br>Types a string with a customizable sleep between characters. |
