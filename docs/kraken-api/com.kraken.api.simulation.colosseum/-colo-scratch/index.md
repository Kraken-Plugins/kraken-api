//[kraken-api](../../../index.md)/[com.kraken.api.simulation.colosseum](../index.md)/[ColoScratch](index.md)

# ColoScratch

[Kraken API]\
class [ColoScratch](index.md)

Reusable working memory for the tick engine and planner: BFS queues, distance fields and per-tick transient flags. One instance per planning thread; nothing here survives as state, so the search loop stays allocation-free after warmup. 

Two kinds of distance fields are cached:

- **Player path fields** - BFS distance from a movement destination over player-legal steps. Keyed by destination; a small LRU keeps the fields for candidate destinations shared across every rollout of one planning pass.
- **NPC approach fields** - BFS distance to the nearest legal melee-attack anchor against the player, per footprint size. Used by route-finding NPCs (Fremennik warband, Red Flag minotaurs). Keyed by player position and size.

## Constructors

| | |
|---|---|
| [ColoScratch](-colo-scratch.md) | [Kraken API]<br>constructor()<br>Creates scratch memory. |

## Properties

| Name | Summary |
|---|---|
| [attackListener](attack-listener.md) | [Kraken API]<br>open var [attackListener](attack-listener.md): [ColoTick.AttackListener](../-colo-tick/-attack-listener/index.md)<br>Optional hook invoked whenever an NPC launches an attack during [advance](../-colo-tick/advance.md); used by tests and debug tooling (null and zero-cost otherwise). |
| [DIR_X](-d-i-r_-x.md) | [Kraken API]<br>val [DIR_X](-d-i-r_-x.md): [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)&gt;<br>Neighbour direction x-offsets shared by BFS and step iteration. |
| [DIR_Y](-d-i-r_-y.md) | [Kraken API]<br>val [DIR_Y](-d-i-r_-y.md): [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)&gt;<br>Neighbour direction y-offsets shared by BFS and step iteration. |
| [UNREACHABLE](-u-n-r-e-a-c-h-a-b-l-e.md) | [Kraken API]<br>val [UNREACHABLE](-u-n-r-e-a-c-h-a-b-l-e.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) = 255<br>Field value for unreachable tiles. |

## Functions

| Name | Summary |
|---|---|
| [approachField](approach-field.md) | [Kraken API]<br>open fun [approachField](approach-field.md)(grid: [ColoGrid](../-colo-grid/index.md), playerPos: [Short](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-short/index.html), size: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[Byte](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-byte/index.html)&gt;<br>Returns the BFS approach field for a route-finding NPC of the given size hunting the player: distance from any anchor tile to the nearest legal melee-attack anchor. |
| [invalidate](invalidate.md) | [Kraken API]<br>open fun [invalidate](invalidate.md)()<br>Invalidates cached fields; call when the grid changes (new capture). |
| [npcCanStepStatic](npc-can-step-static.md) | [Kraken API]<br>open fun [npcCanStepStatic](npc-can-step-static.md)(grid: [ColoGrid](../-colo-grid/index.md), x: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), y: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), dx: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), dy: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), size: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>NPC anchor single-step legality against static collision (dynamic entity blocking is checked separately at step-execution time). |
| [playerCanStep](player-can-step.md) | [Kraken API]<br>open fun [playerCanStep](player-can-step.md)(grid: [ColoGrid](../-colo-grid/index.md), x: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), y: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), dx: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), dy: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>Player single-step legality: destination tile must be free; diagonal steps additionally require both flanking cardinal tiles to be free (standard player movement rule). |
| [playerField](player-field.md) | [Kraken API]<br>open fun [playerField](player-field.md)(grid: [ColoGrid](../-colo-grid/index.md), dest: [Short](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-short/index.html)): [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[Byte](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-byte/index.html)&gt;<br>Returns the BFS distance field toward a player movement destination. |
