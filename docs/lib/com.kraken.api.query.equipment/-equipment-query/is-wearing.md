//[lib](../../../index.md)/[com.kraken.api.query.equipment](../index.md)/[EquipmentQuery](index.md)/[isWearing](is-wearing.md)

# isWearing

[Kraken API]\
open fun [isWearing](is-wearing.md)(id: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)

Checks if the player is wearing an item by id. Note: This strictly checks the equipment slots, ignoring the &quot;inInventory&quot; setting.

#### Return

True if the player is wearing the equipment and false otherwise

#### Parameters

Kraken API

| | |
|---|---|
| id | The item id for the equipment to check |

[Kraken API]\
open fun [isWearing](is-wearing.md)(name: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)

Checks if the player is wearing an item by name. Note: This strictly checks the equipment slots, ignoring the &quot;inInventory&quot; setting.

#### Return

True if the player is wearing the equipment and false otherwise

#### Parameters

Kraken API

| | |
|---|---|
| name | The name of the equipment to check |
