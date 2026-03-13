//[kraken-api](../../../index.md)/[com.kraken.api.query.world](../index.md)/[WorldQuery](index.md)/[freeToPlay](free-to-play.md)

# freeToPlay

[Kraken API]\
open fun [freeToPlay](free-to-play.md)(): [WorldQuery](index.md)

Filters the current query to include only free-to-play worlds. 

 This method modifies the query to exclude any worlds classified as `MEMBERS`-only. Free-to-play worlds are worlds that do not have the `WorldType.MEMBERS` tag. 

#### Return

A `WorldQuery` object filtered to include only free-to-play worlds.
