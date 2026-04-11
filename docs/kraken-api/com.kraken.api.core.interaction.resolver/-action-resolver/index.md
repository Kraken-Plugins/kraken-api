//[kraken-api](../../../index.md)/[com.kraken.api.core.interaction.resolver](../index.md)/[ActionResolver](index.md)

# ActionResolver

[Kraken API]\
class [ActionResolver](index.md)

Stateless utility for matching action strings against available action arrays.

## Constructors

| | |
|---|---|
| [ActionResolver](-action-resolver.md) | [Kraken API]<br>constructor() |

## Functions

| Name | Summary |
|---|---|
| [findAction](find-action.md) | [Kraken API]<br>open fun [findAction](find-action.md)(requested: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html), available: [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)&gt;, factory: [IntFunction](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/function/IntFunction.html)&lt;[MenuOption](../../com.kraken.api.core.interaction.model/-menu-option/index.md)&gt;): [Optional](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/Optional.html)&lt;[MenuOption](../../com.kraken.api.core.interaction.model/-menu-option/index.md)&gt;<br>Finds the first action in `available` matching `requested`, then maps its index through `factory` to produce a MenuOption. |
| [matches](matches.md) | [Kraken API]<br>open fun [matches](matches.md)(requested: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html), candidate: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>Compares a requested string with a candidate string to determine if they match as equal. |
