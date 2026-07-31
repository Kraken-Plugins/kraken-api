//[kraken-api](../../index.md)/[com.kraken.api.simulation.colosseum](index.md)

# Package-level declarations

## Types

| Name | Summary |
|---|---|
| [ColoConstants](-colo-constants/index.md) | [Kraken API]<br>class [ColoConstants](-colo-constants/index.md)<br>Timing and behaviour constants for the Fortis Colosseum simulation. |
| [ColoCoords](-colo-coords/index.md) | [Kraken API]<br>class [ColoCoords](-colo-coords/index.md)<br>Bit-packing helpers for arena-local tile coordinates. |
| [ColoFrame](-colo-frame/index.md) | [Kraken API]<br>class [ColoFrame](-colo-frame/index.md)<br>Immutable per-decision context shared by every [ColoState](-colo-state/index.md) branched from one snapshot: the collision grid, per-slot NPC identity data and the player loadout. |
| [ColoGrid](-colo-grid/index.md) | [Kraken API]<br>class [ColoGrid](-colo-grid/index.md)<br>Immutable arena collision grid backed by one 64-bit row mask per tile row. |
| [ColoLos](-colo-los/index.md) | [Kraken API]<br>class [ColoLos](-colo-los/index.md)<br>Line-of-sight and melee reachability for the colosseum grid. |
| [ColoNpcType](-colo-npc-type/index.md) | [Kraken API]<br>enum [ColoNpcType](-colo-npc-type/index.md)<br>Combat and behaviour definitions for every simulated Fortis Colosseum wave NPC (Sol Heredit is intentionally excluded). |
| [ColoScratch](-colo-scratch/index.md) | [Kraken API]<br>class [ColoScratch](-colo-scratch/index.md)<br>Reusable working memory for the tick engine and planner: BFS queues, distance fields and per-tick transient flags. |
| [ColoState](-colo-state/index.md) | [Kraken API]<br>class [ColoState](-colo-state/index.md)<br>Mutable, poolable simulation state: everything about one possible future of the arena. |
| [ColoTick](-colo-tick/index.md) | [Kraken API]<br>class [ColoTick](-colo-tick/index.md)<br>The colosseum tick engine: advances a [ColoState](-colo-state/index.md) by exactly one game tick given a [PlayerCommand](-player-command/index.md). |
| [LoadoutConfig](-loadout-config/index.md) | [Kraken API]<br>class [LoadoutConfig](-loadout-config/index.md)<br>Player capability configuration for the colosseum simulation: gear sets with expected damage output per NPC type, supply heal values, and the passive stats (prayer bonus, agility, weight) that drive drain/regen formulas. |
| [PlayerCommand](-player-command/index.md) | [Kraken API]<br>class [PlayerCommand](-player-command/index.md)<br>One tick's worth of player intent, bit-packed into a single `long` so search nodes can branch over thousands of commands with zero allocation. |
