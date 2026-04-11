//[kraken-api](../../../index.md)/[com.kraken.api.core.interaction](../index.md)/[DoActionInvoker](index.md)/[invoke](invoke.md)

# invoke

[Kraken API]\
open fun [invoke](invoke.md)(param0: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), param1: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), opcode: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), identifier: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), itemId: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), worldViewId: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), option: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html), target: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html), canvasX: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), canvasY: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html))

Invokes the *doAction* method through reflection, passing the provided parameters. Resolves and caches the required method dynamically if not already loaded. If the invocation fails, an error is logged. 

This method is designed to execute client-side handling tied to in-game interactions like menu or widget actions within a given client context.

#### Parameters

Kraken API

| | |
|---|---|
| param0 | First coordinate or identifier relevant to the action. |
| param1 | Second coordinate or identifier relevant to the action. |
| opcode | Action opcode indicating the type of interaction to perform. |
| identifier | Unique identifier for the action's context, such as an in-game object or widget. |
| itemId | Item identifier when the action pertains to an inventory or bank item. |
| worldViewId | Identifier representing the view context of the action in the game world. |
| option | String representing the action's option (e.g., &quot;Examine&quot;, &quot;Use&quot;). |
| target | Target entity or in-game object related to the action. |
| canvasX | X-coordinate on the game's canvas where the action occurs. |
| canvasY | Y-coordinate on the game's canvas where the action occurs. |
