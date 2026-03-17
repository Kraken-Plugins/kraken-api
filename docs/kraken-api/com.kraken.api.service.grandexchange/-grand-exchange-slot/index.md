//[kraken-api](../../../index.md)/[com.kraken.api.service.grandexchange](../index.md)/[GrandExchangeSlot](index.md)

# GrandExchangeSlot

[Kraken API]\
enum [GrandExchangeSlot](index.md)

## Entries

| | |
|---|---|
| [SLOT_1](-s-l-o-t_1/index.md) | [Kraken API]<br>[SLOT_1](-s-l-o-t_1/index.md) |
| [SLOT_2](-s-l-o-t_2/index.md) | [Kraken API]<br>[SLOT_2](-s-l-o-t_2/index.md) |
| [SLOT_3](-s-l-o-t_3/index.md) | [Kraken API]<br>[SLOT_3](-s-l-o-t_3/index.md) |
| [SLOT_4](-s-l-o-t_4/index.md) | [Kraken API]<br>[SLOT_4](-s-l-o-t_4/index.md) |
| [SLOT_5](-s-l-o-t_5/index.md) | [Kraken API]<br>[SLOT_5](-s-l-o-t_5/index.md) |
| [SLOT_6](-s-l-o-t_6/index.md) | [Kraken API]<br>[SLOT_6](-s-l-o-t_6/index.md) |
| [SLOT_7](-s-l-o-t_7/index.md) | [Kraken API]<br>[SLOT_7](-s-l-o-t_7/index.md) |
| [SLOT_8](-s-l-o-t_8/index.md) | [Kraken API]<br>[SLOT_8](-s-l-o-t_8/index.md) |

## Functions

| Name | Summary |
|---|---|
| [getBySlot](get-by-slot.md) | [Kraken API]<br>open fun [getBySlot](get-by-slot.md)(slot: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [GrandExchangeSlot](index.md)<br>Given an integer for the slot number, returns the grand exchange slot object for that slot. |
| [getItemId](get-item-id.md) | [Kraken API]<br>open fun [getItemId](get-item-id.md)(): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)<br>Returns the item id for the item in the grand exchange offer slot. |
| [isFulfilled](is-fulfilled.md) | [Kraken API]<br>open fun [isFulfilled](is-fulfilled.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>Returns true when a grand exchange slot has been fulfilled (the item has been bought or sold). |
| [valueOf](value-of.md) | [Kraken API]<br>open fun [valueOf](value-of.md)(name: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)): [GrandExchangeSlot](index.md)<br>Returns the enum constant of this type with the specified name. The string must match exactly an identifier used to declare an enum constant in this type. (Extraneous whitespace characters are not permitted.) |
| [values](values.md) | [Kraken API]<br>open fun [values](values.md)(): [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[GrandExchangeSlot](index.md)&gt;<br>Returns an array containing the constants of this enum type, in the order they're declared. This method may be used to iterate over the constants. |
