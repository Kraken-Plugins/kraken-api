//[kraken-api](../../../index.md)/[com.kraken.api.service.magic.rune](../index.md)/[RunePouch](index.md)/[byItemId](by-item-id.md)

# byItemId

[Kraken API]\
open fun [byItemId](by-item-id.md)(itemId: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [RunePouch](index.md)

Retrieves the @code RunePouch instance corresponding to the specified item ID. This method searches through all defined @code RunePouch values and returns the matching instance if the provided item ID corresponds to a known Rune Pouch. 

Returns @code null if no @code RunePouch with the given item ID is found.

#### Return

The @code RunePouch instance matching the given item ID, or @code null if no match is found.

#### Parameters

Kraken API

| | |
|---|---|
| itemId | The item ID of the Rune Pouch to locate. This should be an integer representing a valid item ID of a Rune Pouch. For example, these may include IDs for standard, divine, or decorative Rune Pouches. |
