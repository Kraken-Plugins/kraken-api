//[lib](../../../index.md)/[com.kraken.api.simulation.colosim](../index.md)/[Simulation](index.md)

# Simulation

[Kraken API]\
open class [Simulation](index.md)

## Constructors

| | |
|---|---|
| [Simulation](-simulation.md) | [Kraken API]<br>constructor() |

## Types

| Name | Summary |
|---|---|
| [StepResult](-step-result/index.md) | [Kraken API]<br>class [StepResult](-step-result/index.md) |

## Properties

| Name | Summary |
|---|---|
| [DELAY_FIRST_ATTACK_TICKS](-d-e-l-a-y_-f-i-r-s-t_-a-t-t-a-c-k_-t-i-c-k-s.md) | [Kraken API]<br>val [DELAY_FIRST_ATTACK_TICKS](-d-e-l-a-y_-f-i-r-s-t_-a-t-t-a-c-k_-t-i-c-k-s.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) = 3 |
| [MANTICORE](-m-a-n-t-i-c-o-r-e.md) | [Kraken API]<br>val [MANTICORE](-m-a-n-t-i-c-o-r-e.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) |
| [MANTICORE_CHARGE_TIME](-m-a-n-t-i-c-o-r-e_-c-h-a-r-g-e_-t-i-m-e.md) | [Kraken API]<br>val [MANTICORE_CHARGE_TIME](-m-a-n-t-i-c-o-r-e_-c-h-a-r-g-e_-t-i-m-e.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) = 10 |
| [MANTICORE_DELAY](-m-a-n-t-i-c-o-r-e_-d-e-l-a-y.md) | [Kraken API]<br>val [MANTICORE_DELAY](-m-a-n-t-i-c-o-r-e_-d-e-l-a-y.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) = 5 |
| [MAP_HEIGHT](-m-a-p_-h-e-i-g-h-t.md) | [Kraken API]<br>val [MAP_HEIGHT](-m-a-p_-h-e-i-g-h-t.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) = 34 |
| [MAP_WIDTH](-m-a-p_-w-i-d-t-h.md) | [Kraken API]<br>val [MAP_WIDTH](-m-a-p_-w-i-d-t-h.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) = 34 |
| [MINOTAUR](-m-i-n-o-t-a-u-r.md) | [Kraken API]<br>val [MINOTAUR](-m-i-n-o-t-a-u-r.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) |
| [MINOTAUR_HEAL_RANGE](-m-i-n-o-t-a-u-r_-h-e-a-l_-r-a-n-g-e.md) | [Kraken API]<br>val [MINOTAUR_HEAL_RANGE](-m-i-n-o-t-a-u-r_-h-e-a-l_-r-a-n-g-e.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) = 7 |

## Functions

| Name | Summary |
|---|---|
| [canAttackPlayer](can-attack-player.md) | [Kraken API]<br>open fun [canAttackPlayer](can-attack-player.md)(mob: [Mob](../../com.kraken.api.simulation.colosim.model/-mob/index.md)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html) |
| [clear](clear.md) | [Kraken API]<br>open fun [clear](clear.md)() |
| [decodeManticoreAttack](decode-manticore-attack.md) | [Kraken API]<br>open fun [decodeManticoreAttack](decode-manticore-attack.md)(styleIndex: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html) |
| [findMobIndexAtTile](find-mob-index-at-tile.md) | [Kraken API]<br>open fun [findMobIndexAtTile](find-mob-index-at-tile.md)(x: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), y: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) |
| [getBlockedTileRanges](get-blocked-tile-ranges.md) | [Kraken API]<br>open fun [getBlockedTileRanges](get-blocked-tile-ranges.md)(): [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)&gt;&gt;&gt; |
| [getManticorePattern](get-manticore-pattern.md) | [Kraken API]<br>open fun [getManticorePattern](get-manticore-pattern.md)(name: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)): [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)&gt; |
| [getMobCenterOffset](get-mob-center-offset.md) | [Kraken API]<br>open fun [getMobCenterOffset](get-mob-center-offset.md)(type: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) |
| [getNpcInfo](get-npc-info.md) | [Kraken API]<br>open fun [getNpcInfo](get-npc-info.md)(type: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [NpcInfo](../../com.kraken.api.simulation.colosim.model/-npc-info/index.md) |
| [getNpcName](get-npc-name.md) | [Kraken API]<br>open fun [getNpcName](get-npc-name.md)(type: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html) |
| [getPillarFilters](get-pillar-filters.md) | [Kraken API]<br>open fun [getPillarFilters](get-pillar-filters.md)(): [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)&gt; |
| [hasLOS](has-l-o-s.md) | [Kraken API]<br>open fun [hasLOS](has-l-o-s.md)(x1: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), y1: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), x2: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), y2: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), s: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), r: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), isNPC: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html) |
| [placeMob](place-mob.md) | [Kraken API]<br>open fun [placeMob](place-mob.md)(x: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), y: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), type: [NpcType](../-npc-type/index.md), extra: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html) |
| [removeMobAtTile](remove-mob-at-tile.md) | [Kraken API]<br>open fun [removeMobAtTile](remove-mob-at-tile.md)(x: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), y: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)) |
| [reset](reset.md) | [Kraken API]<br>open fun [reset](reset.md)() |
| [setPlayer](set-player.md) | [Kraken API]<br>open fun [setPlayer](set-player.md)(x: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), y: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)) |
| [step](step.md) | [Kraken API]<br>open fun [step](step.md)(): [Simulation.StepResult](-step-result/index.md) |
