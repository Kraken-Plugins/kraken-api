//[kraken-api](../../../index.md)/[com.kraken.api.service.magic.rune](../index.md)/[Rune](index.md)/[providesRune](provides-rune.md)

# providesRune

[Kraken API]\
open fun [providesRune](provides-rune.md)(rune: [Rune](index.md)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)

Determines whether this `Rune` provides the specified `Rune`. 

 This method checks if the current `Rune` is equal to the provided `Rune`, or if the provided `Rune` is included in this `Rune`'s base runes. Base runes represent the elemental components of a combo `Rune`. 

#### Return

`true` if the specified `Rune` is equal to this `Rune`, or if it is contained within this `Rune`'s base runes; `false` otherwise.

#### Parameters

Kraken API

| | |
|---|---|
| rune | The `Rune` to check against this `Rune`. If `rune` is `null`, the method will return `false`. |
