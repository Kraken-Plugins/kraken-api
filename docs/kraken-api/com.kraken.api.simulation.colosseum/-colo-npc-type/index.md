//[kraken-api](../../../index.md)/[com.kraken.api.simulation.colosseum](../index.md)/[ColoNpcType](index.md)

# ColoNpcType

[Kraken API]\
enum [ColoNpcType](index.md)

Combat and behaviour definitions for every simulated Fortis Colosseum wave NPC (Sol Heredit is intentionally excluded). 

Stats sourced from the OSRS Wiki monster pages (July 2026): hitpoints, footprint size, attack style/range/speed and max hits. Behavioural details (manticore charge and stagger, javelin sky special cadence, minotaur heal rule, warband route-finding and 6-tick global attack cycle) come from the wiki strategy pages and the community wave simulator that this engine's tick loop is parity-tested against.

## Entries

| | |
|---|---|
| [MANTICORE](-m-a-n-t-i-c-o-r-e/index.md) | [Kraken API]<br>[MANTICORE](-m-a-n-t-i-c-o-r-e/index.md)<br>Manticore: charges a triple attack (10 ticks) on first line of sight, then fires three orbs on consecutive ticks - ranged/magic in a fixed per-spawn order, melee always last (Mantimayhem III unlocks melee-first patterns). Only one manticore may begin firing per tick; other ready manticores are delayed 5 ticks. Prayer is checked on each orb's launch tick (projectiles have no travel time). |
| [SERPENT_SHAMAN](-s-e-r-p-e-n-t_-s-h-a-m-a-n/index.md) | [Kraken API]<br>[SERPENT_SHAMAN](-s-e-r-p-e-n-t_-s-h-a-m-a-n/index.md)<br>Serpent shaman: accurate 10-range Water Surge caster, no special mechanics. |
| [JAVELIN_COLOSSUS](-j-a-v-e-l-i-n_-c-o-l-o-s-s-u-s/index.md) | [Kraken API]<br>[JAVELIN_COLOSSUS](-j-a-v-e-l-i-n_-c-o-l-o-s-s-u-s/index.md)<br>Javelin colossus: 15-range thrower. Every fifth attack is a sky javelin aimed at the player's tile that lands 6 ticks later for up to 40 typeless damage - dodged by moving, not by prayer. |
| [SHOCKWAVE_COLOSSUS](-s-h-o-c-k-w-a-v-e_-c-o-l-o-s-s-u-s/index.md) | [Kraken API]<br>[SHOCKWAVE_COLOSSUS](-s-h-o-c-k-w-a-v-e_-c-o-l-o-s-s-u-s/index.md)<br>Shockwave colossus: hard-hitting 15-range mage, no special mechanics. |
| [JAGUAR_WARRIOR](-j-a-g-u-a-r_-w-a-r-r-i-o-r/index.md) | [Kraken API]<br>[JAGUAR_WARRIOR](-j-a-g-u-a-r_-w-a-r-r-i-o-r/index.md)<br>Jaguar warrior: melee reinforcement that rolls three independent hits per attack. |
| [MINOTAUR](-m-i-n-o-t-a-u-r/index.md) | [Kraken API]<br>[MINOTAUR](-m-i-n-o-t-a-u-r/index.md)<br>Minotaur: melee reinforcement. When no player is in melee reach on its attack timer it scans for a damaged NPC (below 75% max HP, centre within 7 tiles, line of sight, not a minotaur) and heals it instead. Its melee damage is applied one tick after the attack. |
| [MINOTAUR_RED_FLAG](-m-i-n-o-t-a-u-r_-r-e-d_-f-l-a-g/index.md) | [Kraken API]<br>[MINOTAUR_RED_FLAG](-m-i-n-o-t-a-u-r_-r-e-d_-f-l-a-g/index.md)<br>Red Flag minotaur variant: identical stats but route-finds around pillars. |
| [FREMENNIK_BERSERKER](-f-r-e-m-e-n-n-i-k_-b-e-r-s-e-r-k-e-r/index.md) | [Kraken API]<br>[FREMENNIK_BERSERKER](-f-r-e-m-e-n-n-i-k_-b-e-r-s-e-r-k-e-r/index.md)<br>Fremennik berserker: route-finds around pillars, attacks only from melee reach while stationary, on the shared warband 6-tick cycle. Weak to magic (one-shot by powered staves). |
| [FREMENNIK_SEER](-f-r-e-m-e-n-n-i-k_-s-e-e-r/index.md) | [Kraken API]<br>[FREMENNIK_SEER](-f-r-e-m-e-n-n-i-k_-s-e-e-r/index.md)<br>Fremennik seer: magic damage but otherwise identical warband behaviour; weak to ranged. |
| [FREMENNIK_ARCHER](-f-r-e-m-e-n-n-i-k_-a-r-c-h-e-r/index.md) | [Kraken API]<br>[FREMENNIK_ARCHER](-f-r-e-m-e-n-n-i-k_-a-r-c-h-e-r/index.md)<br>Fremennik archer: ranged damage but otherwise identical warband behaviour; weak to melee. |

## Types

| Name | Summary |
|---|---|
| [SpecialKind](-special-kind/index.md) | [Kraken API]<br>enum [SpecialKind](-special-kind/index.md)<br>Special behaviour hooks the tick engine dispatches on. |

## Functions

| Name | Summary |
|---|---|
| [fromNpcId](from-npc-id.md) | [Kraken API]<br>open fun [fromNpcId](from-npc-id.md)(npcId: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [ColoNpcType](index.md)<br>Resolves a colosseum NPC type from a RuneLite NPC id. |
| [isMeleeReach](is-melee-reach.md) | [Kraken API]<br>open fun [isMeleeReach](is-melee-reach.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html) |
| [isWarband](is-warband.md) | [Kraken API]<br>open fun [isWarband](is-warband.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html) |
| [valueOf](value-of.md) | [Kraken API]<br>open fun [valueOf](value-of.md)(name: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)): [ColoNpcType](index.md)<br>Returns the enum constant of this type with the specified name. The string must match exactly an identifier used to declare an enum constant in this type. (Extraneous whitespace characters are not permitted.) |
| [values](values.md) | [Kraken API]<br>open fun [values](values.md)(): [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[ColoNpcType](index.md)&gt;<br>Returns an array containing the constants of this enum type, in the order they're declared. This method may be used to iterate over the constants. |
