//[kraken-api](../../../index.md)/[com.kraken.api.query.world](../index.md)/[WorldQuery](index.md)/[nonSkillTotalWorlds](non-skill-total-worlds.md)

# nonSkillTotalWorlds

[Kraken API]\
open fun [nonSkillTotalWorlds](non-skill-total-worlds.md)(): [WorldQuery](index.md)

Retrieves a query for worlds that exclude the SKILL_TOTAL type. 

 This method creates a query to filter out worlds that are categorized as SKILL_TOTAL, allowing only other world types to be included in the result.

#### Return

a `WorldQuery` instance that excludes worlds of type SKILL_TOTAL.
