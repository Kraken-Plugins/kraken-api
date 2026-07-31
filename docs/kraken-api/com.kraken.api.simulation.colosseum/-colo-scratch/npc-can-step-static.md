//[kraken-api](../../../index.md)/[com.kraken.api.simulation.colosseum](../index.md)/[ColoScratch](index.md)/[npcCanStepStatic](npc-can-step-static.md)

# npcCanStepStatic

[Kraken API]\
open fun [npcCanStepStatic](npc-can-step-static.md)(grid: [ColoGrid](../-colo-grid/index.md), x: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), y: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), dx: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), dy: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), size: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)

NPC anchor single-step legality against static collision (dynamic entity blocking is checked separately at step-execution time). Size-1 NPCs may not cut corners; larger NPCs may step diagonally whenever the destination footprint fits, matching the validated community simulator.

#### Return

true when the step is statically legal.

#### Parameters

Kraken API

| | |
|---|---|
| grid | collision grid. |
| x | anchor x. |
| y | anchor y. |
| dx | step x. |
| dy | step y. |
| size | footprint size. |
