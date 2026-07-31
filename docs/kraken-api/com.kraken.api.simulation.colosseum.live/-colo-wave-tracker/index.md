//[kraken-api](../../../index.md)/[com.kraken.api.simulation.colosseum.live](../index.md)/[ColoWaveTracker](index.md)

# ColoWaveTracker

[Kraken API]\
class [ColoWaveTracker](index.md)

Persistent per-wave observation state that a single snapshot cannot provide: attack cooldowns (from observed attack animations), manticore charge states and patterns (from spot-anims), javelin special counters, the warband attack-cycle phase, and the wave tick. 

The hosting plugin forwards RuneLite events into this tracker ([onGameTick](on-game-tick.md), [onNpcSpawned](on-npc-spawned.md), [onNpcDespawned](on-npc-despawned.md), [onAnimationChanged](on-animation-changed.md)); [ColoCapture](../-colo-capture/index.md) then reads it while building the simulation state. Everything here is an estimate that self-corrects every tick - the planner replans from a fresh capture each tick, which is what makes the system adapt to surprises (reinforcements, missed observations, lag).

## Constructors

| | |
|---|---|
| [ColoWaveTracker](-colo-wave-tracker.md) | [Kraken API]<br>constructor() |

## Types

| Name | Summary |
|---|---|
| [NpcRecord](-npc-record/index.md) | [Kraken API]<br>class [NpcRecord](-npc-record/index.md)<br>Per-NPC observation record. |

## Functions

| Name | Summary |
|---|---|
| [cooldownEstimate](cooldown-estimate.md) | [Kraken API]<br>open fun [cooldownEstimate](cooldown-estimate.md)(record: [ColoWaveTracker.NpcRecord](-npc-record/index.md)): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)<br>Estimates an NPC's current attack cooldown from its last observed attack. |
| [isWaveActive](is-wave-active.md) | [Kraken API]<br>open fun [isWaveActive](is-wave-active.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html) |
| [javelinAutosSinceSpecial](javelin-autos-since-special.md) | [Kraken API]<br>open fun [javelinAutosSinceSpecial](javelin-autos-since-special.md)(record: [ColoWaveTracker.NpcRecord](-npc-record/index.md)): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) |
| [lastKnownHp](last-known-hp.md) | [Kraken API]<br>open fun [lastKnownHp](last-known-hp.md)(record: [ColoWaveTracker.NpcRecord](-npc-record/index.md)): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) |
| [manticoreChargingStarted](manticore-charging-started.md) | [Kraken API]<br>open fun [manticoreChargingStarted](manticore-charging-started.md)(record: [ColoWaveTracker.NpcRecord](-npc-record/index.md)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html) |
| [manticoreOrbsRemaining](manticore-orbs-remaining.md) | [Kraken API]<br>open fun [manticoreOrbsRemaining](manticore-orbs-remaining.md)(record: [ColoWaveTracker.NpcRecord](-npc-record/index.md)): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) |
| [markWaveStart](mark-wave-start.md) | [Kraken API]<br>open fun [markWaveStart](mark-wave-start.md)()<br>Manually restarts the wave clock (e.g. |
| [onAnimationChanged](on-animation-changed.md) | [Kraken API]<br>open fun [onAnimationChanged](on-animation-changed.md)(actor: Actor, localPlayer: Player)<br>Records attack animations for cooldown estimation. |
| [onGameTick](on-game-tick.md) | [Kraken API]<br>open fun [onGameTick](on-game-tick.md)()<br>Advances the wave clock and refreshes per-NPC spot-anim observations. |
| [onNpcDespawned](on-npc-despawned.md) | [Kraken API]<br>open fun [onNpcDespawned](on-npc-despawned.md)(npc: NPC)<br>Removes a despawned NPC; an empty arena ends the wave clock. |
| [onNpcSpawned](on-npc-spawned.md) | [Kraken API]<br>open fun [onNpcSpawned](on-npc-spawned.md)(npc: NPC)<br>Registers a spawned colosseum NPC; the first spawn into an empty arena marks the start of a wave. |
| [onPlayerAte](on-player-ate.md) | [Kraken API]<br>open fun [onPlayerAte](on-player-ate.md)()<br>Marks that the player just ate (hosting plugins may call this from inventory or chat events to tighten the consumption-timer estimates). |
| [playerAttackDelayEstimate](player-attack-delay-estimate.md) | [Kraken API]<br>open fun [playerAttackDelayEstimate](player-attack-delay-estimate.md)(weaponSpeedTicks: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) |
| [playerFoodDelayEstimate](player-food-delay-estimate.md) | [Kraken API]<br>open fun [playerFoodDelayEstimate](player-food-delay-estimate.md)(): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) |
| [record](record.md) | [Kraken API]<br>open fun [record](record.md)(npcIndex: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [ColoWaveTracker.NpcRecord](-npc-record/index.md) |
| [records](records.md) | [Kraken API]<br>open fun [records](records.md)(): [Iterable](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/Iterable.html)&lt;[ColoWaveTracker.NpcRecord](-npc-record/index.md)&gt; |
| [reset](reset.md) | [Kraken API]<br>open fun [reset](reset.md)()<br>Clears all observations. |
