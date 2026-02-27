//[lib](../../../index.md)/[com.kraken.api.service.ui](../index.md)/[UIService](index.md)/[toChildId](to-child-id.md)

# toChildId

[Kraken API]\
open fun [toChildId](to-child-id.md)(packedId: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)

Converts a given integer ID to a child ID by masking higher-order bits. 

 This method extracts the lower 16 bits of the provided integer.

#### Return

the child ID represented as the lower 16 bits of the input ID.

#### Parameters

Kraken API

| | |
|---|---|
| packedId | the input integer ID to be converted. |
