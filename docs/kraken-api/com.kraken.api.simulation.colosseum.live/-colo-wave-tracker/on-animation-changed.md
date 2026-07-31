//[kraken-api](../../../index.md)/[com.kraken.api.simulation.colosseum.live](../index.md)/[ColoWaveTracker](index.md)/[onAnimationChanged](on-animation-changed.md)

# onAnimationChanged

[Kraken API]\
open fun [onAnimationChanged](on-animation-changed.md)(actor: Actor, localPlayer: Player)

Records attack animations for cooldown estimation. Colosseum NPCs only play non-pose animations when attacking (or dying), so any animation change while tracked is treated as an attack launch; estimates self-correct next cycle.

#### Parameters

Kraken API

| | |
|---|---|
| actor | actor whose animation changed. |
| localPlayer | the local player, for player cooldown tracking. |
