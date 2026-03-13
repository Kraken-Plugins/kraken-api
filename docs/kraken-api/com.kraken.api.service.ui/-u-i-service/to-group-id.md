//[kraken-api](../../../index.md)/[com.kraken.api.service.ui](../index.md)/[UIService](index.md)/[toGroupId](to-group-id.md)

# toGroupId

[Kraken API]\
open fun [toGroupId](to-group-id.md)(packedId: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)

Converts the given packed widget integer ID to a group ID by performing a bitwise right shift. 

This method shifts the bits of the input ID 16 positions to the right, effectively dividing by 2^16 and discarding the lower 16 bits.

#### Return

the resulting group ID after the bitwise shift.

#### Parameters

Kraken API

| | |
|---|---|
| packedId | the integer ID to be converted. |
