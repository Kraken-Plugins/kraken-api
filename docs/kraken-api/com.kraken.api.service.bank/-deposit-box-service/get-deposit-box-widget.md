//[kraken-api](../../../index.md)/[com.kraken.api.service.bank](../index.md)/[DepositBoxService](index.md)/[getDepositBoxWidget](get-deposit-box-widget.md)

# getDepositBoxWidget

[Kraken API]\
open fun [getDepositBoxWidget](get-deposit-box-widget.md)(slot: EquipmentInventorySlot): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)

Retrieves the interface widget ID corresponding to a deposit box slot for the specified EquipmentInventorySlot. 

This method maps equipment slots to their respective widget IDs used in the deposit box interface. If the specified slot does not match a known equipment slot, an [IllegalArgumentException](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/IllegalArgumentException.html) is thrown.

#### Return

The interface widget ID as an integer that corresponds to the provided EquipmentInventorySlot.

#### Parameters

Kraken API

| | |
|---|---|
| slot | The EquipmentInventorySlot representing the equipment slot for which the deposit box widget ID is to be retrieved. <br>- @code HEAD maps to @code SLOT0 - @code CAPE maps to @code SLOT1 - @code AMULET maps to @code SLOT2 - @code WEAPON maps to @code SLOT3 - @code BODY maps to @code SLOT4 - @code SHIELD maps to @code SLOT5 - @code LEGS maps to @code SLOT7 - @code GLOVES maps to @code SLOT9 - @code BOOTS maps to @code SLOT10 - @code RING maps to @code SLOT12 - @code AMMO maps to @code SLOT13 |

#### Throws

| | |
|---|---|
| [IllegalArgumentException](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/IllegalArgumentException.html) | If an unknown or unsupported EquipmentInventorySlot is provided. |
