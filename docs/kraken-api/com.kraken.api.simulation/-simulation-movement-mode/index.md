//[kraken-api](../../../index.md)/[com.kraken.api.simulation](../index.md)/[SimulationMovementMode](index.md)

# SimulationMovementMode

[Kraken API]\
enum [SimulationMovementMode](index.md)

Strategy for generating movement destinations during tree expansion.

## Entries

| | |
|---|---|
| [RADIUS](-r-a-d-i-u-s/index.md) | [Kraken API]<br>[RADIUS](-r-a-d-i-u-s/index.md)<br>Expands movement actions to reachable tiles inside a configured radius from the player. |
| [REACHABLE](-r-e-a-c-h-a-b-l-e/index.md) | [Kraken API]<br>[REACHABLE](-r-e-a-c-h-a-b-l-e/index.md)<br>Expands movement actions to all reachable tiles within the remaining simulation horizon. |

## Functions

| Name | Summary |
|---|---|
| [valueOf](value-of.md) | [Kraken API]<br>open fun [valueOf](value-of.md)(name: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)): [SimulationMovementMode](index.md)<br>Returns the enum constant of this type with the specified name. The string must match exactly an identifier used to declare an enum constant in this type. (Extraneous whitespace characters are not permitted.) |
| [values](values.md) | [Kraken API]<br>open fun [values](values.md)(): [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[SimulationMovementMode](index.md)&gt;<br>Returns an array containing the constants of this enum type, in the order they're declared. This method may be used to iterate over the constants. |
