//[kraken-api](../../../index.md)/[com.kraken.api.simulation.colosseum](../index.md)/[ColoState](index.md)

# ColoState

[Kraken API]\
class [ColoState](index.md)

Mutable, poolable simulation state: everything about one possible future of the arena. 

Layout is deliberately flat - primitive scalars plus a few small primitive arrays - so that [copyFrom](copy-from.md) is a handful of `System.arraycopy` calls and the search loop allocates nothing. Immutable identity data (types, sizes, max HP, the grid) lives in the shared [ColoFrame](../-colo-frame/index.md).

In-flight attacks are packed into a `long` ring: land tick (16 bits), max damage (8), style (2), source/target slot (6), targeted tile for dodgeable specials (12), a prayer-resolved flag and expected damage (8). See [ColoTick](../-colo-tick/index.md) for encoding.

## Constructors

| | |
|---|---|
| [ColoState](-colo-state.md) | [Kraken API]<br>constructor() |

## Properties

| Name | Summary |
|---|---|
| [OVERHEAD_MAGIC](-o-v-e-r-h-e-a-d_-m-a-g-i-c.md) | [Kraken API]<br>val [OVERHEAD_MAGIC](-o-v-e-r-h-e-a-d_-m-a-g-i-c.md): [Byte](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-byte/index.html) = 3<br>Overhead code: Protect from Magic. |
| [OVERHEAD_MELEE](-o-v-e-r-h-e-a-d_-m-e-l-e-e.md) | [Kraken API]<br>val [OVERHEAD_MELEE](-o-v-e-r-h-e-a-d_-m-e-l-e-e.md): [Byte](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-byte/index.html) = 1<br>Overhead code: Protect from Melee. |
| [OVERHEAD_MISSILES](-o-v-e-r-h-e-a-d_-m-i-s-s-i-l-e-s.md) | [Kraken API]<br>val [OVERHEAD_MISSILES](-o-v-e-r-h-e-a-d_-m-i-s-s-i-l-e-s.md): [Byte](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-byte/index.html) = 2<br>Overhead code: Protect from Missiles. |
| [OVERHEAD_NONE](-o-v-e-r-h-e-a-d_-n-o-n-e.md) | [Kraken API]<br>val [OVERHEAD_NONE](-o-v-e-r-h-e-a-d_-n-o-n-e.md): [Byte](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-byte/index.html) = 0<br>Overhead code: no protection prayer active. |

## Functions

