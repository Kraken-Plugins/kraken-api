//[kraken-api](../../../index.md)/[com.kraken.api.simulation.colosseum](../index.md)/[ColoTick](index.md)

# ColoTick

[Kraken API]\
class [ColoTick](index.md)

The colosseum tick engine: advances a [ColoState](../-colo-state/index.md) by exactly one game tick given a [PlayerCommand](../-player-command/index.md). 

Phase order per tick follows the real engine (client input, then NPC turns, then the player turn) as documented by the community engine references, and NPC behaviour is parity-modelled on the community wave simulator that has been validated against the live arena:

1. **Client input** - the command's prayer/gear/consumable/click intents apply. Because the real client processes clicks at the start of the next tick, a decision made while watching tick T-1 takes effect here on tick T - exactly how flicking works.
2. **NPC turns** - player-targeted hits were rolled on previous ticks; now each NPC (in processing order) counts down its cooldown, moves if it has no line of sight (route-finders descend a BFS approach field, everything else uses the naive diagonal-then-cardinal chase step), manticores charge as a group, and attackers with line of sight and an expired cooldown launch attacks. Protection prayers are evaluated at launch (the prayer active at the start of the attack tick), so launched hits carry final damage; only the minotaur's delayed melee re-checks prayer on landing.
3. **Player turn** - queued hits landing this tick apply (tick-eating works because phase 1 healing already happened), then movement (walk 1 / run 2 tiles along a BFS field toward the clicked destination), then the post-movement attack attempt.
4. **Housekeeping** - cooldown decrements, prayer drain, run energy and special attack regeneration.

Damage is tracked as expected value (accuracy x mean roll) for optimisation plus a parallel worst-case burst floor for lethality checks; both live on the state.

## Types

| Name | Summary |
|---|---|
| [AttackListener](-attack-listener/index.md) | [Kraken API]<br>interface [AttackListener](-attack-listener/index.md)<br>Observer for NPC attack launches, wired through [attackListener](../-colo-scratch/attack-listener.md). |

## Properties

| Name | Summary |
|---|---|
| [JAGUAR_HITS_PER_ATTACK](-j-a-g-u-a-r_-h-i-t-s_-p-e-r_-a-t-t-a-c-k.md) | [Kraken API]<br>val [JAGUAR_HITS_PER_ATTACK](-j-a-g-u-a-r_-h-i-t-s_-p-e-r_-a-t-t-a-c-k.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) = 3<br>Jaguar warriors roll three independent melee hits per attack. |
| [MANTICORE_MAX_HIT_MAGIC](-m-a-n-t-i-c-o-r-e_-m-a-x_-h-i-t_-m-a-g-i-c.md) | [Kraken API]<br>val [MANTICORE_MAX_HIT_MAGIC](-m-a-n-t-i-c-o-r-e_-m-a-x_-h-i-t_-m-a-g-i-c.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) = 31 |
| [MANTICORE_MAX_HIT_MELEE](-m-a-n-t-i-c-o-r-e_-m-a-x_-h-i-t_-m-e-l-e-e.md) | [Kraken API]<br>val [MANTICORE_MAX_HIT_MELEE](-m-a-n-t-i-c-o-r-e_-m-a-x_-h-i-t_-m-e-l-e-e.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) = 31<br>Manticore per-style max hits (wiki: melee 31, ranged 36, magic 31). |
| [MANTICORE_MAX_HIT_RANGED](-m-a-n-t-i-c-o-r-e_-m-a-x_-h-i-t_-r-a-n-g-e-d.md) | [Kraken API]<br>val [MANTICORE_MAX_HIT_RANGED](-m-a-n-t-i-c-o-r-e_-m-a-x_-h-i-t_-r-a-n-g-e-d.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) = 36 |
| [ORB_STYLES](-o-r-b_-s-t-y-l-e-s.md) | [Kraken API]<br>val [ORB_STYLES](-o-r-b_-s-t-y-l-e-s.md): [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)&gt;&gt;<br>Per-orb style lookup indexed by [patternCode][orbIndex]; pattern 0 is unknown. |
| [PATTERN_MAGIC_FIRST](-p-a-t-t-e-r-n_-m-a-g-i-c_-f-i-r-s-t.md) | [Kraken API]<br>val [PATTERN_MAGIC_FIRST](-p-a-t-t-e-r-n_-m-a-g-i-c_-f-i-r-s-t.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) = 2<br>Manticore pattern: magic, ranged, melee. |
| [PATTERN_MAGIC_MELEE_RANGED](-p-a-t-t-e-r-n_-m-a-g-i-c_-m-e-l-e-e_-r-a-n-g-e-d.md) | [Kraken API]<br>val [PATTERN_MAGIC_MELEE_RANGED](-p-a-t-t-e-r-n_-m-a-g-i-c_-m-e-l-e-e_-r-a-n-g-e-d.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) = 6<br>Mantimayhem III pattern: magic, melee, ranged. |
| [PATTERN_MELEE_MAGIC_RANGED](-p-a-t-t-e-r-n_-m-e-l-e-e_-m-a-g-i-c_-r-a-n-g-e-d.md) | [Kraken API]<br>val [PATTERN_MELEE_MAGIC_RANGED](-p-a-t-t-e-r-n_-m-e-l-e-e_-m-a-g-i-c_-r-a-n-g-e-d.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) = 4<br>Mantimayhem III pattern: melee, magic, ranged. |
| [PATTERN_MELEE_RANGED_MAGIC](-p-a-t-t-e-r-n_-m-e-l-e-e_-r-a-n-g-e-d_-m-a-g-i-c.md) | [Kraken API]<br>val [PATTERN_MELEE_RANGED_MAGIC](-p-a-t-t-e-r-n_-m-e-l-e-e_-r-a-n-g-e-d_-m-a-g-i-c.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) = 3<br>Mantimayhem III pattern: melee, ranged, magic. |
| [PATTERN_RANGED_FIRST](-p-a-t-t-e-r-n_-r-a-n-g-e-d_-f-i-r-s-t.md) | [Kraken API]<br>val [PATTERN_RANGED_FIRST](-p-a-t-t-e-r-n_-r-a-n-g-e-d_-f-i-r-s-t.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) = 1<br>Manticore pattern: ranged, magic, melee. |
| [PATTERN_RANGED_MELEE_MAGIC](-p-a-t-t-e-r-n_-r-a-n-g-e-d_-m-e-l-e-e_-m-a-g-i-c.md) | [Kraken API]<br>val [PATTERN_RANGED_MELEE_MAGIC](-p-a-t-t-e-r-n_-r-a-n-g-e-d_-m-e-l-e-e_-m-a-g-i-c.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) = 5<br>Mantimayhem III pattern: ranged, melee, magic. |
| [PATTERN_UNKNOWN](-p-a-t-t-e-r-n_-u-n-k-n-o-w-n.md) | [Kraken API]<br>val [PATTERN_UNKNOWN](-p-a-t-t-e-r-n_-u-n-k-n-o-w-n.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) = 0<br>Manticore pattern: not yet known. |
| [STYLE_MAGIC](-s-t-y-l-e_-m-a-g-i-c.md) | [Kraken API]<br>val [STYLE_MAGIC](-s-t-y-l-e_-m-a-g-i-c.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) = 3<br>Hit style: magic. |
| [STYLE_MELEE](-s-t-y-l-e_-m-e-l-e-e.md) | [Kraken API]<br>val [STYLE_MELEE](-s-t-y-l-e_-m-e-l-e-e.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) = 1<br>Hit style: melee. |
| [STYLE_RANGED](-s-t-y-l-e_-r-a-n-g-e-d.md) | [Kraken API]<br>val [STYLE_RANGED](-s-t-y-l-e_-r-a-n-g-e-d.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) = 2<br>Hit style: ranged. |
| [STYLE_TYPELESS](-s-t-y-l-e_-t-y-p-e-l-e-s-s.md) | [Kraken API]<br>val [STYLE_TYPELESS](-s-t-y-l-e_-t-y-p-e-l-e-s-s.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) = 0<br>Hit style: typeless (prayer never applies). |

