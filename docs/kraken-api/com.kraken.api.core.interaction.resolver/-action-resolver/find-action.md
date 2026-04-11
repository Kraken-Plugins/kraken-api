//[kraken-api](../../../index.md)/[com.kraken.api.core.interaction.resolver](../index.md)/[ActionResolver](index.md)/[findAction](find-action.md)

# findAction

[Kraken API]\
open fun [findAction](find-action.md)(requested: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html), available: [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)&gt;, factory: [IntFunction](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/function/IntFunction.html)&lt;[MenuOption](../../com.kraken.api.core.interaction.model/-menu-option/index.md)&gt;): [Optional](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/Optional.html)&lt;[MenuOption](../../com.kraken.api.core.interaction.model/-menu-option/index.md)&gt;

Finds the first action in `available` matching `requested`, then maps its index through `factory` to produce a MenuOption.

#### Return

The first action in `available` matching `requested`,

#### Parameters

Kraken API

| | |
|---|---|
| requested | The action string to match. |
| available | The array of actions to match. |
| factory | The factory to produce a MenuOption. |
