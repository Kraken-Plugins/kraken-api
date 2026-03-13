//[kraken-api](../../../index.md)/[com.kraken.api.query.groundobject](../index.md)/[GroundObjectEntity](index.md)/[interact](interact.md)

# interact

[Kraken API]\
open fun [interact](interact.md)(action: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)

Interact interacts with the ground object. This interact is slightly different from other interactions because the underlying packet object does not accept an action to take on the ground object. It is always &quot;Take&quot; because that is the only action you can perform to an item on the ground. 

 To conform to the interface for an `AbstractEntity` we still accept an action parameter although it will do nothing in this particular instance.

#### Return

True if the interaction is successful and false otherwise

#### Parameters

Kraken API

| | |
|---|---|
| action | The menu action to trigger (e.g. &quot;Take&quot;) |
