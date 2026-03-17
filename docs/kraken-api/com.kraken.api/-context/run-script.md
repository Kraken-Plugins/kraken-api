//[kraken-api](../../../index.md)/[com.kraken.api](../index.md)/[Context](index.md)/[runScript](run-script.md)

# runScript

[Kraken API]\
open fun [runScript](run-script.md)(id: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html))

Wraps the RuneLite client's run script method scheduling the run on the client thread. This is a convenience method for `ctx.runOnClientThread(() -> ctx.getClient().runScript(...));`

#### Parameters

Kraken API

| | |
|---|---|
| id | The CS2 script id to run. |
