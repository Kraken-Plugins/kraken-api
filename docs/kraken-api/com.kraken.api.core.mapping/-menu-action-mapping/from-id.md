//[kraken-api](../../../index.md)/[com.kraken.api.core.mapping](../index.md)/[MenuActionMapping](index.md)/[fromId](from-id.md)

# fromId

[Kraken API]\
open fun [fromId](from-id.md)(id: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [MenuActionMapping](index.md)

Finds the corresponding mapping based on the internal engine Action ID. Useful for ASM bytecode analysis.

#### Return

The MenuActionMapping, or null if the ID is not mapped.

#### Parameters

Kraken API

| | |
|---|---|
| id | The integer opcode found in the doAction method. |
