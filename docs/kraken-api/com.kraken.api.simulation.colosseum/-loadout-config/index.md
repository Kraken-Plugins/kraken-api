//[kraken-api](../../../index.md)/[com.kraken.api.simulation.colosseum](../index.md)/[LoadoutConfig](index.md)

# LoadoutConfig

[Kraken API]\
class [LoadoutConfig](index.md)

Player capability configuration for the colosseum simulation: gear sets with expected damage output per NPC type, supply heal values, and the passive stats (prayer bonus, agility, weight) that drive drain/regen formulas. 

Expected damage values are deliberately simple averages (accuracy x mean hit) because the planner optimises expected outcomes; exact damage rolls are unknowable one tick ahead. Callers should tune GearSet#getExpectedDamageByType() to their actual gear.

## Constructors

| | |
|---|---|
| [LoadoutConfig](-loadout-config.md) | [Kraken API]<br>constructor() |

## Types

| Name | Summary |
|---|---|
| [GearSet](-gear-set/index.md) | [Kraken API]<br>class [GearSet](-gear-set/index.md)<br>One switchable equipment loadout (weapon + armour swap executed as one batch of same-tick equips). |

## Properties

| Name | Summary |
|---|---|
| [SET_MAGIC](-s-e-t_-m-a-g-i-c.md) | [Kraken API]<br>val [SET_MAGIC](-s-e-t_-m-a-g-i-c.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) = 2<br>Index of the magic gear set in getGearSets() for the default loadout. |
| [SET_MELEE](-s-e-t_-m-e-l-e-e.md) | [Kraken API]<br>val [SET_MELEE](-s-e-t_-m-e-l-e-e.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) = 0<br>Index of the melee gear set in getGearSets() for the default loadout. |
| [SET_RANGED](-s-e-t_-r-a-n-g-e-d.md) | [Kraken API]<br>val [SET_RANGED](-s-e-t_-r-a-n-g-e-d.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) = 1<br>Index of the ranged gear set in getGearSets() for the default loadout. |

## Functions

| Name | Summary |
|---|---|
| [defaults](defaults.md) | [Kraken API]<br>open fun [defaults](defaults.md)(): [LoadoutConfig](index.md)<br>Builds a sensible high-level default loadout: fang-style melee, twisted-bow-style ranged and shadow-style magic sets with weakness-aware expected damage, anglerfish, karambwans, brews and restores. |
| [gearSet](gear-set.md) | [Kraken API]<br>open fun [gearSet](gear-set.md)(index: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [LoadoutConfig.GearSet](-gear-set/index.md) |
| [gearSetCount](gear-set-count.md) | [Kraken API]<br>open fun [gearSetCount](gear-set-count.md)(): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) |
