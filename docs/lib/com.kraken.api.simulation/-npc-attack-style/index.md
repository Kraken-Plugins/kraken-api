//[lib](../../../index.md)/[com.kraken.api.simulation](../index.md)/[NpcAttackStyle](index.md)

# NpcAttackStyle

[Kraken API]\
enum [NpcAttackStyle](index.md)

Simulated NPC combat attack style used for prayer-threat modeling.

## Entries

| | |
|---|---|
| [MELEE](-m-e-l-e-e/index.md) | [Kraken API]<br>[MELEE](-m-e-l-e-e/index.md) |
| [RANGED](-r-a-n-g-e-d/index.md) | [Kraken API]<br>[RANGED](-r-a-n-g-e-d/index.md) |
| [MAGIC](-m-a-g-i-c/index.md) | [Kraken API]<br>[MAGIC](-m-a-g-i-c/index.md) |
| [UNKNOWN](-u-n-k-n-o-w-n/index.md) | [Kraken API]<br>[UNKNOWN](-u-n-k-n-o-w-n/index.md) |

## Functions

| Name | Summary |
|---|---|
| [toProtectionPrayer](to-protection-prayer.md) | [Kraken API]<br>open fun [toProtectionPrayer](to-protection-prayer.md)(): Prayer<br>Maps attack style to the corresponding protection prayer. |
| [valueOf](value-of.md) | [Kraken API]<br>open fun [valueOf](value-of.md)(name: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)): [NpcAttackStyle](index.md)<br>Returns the enum constant of this type with the specified name. The string must match exactly an identifier used to declare an enum constant in this type. (Extraneous whitespace characters are not permitted.) |
| [values](values.md) | [Kraken API]<br>open fun [values](values.md)(): [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[NpcAttackStyle](index.md)&gt;<br>Returns an array containing the constants of this enum type, in the order they're declared. This method may be used to iterate over the constants. |
