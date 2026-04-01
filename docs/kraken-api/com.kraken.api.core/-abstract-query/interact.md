//[kraken-api](../../../index.md)/[com.kraken.api.core](../index.md)/[AbstractQuery](index.md)/[interact](interact.md)

# interact

[Kraken API]\
open fun [interact](interact.md)(action: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)

Interacts with the first matching entity using the given action. Returns false if no entity is found or the interaction fails. Usage: ctx.gameObjects().nameContains(&quot;Bank&quot;).interact(&quot;Open&quot;);

#### Return

Boolean true if the interaction was successful and false otherwise.

#### Parameters

Kraken API

| | |
|---|---|
| action | The action string to perform the interaction i.e. &quot;Open&quot;, &quot;Take&quot;, &quot;Bank&quot;, etc... |
