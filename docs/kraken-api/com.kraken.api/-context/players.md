//[kraken-api](../../../index.md)/[com.kraken.api](../index.md)/[Context](index.md)/[players](players.md)

# players

[Kraken API]\
open fun [players](players.md)(): [PlayerQuery](../../com.kraken.api.query.player/-player-query/index.md)

Creates a new query builder for Players. This will also include the local player as well which can be grabbed with `.local()`. Usage: ctx.players().withName(&quot;Zezima&quot;).first().interact(&quot;Follow&quot;); ctx.players().local().getName();

#### Return

PlayerQuery object used to chain together predicates to select specific Players's within the scene.
