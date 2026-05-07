//[kraken-api](../../../index.md)/[com.kraken.api.service.magic.rune](../index.md)/[ElementalTome](index.md)/[forId](for-id.md)

# forId

[Kraken API]\
open fun [forId](for-id.md)(id: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [ElementalTome](index.md)

Retrieves the `ElementalTome` associated with the specified item ID. 

 This method searches through all available `ElementalTome` values and compares their item IDs to the provided `id`. If a match is found, the corresponding `ElementalTome` is returned. Otherwise, `null` is returned if no matching `ElementalTome` exists. 

#### Return

The `ElementalTome` associated with the specified `id`, or `null` if no match is found.

#### Parameters

Kraken API

| | |
|---|---|
| id | The item ID used to identify the corresponding `ElementalTome`. This value represents the unique identifier assigned to an `ElementalTome`. |
