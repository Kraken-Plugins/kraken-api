//[kraken-api](../../../index.md)/[com.kraken.api.query.gameobject](../index.md)/[GameObjectQuery](index.md)/[withAction](with-action.md)

# withAction

[Kraken API]\
open fun [withAction](with-action.md)(action: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)): [GameObjectQuery](index.md)

Filters for objects that have a specific action available. Usage: ctx.objects().withAction(&quot;Mine&quot;).nearest().first();

#### Return

GameObjectQuery

#### Parameters

Kraken API

| | |
|---|---|
| action | The action to check for i.e &quot;Mine&quot;, &quot;Chop&quot;, &quot;Examine&quot;. |
