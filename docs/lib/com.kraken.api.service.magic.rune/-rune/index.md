//[lib](../../../index.md)/[com.kraken.api.service.magic.rune](../index.md)/[Rune](index.md)

# Rune

[Kraken API]\
enum [Rune](index.md)

## Entries

| | |
|---|---|
| [AIR](-a-i-r/index.md) | [Kraken API]<br>[AIR](-a-i-r/index.md) |
| [WATER](-w-a-t-e-r/index.md) | [Kraken API]<br>[WATER](-w-a-t-e-r/index.md) |
| [EARTH](-e-a-r-t-h/index.md) | [Kraken API]<br>[EARTH](-e-a-r-t-h/index.md) |
| [FIRE](-f-i-r-e/index.md) | [Kraken API]<br>[FIRE](-f-i-r-e/index.md) |
| [MIND](-m-i-n-d/index.md) | [Kraken API]<br>[MIND](-m-i-n-d/index.md) |
| [CHAOS](-c-h-a-o-s/index.md) | [Kraken API]<br>[CHAOS](-c-h-a-o-s/index.md) |
| [DEATH](-d-e-a-t-h/index.md) | [Kraken API]<br>[DEATH](-d-e-a-t-h/index.md) |
| [BLOOD](-b-l-o-o-d/index.md) | [Kraken API]<br>[BLOOD](-b-l-o-o-d/index.md) |
| [COSMIC](-c-o-s-m-i-c/index.md) | [Kraken API]<br>[COSMIC](-c-o-s-m-i-c/index.md) |
| [NATURE](-n-a-t-u-r-e/index.md) | [Kraken API]<br>[NATURE](-n-a-t-u-r-e/index.md) |
| [LAW](-l-a-w/index.md) | [Kraken API]<br>[LAW](-l-a-w/index.md) |
| [BODY](-b-o-d-y/index.md) | [Kraken API]<br>[BODY](-b-o-d-y/index.md) |
| [SOUL](-s-o-u-l/index.md) | [Kraken API]<br>[SOUL](-s-o-u-l/index.md) |
| [ASTRAL](-a-s-t-r-a-l/index.md) | [Kraken API]<br>[ASTRAL](-a-s-t-r-a-l/index.md) |
| [MIST](-m-i-s-t/index.md) | [Kraken API]<br>[MIST](-m-i-s-t/index.md) |
| [MUD](-m-u-d/index.md) | [Kraken API]<br>[MUD](-m-u-d/index.md) |
| [DUST](-d-u-s-t/index.md) | [Kraken API]<br>[DUST](-d-u-s-t/index.md) |
| [LAVA](-l-a-v-a/index.md) | [Kraken API]<br>[LAVA](-l-a-v-a/index.md) |
| [STEAM](-s-t-e-a-m/index.md) | [Kraken API]<br>[STEAM](-s-t-e-a-m/index.md) |
| [SMOKE](-s-m-o-k-e/index.md) | [Kraken API]<br>[SMOKE](-s-m-o-k-e/index.md) |
| [WRATH](-w-r-a-t-h/index.md) | [Kraken API]<br>[WRATH](-w-r-a-t-h/index.md) |
| [SUNFIRE](-s-u-n-f-i-r-e/index.md) | [Kraken API]<br>[SUNFIRE](-s-u-n-f-i-r-e/index.md) |
| [AETHER](-a-e-t-h-e-r/index.md) | [Kraken API]<br>[AETHER](-a-e-t-h-e-r/index.md) |

## Functions

| Name | Summary |
|---|---|
| [byItemId](by-item-id.md) | [Kraken API]<br>open fun [byItemId](by-item-id.md)(itemId: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [Rune](index.md)<br>Retrieves the `Rune` associated with the specified item ID. |
| [getComboRunes](get-combo-runes.md) | [Kraken API]<br>open fun [getComboRunes](get-combo-runes.md)(rune: [Rune](index.md)): [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[Rune](index.md)&gt;<br>Retrieves an array of combo runes that can be derived from the specified base `Rune`. |
| [isComboRune](is-combo-rune.md) | [Kraken API]<br>open fun [isComboRune](is-combo-rune.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>Determines whether this `Rune` is a combo rune. |
| [providesRune](provides-rune.md) | [Kraken API]<br>open fun [providesRune](provides-rune.md)(rune: [Rune](index.md)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>Determines whether this `Rune` provides the specified `Rune`. |
| [valueOf](value-of.md) | [Kraken API]<br>open fun [valueOf](value-of.md)(name: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)): [Rune](index.md)<br>Returns the enum constant of this type with the specified name. The string must match exactly an identifier used to declare an enum constant in this type. (Extraneous whitespace characters are not permitted.) |
| [values](values.md) | [Kraken API]<br>open fun [values](values.md)(): [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[Rune](index.md)&gt;<br>Returns an array containing the constants of this enum type, in the order they're declared. This method may be used to iterate over the constants. |
