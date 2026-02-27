//[lib](../../../index.md)/[com.kraken.api.service.magic.rune](../index.md)/[RunePouch](index.md)

# RunePouch

[Kraken API]\
enum [RunePouch](index.md)

## Entries

| | |
|---|---|
| [RUNE_POUCH](-r-u-n-e_-p-o-u-c-h/index.md) | [Kraken API]<br>[RUNE_POUCH](-r-u-n-e_-p-o-u-c-h/index.md) |
| [DIVINE_RUNE_POUCH](-d-i-v-i-n-e_-r-u-n-e_-p-o-u-c-h/index.md) | [Kraken API]<br>[DIVINE_RUNE_POUCH](-d-i-v-i-n-e_-r-u-n-e_-p-o-u-c-h/index.md) |
| [DIVINE_RUNE_POUCH_L](-d-i-v-i-n-e_-r-u-n-e_-p-o-u-c-h_-l/index.md) | [Kraken API]<br>[DIVINE_RUNE_POUCH_L](-d-i-v-i-n-e_-r-u-n-e_-p-o-u-c-h_-l/index.md) |
| [RUNE_POUCH_L](-r-u-n-e_-p-o-u-c-h_-l/index.md) | [Kraken API]<br>[RUNE_POUCH_L](-r-u-n-e_-p-o-u-c-h_-l/index.md) |
| [RUNE_POUCH_LMS](-r-u-n-e_-p-o-u-c-h_-l-m-s/index.md) | [Kraken API]<br>[RUNE_POUCH_LMS](-r-u-n-e_-p-o-u-c-h_-l-m-s/index.md) |

## Functions

| Name | Summary |
|---|---|
| [byItemId](by-item-id.md) | [Kraken API]<br>open fun [byItemId](by-item-id.md)(itemId: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [RunePouch](index.md)<br>Retrieves the @code RunePouch instance corresponding to the specified item ID. |
| [getBaseRunePouchContents](get-base-rune-pouch-contents.md) | [Kraken API]<br>open fun [getBaseRunePouchContents](get-base-rune-pouch-contents.md)(): [Map](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/Map.html)&lt;[Rune](../-rune/index.md), [Integer](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/Integer.html)&gt;<br>Retrieves the base contents of the rune pouch, converting any combination runes into their respective basic elemental runes. |
| [getRunePouch](get-rune-pouch.md) | [Kraken API]<br>open fun [getRunePouch](get-rune-pouch.md)(): [RunePouch](index.md)<br>Retrieves the `RunePouch` instance corresponding to a rune pouch currently present in the player's inventory. |
| [getRunePouchContents](get-rune-pouch-contents.md) | [Kraken API]<br>open fun [getRunePouchContents](get-rune-pouch-contents.md)(): [Map](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/Map.html)&lt;[Rune](../-rune/index.md), [Integer](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/Integer.html)&gt;<br>Retrieves the current contents of the rune pouch if one is present in the player's inventory. |
| [hasRunePouch](has-rune-pouch.md) | [Kraken API]<br>open fun [hasRunePouch](has-rune-pouch.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html) |
| [valueOf](value-of.md) | [Kraken API]<br>open fun [valueOf](value-of.md)(name: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)): [RunePouch](index.md)<br>Returns the enum constant of this type with the specified name. The string must match exactly an identifier used to declare an enum constant in this type. (Extraneous whitespace characters are not permitted.) |
| [values](values.md) | [Kraken API]<br>open fun [values](values.md)(): [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[RunePouch](index.md)&gt;<br>Returns an array containing the constants of this enum type, in the order they're declared. This method may be used to iterate over the constants. |
