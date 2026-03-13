//[kraken-api](../../../index.md)/[com.kraken.api.service.magic.rune](../index.md)/[Rune](index.md)/[byItemId](by-item-id.md)

# byItemId

[Kraken API]\
open fun [byItemId](by-item-id.md)(itemId: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [Rune](index.md)

Retrieves the `Rune` associated with the specified item ID. 

 This method searches through all available `Rune` values and compares their associated item IDs to the provided `itemId`. If a match is found, the corresponding `Rune` is returned. Otherwise, `null` is returned if no matching `Rune` exists. 

#### Return

The `Rune` associated with the specified `itemId`, or `null` if no match is found.

#### Parameters

Kraken API

| | |
|---|---|
| itemId | The item ID used to identify the corresponding `Rune`. |
