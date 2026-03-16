//[kraken-api](../../../index.md)/[com.kraken.api.service.bank](../index.md)/[DepositBoxService](index.md)/[depositWorn](deposit-worn.md)

# depositWorn

[Kraken API]\
open fun [depositWorn](deposit-worn.md)(slot: EquipmentInventorySlot): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)

Deposits a specific worn equipment item from the player's inventory into the bank deposit box. 

This method interacts with the deposit box interface to deposit a single worn equipment item specified by its EquipmentInventorySlot. It uses the @ctxProvider to locate the corresponding widget and perform the interaction.

- The method interacts with the specific slot's widget, which corresponds to the player's equipped item to be deposited.
- The deposit box interface must be open and accessible for this operation to succeed. If the interface is not open, the interaction will fail.

#### Return

`true` if the deposit operation on the specified worn item is successful; `false` if the interaction fails or the required conditions are not met (e.g., the deposit box is not open, or the widget is unavailable).

#### Parameters

Kraken API

| | |
|---|---|
| slot | the EquipmentInventorySlot representing the specific item to be deposited. This parameter identifies the equipment slot (e.g., Helmet, Chest, Gloves) for the item to be interacted with. |

[Kraken API]\
open fun [depositWorn](deposit-worn.md)(id: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)

Deposits a specific worn equipment item into the bank deposit box based on the item ID. 

This method retrieves the worn equipment item associated with the given `id` from the player's equipment. If the item is found and associated with a valid EquipmentInventorySlot, it attempts to deposit the item into the bank deposit box.

- The deposit operation is performed on the specific slot that corresponds to the equipment item with the provided `id`.
- The operation assumes the deposit box interface is already open; otherwise, the interaction may fail.
- If the item or its corresponding slot is not found, the method returns `false`.

#### Return

`true` if the interaction to deposit the worn item succeeds; `false` otherwise, such as when the item or slot is not found, or the deposit operation fails.

#### Parameters

Kraken API

| | |
|---|---|
| id | the unique identifier of the equipment item to be deposited. This represents the item to locate in the player's worn equipment slots. |

[Kraken API]\
open fun [depositWorn](deposit-worn.md)(name: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)

Deposits an item currently equipped on the player into a storage system. 

 This method attempts to locate an equipped item by its name and then identifies its respective equipment slot. If both are valid, the item is deposited. 

#### Return

true if the item was successfully deposited; false if the item could not be located or deposited.

#### Parameters

Kraken API

| | |
|---|---|
| name | The name of the equipped item to be deposited. <br>This parameter specifies the exact name of the item as it appears in the game's interface or inventory system. |
