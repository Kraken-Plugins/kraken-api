//[kraken-api](../../../index.md)/[com.kraken.api.simulation.colosseum.plan](../index.md)/[ColoDecision](index.md)/[hasAttack](has-attack.md)

# hasAttack

[Kraken API]\
open fun [hasAttack](has-attack.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)

#### Return

true when this decision issues a NEW attack click this tick. When the player is already attacking the plan's target, combat continues on its own and no click is needed - getAttackNpcRuneliteIndex() still names the plan's target for display purposes.
