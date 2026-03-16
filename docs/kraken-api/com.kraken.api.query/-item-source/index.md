//[kraken-api](../../../index.md)/[com.kraken.api.query](../index.md)/[ItemSource](index.md)

# ItemSource

[Kraken API]\
enum [ItemSource](index.md)

Represents the source from which an item can be obtained or accessed.

This enumeration is typically used to define or restrict the context in which an item exists or is available for interaction.

- INVENTORY_ONLY - Indicates that the item is available exclusively within the inventory.
- INTERFACE_ONLY - Indicates that the item is accessible only through the user interface i.e. equipment interface or bank deposit boxes equipment interface.
- BOTH - Indicates that the item is available in both the inventory and the user interface.

## Entries

| | |
|---|---|
| [INVENTORY_ONLY](-i-n-v-e-n-t-o-r-y_-o-n-l-y/index.md) | [Kraken API]<br>[INVENTORY_ONLY](-i-n-v-e-n-t-o-r-y_-o-n-l-y/index.md) |
| [INTERFACE_ONLY](-i-n-t-e-r-f-a-c-e_-o-n-l-y/index.md) | [Kraken API]<br>[INTERFACE_ONLY](-i-n-t-e-r-f-a-c-e_-o-n-l-y/index.md) |
| [BOTH](-b-o-t-h/index.md) | [Kraken API]<br>[BOTH](-b-o-t-h/index.md) |

## Functions

| Name | Summary |
|---|---|
| [valueOf](value-of.md) | [Kraken API]<br>open fun [valueOf](value-of.md)(name: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)): [ItemSource](index.md)<br>Returns the enum constant of this type with the specified name. The string must match exactly an identifier used to declare an enum constant in this type. (Extraneous whitespace characters are not permitted.) |
| [values](values.md) | [Kraken API]<br>open fun [values](values.md)(): [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[ItemSource](index.md)&gt;<br>Returns an array containing the constants of this enum type, in the order they're declared. This method may be used to iterate over the constants. |
