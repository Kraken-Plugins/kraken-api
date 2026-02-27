//[lib](../../../index.md)/[com.kraken.api.query.npc](../index.md)/[NpcEntity](index.md)

# NpcEntity

[Kraken API]\
open class [NpcEntity](index.md) : [AbstractEntity](../../com.kraken.api.core/-abstract-entity/index.md)&lt;[T](../../com.kraken.api.core/-abstract-entity/index.md)&gt;

## Constructors

| | |
|---|---|
| [NpcEntity](-npc-entity.md) | [Kraken API]<br>constructor(ctx: [Context](../../com.kraken.api/-context/index.md), raw: NPC) |

## Functions

| Name | Summary |
|---|---|
| [attack](attack.md) | [Kraken API]<br>open fun [attack](attack.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>Attacks an NPC. |
| [equals](../../com.kraken.api.core/-abstract-entity/equals.md) | [Kraken API]<br>open fun [equals](../../com.kraken.api.core/-abstract-entity/equals.md)(o: [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-any/index.html)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html) |
| [getDistanceFromPlayer](get-distance-from-player.md) | [Kraken API]<br>open fun [getDistanceFromPlayer](get-distance-from-player.md)(): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)<br>Calculates the distance between the NPC and the local player within the game world. |
| [getHeadIcon](get-head-icon.md) | [Kraken API]<br>open fun [getHeadIcon](get-head-icon.md)(): HeadIcon<br>Retrieves the head icon associated with the NPC, if it exists. |
| [getHealthPercentage](get-health-percentage.md) | [Kraken API]<br>open fun [getHealthPercentage](get-health-percentage.md)(): [Double](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-double/index.html)<br>Gets the health percentage of the NPC. |
| [getId](get-id.md) | [Kraken API]<br>open fun [getId](get-id.md)(): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)<br>The item ID for the wrapped game entity |
| [getName](get-name.md) | [Kraken API]<br>open fun [getName](get-name.md)(): [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)<br>The game entities name. |
| [hashCode](../../com.kraken.api.core/-abstract-entity/hash-code.md) | [Kraken API]<br>open fun [hashCode](../../com.kraken.api.core/-abstract-entity/hash-code.md)(): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) |
| [interact](interact.md) | [Kraken API]<br>open fun [interact](interact.md)(action: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>Interacts with the entity using the given action verb. |
| [isInArea](is-in-area.md) | [Kraken API]<br>open fun [isInArea](is-in-area.md)(area: [GameArea](../../com.kraken.api.service.tile/-game-area/index.md)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>Checks if the NPC's current location is within the game area. |
| [isNull](../../com.kraken.api.core/-abstract-entity/is-null.md) | [Kraken API]<br>open fun [isNull](../../com.kraken.api.core/-abstract-entity/is-null.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>True when the game entity is null and false otherwise. |
| [raw](../../com.kraken.api.core/-abstract-entity/raw.md) | [Kraken API]<br>open fun [raw](../../com.kraken.api.core/-abstract-entity/raw.md)(): [T](../../com.kraken.api.core/-abstract-entity/index.md)<br>Returns the wrapped (raw) RuneLite API object for this interactable game entity. |
| [useWidget](use-widget.md) | [Kraken API]<br>open fun [useWidget](use-widget.md)(widget: Widget): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>Uses a specified widget on the NPC (i.e. |
