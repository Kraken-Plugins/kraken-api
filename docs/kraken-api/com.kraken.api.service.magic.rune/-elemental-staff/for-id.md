//[kraken-api](../../../index.md)/[com.kraken.api.service.magic.rune](../index.md)/[ElementalStaff](index.md)/[forId](for-id.md)

# forId

[Kraken API]\
open fun [forId](for-id.md)(id: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [ElementalStaff](index.md)

Retrieves an `ElementalStaff` corresponding to the provided item ID. 

This method iterates through all `ElementalStaff` enum values and matches the given ID against the `itemId` field of each staff. If a match is found, the corresponding `ElementalStaff` is returned.

#### Return

The `ElementalStaff` corresponding to the provided item ID,

#### Parameters

Kraken API

| | |
|---|---|
| id | The id of the `ElementalStaff` to match. |
