//[kraken-api](../../../index.md)/[com.kraken.api.service.ui](../index.md)/[UIService](index.md)/[pack](pack.md)

# pack

[Kraken API]\
open fun [pack](pack.md)(groupId: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), childId: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)

Combines a group ID and a child ID into a single packed integer. 

The group ID is shifted 16 bits to the left and combined with the child ID using a bitwise OR operation.

#### Return

the packed integer containing the group ID and child ID

#### Parameters

Kraken API

| | |
|---|---|
| groupId | the identifier for the group |
| childId | the identifier for the child |
