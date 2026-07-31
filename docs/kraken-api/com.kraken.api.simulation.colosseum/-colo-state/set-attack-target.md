//[kraken-api](../../../index.md)/[com.kraken.api.simulation.colosseum](../index.md)/[ColoState](index.md)/[setAttackTarget](set-attack-target.md)

# setAttackTarget

[Kraken API]\
open fun [setAttackTarget](set-attack-target.md)(slot: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html))

Sets the player's current attack interaction; used by the snapshot builder so plans know an engagement is already in progress and no fresh attack click is needed.

#### Parameters

Kraken API

| | |
|---|---|
| slot | npc slot the player is attacking, or -1 for none. |
