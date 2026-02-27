//[lib](../../../index.md)/[com.kraken.api.service.magic.rune](../index.md)/[Rune](index.md)/[getComboRunes](get-combo-runes.md)

# getComboRunes

[Kraken API]\
open fun [getComboRunes](get-combo-runes.md)(rune: [Rune](index.md)): [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[Rune](index.md)&gt;

Retrieves an array of combo runes that can be derived from the specified base `Rune`. 

 A combo rune is defined as a rune that includes the specified base rune as one of its elemental components. This method will return all applicable combo runes for the given input rune. 

#### Return

An array of `Rune` representing all combo runes that include the provided base rune. If no combo runes are found for the given rune, an empty array is returned.

#### Parameters

Kraken API

| | |
|---|---|
| rune | The base `Rune` for which to find applicable combo runes. If the specified rune is not part of any combo rune, an empty array will be returned. |
