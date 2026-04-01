//[kraken-api](../../../index.md)/[com.kraken.api.core](../index.md)/[AbstractQuery](index.md)/[interactRandom](interact-random.md)

# interactRandom

[Kraken API]\
open fun [interactRandom](interact-random.md)(action: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)

Interacts with a random matching entity. Returns false if no entity is found or the interaction fails. Usage: ctx.npcs().withName(&quot;Cow&quot;).interactRandom(&quot;Attack&quot;);

#### Return

Boolean true if the interaction was successful and false otherwise.

#### Parameters

Kraken API

| | |
|---|---|
| action | The action string to perform the interaction i.e. &quot;Open&quot;, &quot;Take&quot;, &quot;Bank&quot;, etc... |