| Name | Summary |
|---|---|
| [activateNpc](activate-npc.md) | [Kraken API]<br>open fun [activateNpc](activate-npc.md)(slot: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), pos: [Short](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-short/index.html), hp: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), cooldown: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html))<br>Activates an npc slot with full identity coming from the frame. |
| [aliveNpcCount](alive-npc-count.md) | [Kraken API]<br>open fun [aliveNpcCount](alive-npc-count.md)(): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) |
| [attackDelay](attack-delay.md) | [Kraken API]<br>open fun [attackDelay](attack-delay.md)(): [Byte](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-byte/index.html) |
| [attackTargetSlot](attack-target-slot.md) | [Kraken API]<br>open fun [attackTargetSlot](attack-target-slot.md)(): [Byte](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-byte/index.html) |
| [brewDoses](brew-doses.md) | [Kraken API]<br>open fun [brewDoses](brew-doses.md)(): [Byte](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-byte/index.html) |
| [comboCount](combo-count.md) | [Kraken API]<br>open fun [comboCount](combo-count.md)(): [Byte](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-byte/index.html) |
| [copyFrom](copy-from.md) | [Kraken API]<br>open fun [copyFrom](copy-from.md)(other: [ColoState](index.md))<br>Copies another state into this instance (pool-friendly branch copy). |
| [damageDealt](damage-dealt.md) | [Kraken API]<br>open fun [damageDealt](damage-dealt.md)(): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) |
| [expectedDamageTaken](expected-damage-taken.md) | [Kraken API]<br>open fun [expectedDamageTaken](expected-damage-taken.md)(): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) |
| [foodCount](food-count.md) | [Kraken API]<br>open fun [foodCount](food-count.md)(): [Byte](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-byte/index.html) |
| [frame](frame.md) | [Kraken API]<br>open fun [frame](frame.md)(): [ColoFrame](../-colo-frame/index.md) |
| [gearSet](gear-set.md) | [Kraken API]<br>open fun [gearSet](gear-set.md)(): [Byte](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-byte/index.html) |
| [javelinAutosSinceSpecial](javelin-autos-since-special.md) | [Kraken API]<br>open fun [javelinAutosSinceSpecial](javelin-autos-since-special.md)(slot: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) |
| [kills](kills.md) | [Kraken API]<br>open fun [kills](kills.md)(): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) |
| [manticoreOrbsRemaining](manticore-orbs-remaining.md) | [Kraken API]<br>open fun [manticoreOrbsRemaining](manticore-orbs-remaining.md)(slot: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) |
| [manticorePattern](manticore-pattern.md) | [Kraken API]<br>open fun [manticorePattern](manticore-pattern.md)(slot: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) |
| [moveTarget](move-target.md) | [Kraken API]<br>open fun [moveTarget](move-target.md)(): [Short](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-short/index.html) |
| [npcActive](npc-active.md) | [Kraken API]<br>open fun [npcActive](npc-active.md)(slot: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html) |
| [npcChargingStarted](npc-charging-started.md) | [Kraken API]<br>open fun [npcChargingStarted](npc-charging-started.md)(slot: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html) |
| [npcCooldown](npc-cooldown.md) | [Kraken API]<br>open fun [npcCooldown](npc-cooldown.md)(slot: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) |
| [npcHp](npc-hp.md) | [Kraken API]<br>open fun [npcHp](npc-hp.md)(slot: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) |
| [npcPos](npc-pos.md) | [Kraken API]<br>open fun [npcPos](npc-pos.md)(slot: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [Short](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-short/index.html) |
| [overhead](overhead.md) | [Kraken API]<br>open fun [overhead](overhead.md)(): [Byte](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-byte/index.html) |
| [pendingHit](pending-hit.md) | [Kraken API]<br>open fun [pendingHit](pending-hit.md)(index: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html) |
| [pendingHitCount](pending-hit-count.md) | [Kraken API]<br>open fun [pendingHitCount](pending-hit-count.md)(): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) |
| [playerDied](player-died.md) | [Kraken API]<br>open fun [playerDied](player-died.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html) |
| [playerHp](player-hp.md) | [Kraken API]<br>open fun [playerHp](player-hp.md)(): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) |
| [playerPos](player-pos.md) | [Kraken API]<br>open fun [playerPos](player-pos.md)(): [Short](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-short/index.html) |
| [prayerPoints](prayer-points.md) | [Kraken API]<br>open fun [prayerPoints](prayer-points.md)(): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) |
| [reset](reset.md) | [Kraken API]<br>open fun [reset](reset.md)(frame: [ColoFrame](../-colo-frame/index.md))<br>Resets this instance to an empty state bound to a frame. |
| [restoreDoses](restore-doses.md) | [Kraken API]<br>open fun [restoreDoses](restore-doses.md)(): [Byte](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-byte/index.html) |
| [runEnergyUnits](run-energy-units.md) | [Kraken API]<br>open fun [runEnergyUnits](run-energy-units.md)(): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) |
| [setAttackTarget](set-attack-target.md) | [Kraken API]<br>open fun [setAttackTarget](set-attack-target.md)(slot: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html))<br>Sets the player's current attack interaction; used by the snapshot builder so plans know an engagement is already in progress and no fresh attack click is needed. |
| [setCooldowns](set-cooldowns.md) | [Kraken API]<br>open fun [setCooldowns](set-cooldowns.md)(food: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), combo: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), potion: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), attack: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html))<br>Sets consumption/attack cooldowns; used by the snapshot builder to carry live timers. |
| [setJavelinAutos](set-javelin-autos.md) | [Kraken API]<br>open fun [setJavelinAutos](set-javelin-autos.md)(slot: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), autos: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html))<br>Sets a javelin colossus's auto-attack counter (from live tracking); the fifth attack after four autos is the sky javelin special. |
| [setManticoreState](set-manticore-state.md) | [Kraken API]<br>open fun [setManticoreState](set-manticore-state.md)(slot: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), patternCode: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), chargingStarted: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html), orbsRemaining: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html))<br>Marks a manticore slot's charge state and attack pattern (from live tracking). |
| [setPlayer](set-player.md) | [Kraken API]<br>open fun [setPlayer](set-player.md)(pos: [Short](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-short/index.html), hp: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), prayer: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), runEnergy: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), spec: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), overheadCode: [Byte](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-byte/index.html), gearSetIndex: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html))<br>Sets player vitals; used by the snapshot builder. |
| [setSupplies](set-supplies.md) | [Kraken API]<br>open fun [setSupplies](set-supplies.md)(food: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), combo: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), brews: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), restores: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html))<br>Sets supply counts; used by the snapshot builder. |
| [setTick](set-tick.md) | [Kraken API]<br>open fun [setTick](set-tick.md)(tick: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html))<br>Sets the wave tick; used by the snapshot builder. |
| [specEnergy](spec-energy.md) | [Kraken API]<br>open fun [specEnergy](spec-energy.md)(): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) |
| [suppliesUsed](supplies-used.md) | [Kraken API]<br>open fun [suppliesUsed](supplies-used.md)(): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) |
| [teleportPlayer](teleport-player.md) | [Kraken API]<br>open fun [teleportPlayer](teleport-player.md)(pos: [Short](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-short/index.html))<br>Moves the player instantly without touching vitals; used by tests and by the snapshot builder when re-syncing position. |
| [tick](tick.md) | [Kraken API]<br>open fun [tick](tick.md)(): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) |
| [worstCaseHpFloor](worst-case-hp-floor.md) | [Kraken API]<br>open fun [worstCaseHpFloor](worst-case-hp-floor.md)(): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) |
