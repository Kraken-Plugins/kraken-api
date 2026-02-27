//[lib](../../../index.md)/[com.kraken.api.service.magic.rune](../index.md)/[Rune](index.md)/[isComboRune](is-combo-rune.md)

# isComboRune

[Kraken API]\
open fun [isComboRune](is-combo-rune.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)

Determines whether this `Rune` is a combo rune. 

 A combo rune is a type of rune that consists of multiple elemental components, represented internally by an array of base runes. This method checks if the `Rune` has any associated base runes. 

#### Return

`true` if this `Rune` has one or more associated base runes, indicating it is a combo rune; `false` otherwise.
