//[lib](../../../index.md)/[com.kraken.api.query.player](../index.md)/[LocalPlayerEntity](index.md)/[toggleSpecialAttack](toggle-special-attack.md)

# toggleSpecialAttack

[Kraken API]\
open fun [toggleSpecialAttack](toggle-special-attack.md)(energyRequired: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html))

Sets the special attack state if current special attack energy is greater than or equal to the required special attack energy

#### Parameters

Kraken API

| | |
|---|---|
| energyRequired | int, 100 = 100% |

[Kraken API]\
open fun [toggleSpecialAttack](toggle-special-attack.md)(energyRequired: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), delay: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html))

Sets the special attack state if the current special attack energy is greater than or equal to the required special attack energy using reflection instead of mouse events.

#### Parameters

Kraken API

| | |
|---|---|
| energyRequired | int, 100 = 100% |
| delay | int a set delay before the spec button is pressed. This can't happen instantaneously because the server needs to process the weapon equip before it can toggle on spec. Otherwise, the game would see you toggle on spec for nothing, then spec weapon gets equipped with spec disabled. |
