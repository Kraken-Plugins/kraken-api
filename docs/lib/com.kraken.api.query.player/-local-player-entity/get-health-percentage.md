//[lib](../../../index.md)/[com.kraken.api.query.player](../index.md)/[LocalPlayerEntity](index.md)/[getHealthPercentage](get-health-percentage.md)

# getHealthPercentage

[Kraken API]\
open fun [getHealthPercentage](get-health-percentage.md)(): [Double](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-double/index.html)

Calculates the player's current health as a percentage of their real (base) health. If the player has 40 hp total and has 36 hp remaining this will return ~85.0 showing that roughly 85% of the players health is remaining.

#### Return

the health percentage as a double. For example: 150.0 if boosted, 80.0 if drained, or 100.0 if unchanged.
