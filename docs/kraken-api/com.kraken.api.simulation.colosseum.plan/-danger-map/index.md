//[kraken-api](../../../index.md)/[com.kraken.api.simulation.colosseum.plan](../index.md)/[DangerMap](index.md)

# DangerMap

[Kraken API]\
class [DangerMap](index.md)

Per-tile threat analysis around the player: for every walkable tile in a radius, which NPCs would have line of sight to it from where they stand now, and how much expected damage per tick that exposure represents. 

Used both to seed the planner's candidate destinations (nearest zero-exposure tiles, lowest-exposure attack positions) and by the debug overlay as a heat map.

## Constructors

| | |
|---|---|
| [DangerMap](-danger-map.md) | [Kraken API]<br>constructor() |

## Functions

| Name | Summary |
|---|---|
| [center](center.md) | [Kraken API]<br>open fun [center](center.md)(): [Short](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-short/index.html) |
| [compute](compute.md) | [Kraken API]<br>open fun [compute](compute.md)(state: [ColoState](../../com.kraken.api.simulation.colosseum/-colo-state/index.md), radius: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html))<br>Recomputes the map around the player. |
| [covers](covers.md) | [Kraken API]<br>open fun [covers](covers.md)(tile: [Short](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-short/index.html)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html) |
| [expectedDamagePerTickX100](expected-damage-per-tick-x100.md) | [Kraken API]<br>open fun [expectedDamagePerTickX100](expected-damage-per-tick-x100.md)(tile: [Short](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-short/index.html)): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) |
| [losCount](los-count.md) | [Kraken API]<br>open fun [losCount](los-count.md)(tile: [Short](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-short/index.html)): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) |
| [radius](radius.md) | [Kraken API]<br>open fun [radius](radius.md)(): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) |
