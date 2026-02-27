//[lib](../../../index.md)/[com.kraken.api.service.tile](../index.md)/[MovementFlag](index.md)

# MovementFlag

[Kraken API]\
enum [MovementFlag](index.md)

This is a copy of the net.runelite.client.plugins.devtools.MovementFlag class which has private access within the RuneLite client.

## Entries

| | |
|---|---|
| [BLOCK_MOVEMENT_NORTH_WEST](-b-l-o-c-k_-m-o-v-e-m-e-n-t_-n-o-r-t-h_-w-e-s-t/index.md) | [Kraken API]<br>[BLOCK_MOVEMENT_NORTH_WEST](-b-l-o-c-k_-m-o-v-e-m-e-n-t_-n-o-r-t-h_-w-e-s-t/index.md) |
| [BLOCK_MOVEMENT_NORTH](-b-l-o-c-k_-m-o-v-e-m-e-n-t_-n-o-r-t-h/index.md) | [Kraken API]<br>[BLOCK_MOVEMENT_NORTH](-b-l-o-c-k_-m-o-v-e-m-e-n-t_-n-o-r-t-h/index.md) |
| [BLOCK_MOVEMENT_NORTH_EAST](-b-l-o-c-k_-m-o-v-e-m-e-n-t_-n-o-r-t-h_-e-a-s-t/index.md) | [Kraken API]<br>[BLOCK_MOVEMENT_NORTH_EAST](-b-l-o-c-k_-m-o-v-e-m-e-n-t_-n-o-r-t-h_-e-a-s-t/index.md) |
| [BLOCK_MOVEMENT_EAST](-b-l-o-c-k_-m-o-v-e-m-e-n-t_-e-a-s-t/index.md) | [Kraken API]<br>[BLOCK_MOVEMENT_EAST](-b-l-o-c-k_-m-o-v-e-m-e-n-t_-e-a-s-t/index.md) |
| [BLOCK_MOVEMENT_SOUTH_EAST](-b-l-o-c-k_-m-o-v-e-m-e-n-t_-s-o-u-t-h_-e-a-s-t/index.md) | [Kraken API]<br>[BLOCK_MOVEMENT_SOUTH_EAST](-b-l-o-c-k_-m-o-v-e-m-e-n-t_-s-o-u-t-h_-e-a-s-t/index.md) |
| [BLOCK_MOVEMENT_SOUTH](-b-l-o-c-k_-m-o-v-e-m-e-n-t_-s-o-u-t-h/index.md) | [Kraken API]<br>[BLOCK_MOVEMENT_SOUTH](-b-l-o-c-k_-m-o-v-e-m-e-n-t_-s-o-u-t-h/index.md) |
| [BLOCK_MOVEMENT_SOUTH_WEST](-b-l-o-c-k_-m-o-v-e-m-e-n-t_-s-o-u-t-h_-w-e-s-t/index.md) | [Kraken API]<br>[BLOCK_MOVEMENT_SOUTH_WEST](-b-l-o-c-k_-m-o-v-e-m-e-n-t_-s-o-u-t-h_-w-e-s-t/index.md) |
| [BLOCK_MOVEMENT_WEST](-b-l-o-c-k_-m-o-v-e-m-e-n-t_-w-e-s-t/index.md) | [Kraken API]<br>[BLOCK_MOVEMENT_WEST](-b-l-o-c-k_-m-o-v-e-m-e-n-t_-w-e-s-t/index.md) |
| [BLOCK_MOVEMENT_OBJECT](-b-l-o-c-k_-m-o-v-e-m-e-n-t_-o-b-j-e-c-t/index.md) | [Kraken API]<br>[BLOCK_MOVEMENT_OBJECT](-b-l-o-c-k_-m-o-v-e-m-e-n-t_-o-b-j-e-c-t/index.md) |
| [BLOCK_MOVEMENT_FLOOR_DECORATION](-b-l-o-c-k_-m-o-v-e-m-e-n-t_-f-l-o-o-r_-d-e-c-o-r-a-t-i-o-n/index.md) | [Kraken API]<br>[BLOCK_MOVEMENT_FLOOR_DECORATION](-b-l-o-c-k_-m-o-v-e-m-e-n-t_-f-l-o-o-r_-d-e-c-o-r-a-t-i-o-n/index.md) |
| [BLOCK_MOVEMENT_FLOOR](-b-l-o-c-k_-m-o-v-e-m-e-n-t_-f-l-o-o-r/index.md) | [Kraken API]<br>[BLOCK_MOVEMENT_FLOOR](-b-l-o-c-k_-m-o-v-e-m-e-n-t_-f-l-o-o-r/index.md) |
| [BLOCK_MOVEMENT_FULL](-b-l-o-c-k_-m-o-v-e-m-e-n-t_-f-u-l-l/index.md) | [Kraken API]<br>[BLOCK_MOVEMENT_FULL](-b-l-o-c-k_-m-o-v-e-m-e-n-t_-f-u-l-l/index.md) |

## Functions

| Name | Summary |
|---|---|
| [getSetFlags](get-set-flags.md) | [Kraken API]<br>open fun [getSetFlags](get-set-flags.md)(collisionData: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [Set](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/Set.html)&lt;[MovementFlag](index.md)&gt; |
| [hasFlag](has-flag.md) | [Kraken API]<br>open fun [hasFlag](has-flag.md)(flags: [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[MovementFlag](index.md)&gt;, flagToCheck: [MovementFlag](index.md)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html) |
| [valueOf](value-of.md) | [Kraken API]<br>open fun [valueOf](value-of.md)(name: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)): [MovementFlag](index.md)<br>Returns the enum constant of this type with the specified name. The string must match exactly an identifier used to declare an enum constant in this type. (Extraneous whitespace characters are not permitted.) |
| [values](values.md) | [Kraken API]<br>open fun [values](values.md)(): [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[MovementFlag](index.md)&gt;<br>Returns an array containing the constants of this enum type, in the order they're declared. This method may be used to iterate over the constants. |
