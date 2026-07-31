//[kraken-api](../../../index.md)/[com.kraken.api.simulation.colosseum](../index.md)/[PlayerCommand](index.md)

# PlayerCommand

[Kraken API]\
class [PlayerCommand](index.md)

One tick's worth of player intent, bit-packed into a single `long` so search nodes can branch over thousands of commands with zero allocation. 

A command mirrors what a real player can queue in one tick: one movement click (or stop), an overhead prayer switch, a gear set swap (any number of equips resolve in the same tick), at most one food + one combo food + one potion, an attack target, and a special attack flag. [NONE](-n-o-n-e.md) keeps everything as-is.

```kotlin
bits 0-11  movement destination (packed local pos), 0xFFF = no new click
bit  12    run toggle for the movement click
bit  13    stop current movement
bits 14-16 overhead: 0 keep, 1 off, 2 protect melee, 3 protect missiles, 4 protect magic
bits 17-19 gear set: 0 keep, otherwise set index + 1
bit  20    eat primary food
bit  21    eat combo food (karambwan)
bit  22    sip Saradomin brew dose
bit  23    sip super restore dose
bit  24    use special attack on the next player attack
bits 25-30 attack target: 0 keep, 63 clear, otherwise npc slot + 1

```

## Properties

| Name | Summary |
|---|---|
| [NONE](-n-o-n-e.md) | [Kraken API]<br>val [NONE](-n-o-n-e.md): [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html) = 4095<br>The empty command: keep doing whatever the previous ticks set up. |
| [OVERHEAD_KEEP](-o-v-e-r-h-e-a-d_-k-e-e-p.md) | [Kraken API]<br>val [OVERHEAD_KEEP](-o-v-e-r-h-e-a-d_-k-e-e-p.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) = 0<br>Overhead code for `keep current`. |
| [OVERHEAD_MAGIC](-o-v-e-r-h-e-a-d_-m-a-g-i-c.md) | [Kraken API]<br>val [OVERHEAD_MAGIC](-o-v-e-r-h-e-a-d_-m-a-g-i-c.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) = 4<br>Overhead code for Protect from Magic. |
| [OVERHEAD_MELEE](-o-v-e-r-h-e-a-d_-m-e-l-e-e.md) | [Kraken API]<br>val [OVERHEAD_MELEE](-o-v-e-r-h-e-a-d_-m-e-l-e-e.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) = 2<br>Overhead code for Protect from Melee. |
| [OVERHEAD_MISSILES](-o-v-e-r-h-e-a-d_-m-i-s-s-i-l-e-s.md) | [Kraken API]<br>val [OVERHEAD_MISSILES](-o-v-e-r-h-e-a-d_-m-i-s-s-i-l-e-s.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) = 3<br>Overhead code for Protect from Missiles. |
| [OVERHEAD_OFF](-o-v-e-r-h-e-a-d_-o-f-f.md) | [Kraken API]<br>val [OVERHEAD_OFF](-o-v-e-r-h-e-a-d_-o-f-f.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) = 1<br>Overhead code for `prayers off`. |

## Functions

| Name | Summary |
|---|---|
| [attackTarget](attack-target.md) | [Kraken API]<br>open fun [attackTarget](attack-target.md)(cmd: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html)): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) |
| [eatCombo](eat-combo.md) | [Kraken API]<br>open fun [eatCombo](eat-combo.md)(cmd: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html) |
| [eatFood](eat-food.md) | [Kraken API]<br>open fun [eatFood](eat-food.md)(cmd: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html) |
| [gearSet](gear-set.md) | [Kraken API]<br>open fun [gearSet](gear-set.md)(cmd: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html)): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) |
| [moveTarget](move-target.md) | [Kraken API]<br>open fun [moveTarget](move-target.md)(cmd: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html)): [Short](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-short/index.html) |
| [moveTo](move-to.md) | [Kraken API]<br>open fun [moveTo](move-to.md)(dest: [Short](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-short/index.html), run: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)): [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html) |
| [overhead](overhead.md) | [Kraken API]<br>open fun [overhead](overhead.md)(cmd: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html)): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) |
| [run](run.md) | [Kraken API]<br>open fun [run](run.md)(cmd: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html) |
| [sipBrew](sip-brew.md) | [Kraken API]<br>open fun [sipBrew](sip-brew.md)(cmd: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html) |
| [sipRestore](sip-restore.md) | [Kraken API]<br>open fun [sipRestore](sip-restore.md)(cmd: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html) |
| [stop](stop.md) | [Kraken API]<br>open fun [stop](stop.md)(cmd: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html) |
| [useSpec](use-spec.md) | [Kraken API]<br>open fun [useSpec](use-spec.md)(cmd: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html) |
| [withAttack](with-attack.md) | [Kraken API]<br>open fun [withAttack](with-attack.md)(cmd: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html), npcSlot: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html) |
| [withAttackClear](with-attack-clear.md) | [Kraken API]<br>open fun [withAttackClear](with-attack-clear.md)(cmd: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html)): [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html) |
| [withConsumables](with-consumables.md) | [Kraken API]<br>open fun [withConsumables](with-consumables.md)(cmd: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html), food: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html), combo: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html), brew: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html), restore: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)): [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html) |
| [withGearSet](with-gear-set.md) | [Kraken API]<br>open fun [withGearSet](with-gear-set.md)(cmd: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html), gearSetIndex: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html) |
| [withMove](with-move.md) | [Kraken API]<br>open fun [withMove](with-move.md)(cmd: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html), dest: [Short](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-short/index.html), run: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)): [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html)<br>Merges a movement click into an existing command. |
| [withOverhead](with-overhead.md) | [Kraken API]<br>open fun [withOverhead](with-overhead.md)(cmd: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html), overheadCode: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html) |
| [withSpec](with-spec.md) | [Kraken API]<br>open fun [withSpec](with-spec.md)(cmd: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html)): [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html) |
| [withStop](with-stop.md) | [Kraken API]<br>open fun [withStop](with-stop.md)(cmd: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html)): [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html) |
