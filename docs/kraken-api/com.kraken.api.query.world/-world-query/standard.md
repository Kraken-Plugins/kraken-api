//[kraken-api](../../../index.md)/[com.kraken.api.query.world](../index.md)/[WorldQuery](index.md)/[standard](standard.md)

# standard

[Kraken API]\
open fun [standard](standard.md)(): [WorldQuery](index.md)

Configures and returns a `WorldQuery` object that excludes specific world types which are not part of the &quot;standard&quot; main game world category. This will filter out worlds like fresh start, deadman, seasonal, leagues, gridmaster etc... 

The following world types will be excluded: 

- @WorldType.PVP_ARENA
- @WorldType.PVP
- @WorldType.QUEST_SPEEDRUNNING
- @WorldType.BETA_WORLD
- @WorldType.LEGACY_ONLY
- @WorldType.EOC_ONLY
- @WorldType.NOSAVE_MODE
- @WorldType.FRESH_START_WORLD
- @WorldType.DEADMAN
- @WorldType.SEASONAL

#### Return

A `WorldQuery` object that specifically excludes the listed world types.
