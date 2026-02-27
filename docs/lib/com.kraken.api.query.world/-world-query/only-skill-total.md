//[lib](../../../index.md)/[com.kraken.api.query.world](../index.md)/[WorldQuery](index.md)/[onlySkillTotal](only-skill-total.md)

# onlySkillTotal

[Kraken API]\
open fun [onlySkillTotal](only-skill-total.md)(total: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [WorldQuery](index.md)

Filters the current query to include only worlds with a skill total requirement less than or equal to the specified value. 

 This method modifies the query to include only worlds that: 

- Have the @WorldType.SKILL_TOTAL tag in their list of types.
- Have a skill total requirement, determined by numeric parsing of the world's activity description.

 If these conditions are not met for a world, it is excluded from the results.

#### Return

A `WorldQuery` object filtered to include only worlds with a skill total requirement less than or equal to the specified total.

#### Parameters

Kraken API

| | |
|---|---|
| total | The maximum skill total value (inclusive) to include in the filtered results. Worlds with a skill total requirement greater than this value will be excluded. |
