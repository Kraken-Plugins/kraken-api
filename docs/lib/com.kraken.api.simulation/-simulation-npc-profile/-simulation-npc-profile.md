//[lib](../../../index.md)/[com.kraken.api.simulation](../index.md)/[SimulationNpcProfile](index.md)/[SimulationNpcProfile](-simulation-npc-profile.md)

# SimulationNpcProfile

[Kraken API]\
constructor(attackRange: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), attackStyle: [NpcAttackStyle](../-npc-attack-style/index.md), attackSpeed: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), maxHit: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), intelligentPathing: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html))

Creates a profile for an NPC id mapping.

#### Parameters

Kraken API

| | |
|---|---|
| attackRange | attack range used for line-of-sight and attack checks. |
| attackStyle | attack style used for protection-prayer checks. |
| attackSpeed | attack speed in ticks. |
| maxHit | max hit used for damage simulation. |
| intelligentPathing | true to use collision-aware pathfinding instead of greedy movement. |