## Functions

| Name | Summary |
|---|---|
| [advance](advance.md) | [Kraken API]<br>open fun [advance](advance.md)(s: [ColoState](../-colo-state/index.md), cmd: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html), scratch: [ColoScratch](../-colo-scratch/index.md))<br>Advances the state by one tick. |
| [hitExpectedDamage](hit-expected-damage.md) | [Kraken API]<br>open fun [hitExpectedDamage](hit-expected-damage.md)(hit: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html)): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) |
| [hitLandTick](hit-land-tick.md) | [Kraken API]<br>open fun [hitLandTick](hit-land-tick.md)(hit: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html)): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)<br>Decodes a pending hit's landing tick, used by overlays and the scorer. |
| [hitMaxDamage](hit-max-damage.md) | [Kraken API]<br>open fun [hitMaxDamage](hit-max-damage.md)(hit: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html)): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) |
| [hitSlot](hit-slot.md) | [Kraken API]<br>open fun [hitSlot](hit-slot.md)(hit: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html)): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) |
| [hitStyle](hit-style.md) | [Kraken API]<br>open fun [hitStyle](hit-style.md)(hit: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html)): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) |
| [hitTargetsNpc](hit-targets-npc.md) | [Kraken API]<br>open fun [hitTargetsNpc](hit-targets-npc.md)(hit: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html) |
| [npcHasLosToPlayer](npc-has-los-to-player.md) | [Kraken API]<br>open fun [npcHasLosToPlayer](npc-has-los-to-player.md)(s: [ColoState](../-colo-state/index.md), slot: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>Whether an NPC currently has line of sight (and range) to the player. |
| [npcHasLosToTile](npc-has-los-to-tile.md) | [Kraken API]<br>open fun [npcHasLosToTile](npc-has-los-to-tile.md)(s: [ColoState](../-colo-state/index.md), slot: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), tile: [Short](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-short/index.html)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>Whether an NPC would have line of sight (and range) to a hypothetical player tile. |
| [predictNextNpcPos](predict-next-npc-pos.md) | [Kraken API]<br>open fun [predictNextNpcPos](predict-next-npc-pos.md)(s: [ColoState](../-colo-state/index.md), scratch: [ColoScratch](../-colo-scratch/index.md), slot: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [Short](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-short/index.html)<br>Predicts the tile an NPC would step to this tick if it moved (ignoring wave-start gating and the has-line-of-sight hold). |
