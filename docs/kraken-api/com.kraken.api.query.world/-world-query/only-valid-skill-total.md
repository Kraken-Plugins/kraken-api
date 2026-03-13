//[kraken-api](../../../index.md)/[com.kraken.api.query.world](../index.md)/[WorldQuery](index.md)/[onlyValidSkillTotal](only-valid-skill-total.md)

# onlyValidSkillTotal

[Kraken API]\
open fun [onlyValidSkillTotal](only-valid-skill-total.md)(): [WorldQuery](index.md)

Filters the current query to include only worlds with valid skill total level requirements that the local player's total skill level satisfies. 

 This method modifies the query to include only worlds meeting the following conditions: 

- The world has the @WorldType.SKILL_TOTAL tag in its list of types.
- The world has a valid numerical skill total requirement, determined by extracting and parsing the numeric value from the world's activity description.
- The player's total skill level is greater than or equal to the world's skill total requirement.

 Worlds that do not meet these conditions, or those with invalid or missing activity information, are excluded from the results.

#### Return

A `WorldQuery` object filtered to include only worlds with skill total requirements that are valid and met by the local player's total skill level.
