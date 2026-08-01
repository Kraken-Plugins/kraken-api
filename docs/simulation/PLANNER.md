# The planner

> Part of the [Fortis Colosseum simulation docs](../SIMULATION.md). Source:
> `src/test/java/plugins/colosseum/simulation/plan/`. Builds on
> [the tick engine](ENGINE.md).

[`Planner.plan(state, options)`](../../src/test/java/plugins/colosseum/simulation/plan/Planner.java)
answers "what should I do this tick?" and must answer it *fast* — it runs inside the
game-tick handler under a hard wall-clock budget (15 ms by default) and returns its
best-so-far answer when the deadline hits.

## The search problem, and how it's cut down

A naive search is hopeless. Even with only ~10 meaningful choices per tick, looking 12
ticks ahead means 10¹² futures. The planner makes this tractable with one big idea:

**Search only over *macro* decisions; handle the *forced* decisions with fixed rules.**

- A **macro plan** is a pair: *where to stand* (a movement destination) × *what to kill*
  (an attack target). This is the part of colosseum play with genuine trade-offs — hide
  behind a pillar vs. stay in range, kill the warband vs. the manticore.
- Everything a competent player does reflexively — praying against the incoming hit,
  eating when low, swapping to the right gear for the target, re-targeting when something
  dies — is not searched at all. Inside every simulated future, deterministic **policies**
  make those calls every tick (below).

So one candidate plan = one (destination, target) pair, evaluated by a **rollout**:
copy the current state, apply the plan's first command, then advance the tick engine
12 ticks (the *horizon*) with the policies steering, and score what the world looks like
at the end. With ~24 candidate destinations × up to 3 targets, a planning pass is
typically a few dozen first-stage rollouts plus second-stage refinements — hundreds of
rollouts, thousands of simulated ticks, comfortably inside the budget.

Only the *first tick* of the winning plan is ever executed. Next game tick, the planner
starts over from a fresh capture.

## Step 1: the danger map

[`DangerMap`](../../src/test/java/plugins/colosseum/simulation/plan/DangerMap.java)
analyses every walkable tile within a radius (default 12) of the player: **which NPCs
could hit that tile from where they stand right now**, and how much expected damage per
tick that exposure adds up to (each NPC contributing `maxHit × 0.30 / attackSpeed`).

Tiles with a line-of-sight count of zero are *cover* — stand there and, until the NPCs
reposition, nothing can touch you. The danger map seeds the candidate destinations and is
also what the scene overlay renders as the red heat map.

## Step 2: candidate destinations

Up to 24 destinations (deduplicated), gathered from three sources:

1. **The immediate neighbourhood** — the player's current tile plus its 8 legal
   neighbours. Cheap dodges and one-tile adjustments.
2. **The nearest cover** — up to 8 zero-exposure tiles, preferring the closest by actual
   walking distance (a BFS field from the player, so tiles behind walls cost their real
   path length).
3. **Attack positions** — up to 6 tiles that keep line of sight *and* attack range on the
   primary target with the best gear for it, preferring low danger and short walks
   (cost = expected damage per tick × 4 + walk distance).

## Step 3: candidate targets

Up to 3 targets: the NPC the player is already fighting (never dropped from
consideration), then the nearest NPC of each type in a fixed, research-backed kill order:

1. Fremennik berserker, seer, archer — fragile, each weak to a style, constant chip
   damage while alive
2. Serpent shaman
3. Jaguar warrior
4. Manticore
5. Shockwave Colossus
6. Javelin Colossus
7. Minotaurs last (tanky, and mostly a threat via healing others)

When a target dies mid-rollout, the retargeting policy picks the next alive NPC in this
same order.

## The in-rollout policies

Every simulated tick after the first, `policyCommand` composes the command a strong
player would issue:

### Prayer: oracle flicking

The policy predicts **which NPCs will launch an attack on the very next tick** and prays
against the style with the largest predicted expected damage. Predictions cover:

- normal attackers whose cooldown is about to expire — including NPCs that currently lack
  line of sight but whose *predicted next step* brings them into it, so a melee mob
  walking around a pillar corner is prayed against on the tick it arrives;
- manticore orb sequences (mid-triple, each orb's style comes from the pattern; a charged
  manticore about to fire is predicted at its first orb);
- the warband metronome tick;
- already-launched hits that land next tick and are prayer-checked on landing (the
  minotaur's delayed melee).

Because the engine checks prayers at launch (see
[the engine page](ENGINE.md#anatomy-of-one-simulated-tick)), praying on the launch tick
is exactly sufficient — this policy is a perfect flicker *within the simulation*. In
reality it is executed one tick at a time and re-derived from a fresh capture every tick,
so a wrong prediction costs one hit, not a cascade.

### Consumables: threshold eating

Eat primary food at ≤ 48 HP; also combo-eat a karambwan at ≤ 34 HP; sip a brew at
≤ 55 HP when out of food; sip a restore at ≤ 15 prayer points (all thresholds
configurable). The engine's eat/potion timers are respected. Note that the policy doesn't
need to be clever about *whether eating is worth the lost attack* — the scorer prices
survival and damage output against each other, so a plan that skips a needed eat simply
scores worse and loses.

### Gear: best set for the target

Swap to the gear set with the best expected damage per attack (weighted by attack speed)
against the current target — e.g. the magic set one-shotting a berserker, ranged against
the seer.

### Movement/attack weaving

The movement half of the policy reproduces standard attack-move play:

- **Kite shots while travelling:** if the weapon is ready and the target is hittable from
  the current tile, click the attack (which pauses the path for a tick), then resume
  walking next tick.
- **Attack on arrival**, or when the weapon comes off cooldown in range.
- **Weave while on cooldown:** when engaged but off the planned tile with ≥ 2 ticks of
  weapon cooldown left, spend the dead ticks walking to the planned position.

## Two-stage search

A single destination can't express "duck behind the pillar *until the triple passes*,
then swing back out". So the best first-stage plans (up to `stage2TopPlans`, capped at 8)
have their rollout end-states saved as launch points, and the planner tries every
candidate destination again *from there*, rolling out a further `stage2HorizonTicks`
(default 10). If a two-stage combination scores best, the decision returned is still just
the first stage's first command — the second stage only proves the first move leads
somewhere good.

Both stages run under the same deadline; the planner simply stops expanding when time is
up.

## Scoring

[`Scorer`](../../src/test/java/plugins/colosseum/simulation/plan/Scorer.java) reduces a
rollout end-state to one number. Default weights:

| Term | Weight | Direction |
|---|---|---|
| Player died during the rollout | 1,000,000 (minus ticks survived, so dying later still beats dying sooner) | penalty |
| Worst-case HP floor below zero ("could have died") | 4,000 per HP below zero | penalty |
| Expected damage taken | 14 per HP | penalty |
| Player HP at the horizon | 6 per HP | reward |
| NPC killed | 900 each | reward |
| Expected damage dealt | 6 per HP | reward |
| Supply consumed | 25 each | penalty |
| Prayer points below cap at horizon | 2.5 each | penalty |
| NPC with line of sight to the player at horizon | 30 each | penalty |
| Run energy retained | 0.4 per 1% | reward |

The structure matters more than the numbers: the death and lethal-risk terms are *orders
of magnitude* above everything else, so no amount of kill progress can buy a risky plan.
Within survivable futures, kills dominate (900 ≫ anything per-HP), ending exposed is
mildly bad, and burning supplies without need is discouraged. All weights are exposed in
the Auto Colosseum config for live tuning.

## Options

[`PlannerOptions`](../../src/test/java/plugins/colosseum/simulation/plan/PlannerOptions.java)
defaults (all overridable per call, and surfaced in the plugin config):

| Option | Default | Meaning |
|---|---|---|
| `budgetNanos` | 15 ms | Hard wall-clock deadline |
| `horizonTicks` | 12 | First-stage rollout depth |
| `stage2HorizonTicks` | 10 | Second-stage rollout depth |
| `stage2TopPlans` | 4 | First-stage plans kept for refinement (hard cap 8) |
| `dangerRadius` | 12 | Danger-map radius around the player |
| `maxDestinations` / `maxSafeTiles` / `maxAttackTiles` | 24 / 8 / 6 | Candidate caps |
| `maxTargets` | 3 | Targets considered per destination |
| `run` | true | Movement clicks use run |
| `eatFoodAtHp` / `eatComboAtHp` / `sipBrewAtHp` / `sipRestoreAtPrayer` | 48 / 34 / 55 / 15 | Policy thresholds |
| `scorer` | `Scorer.defaults()` | Scoring weights |

## The `Decision`

[`Decision`](../../src/test/java/plugins/colosseum/simulation/plan/Decision.java) is the
planner's output — both the *orders* and the *evidence*:

- **Orders:** the packed first-tick command, exposed through convenience accessors the
  executor consumes — `getMoveDestination()` (a world point), `getPrayerToActivate()`,
  `isEatFood()`/`isSipBrew()`/…, `getGearSetIndex()`, `isUseSpec()`, and `hasAttack()`
  with the target's live RuneLite NPC index. `hasAttack()` is only true when a *new*
  attack click is needed — if the player is already fighting the plan's target, combat
  continues on its own and no click is issued.
- **Evidence (for the overlays):** every candidate tile with its best score, the
  predicted player path along the winning plan, predicted HP / worst-case floor / damage
  taken / kills at the horizon, rollout count and elapsed time, and a one-line
  human-readable reasoning string, e.g.
  `Move to (27,14) [cover]; pray Missiles; attack Fremennik warband seer with Ranged | score 1520 (21 tiles x 3 targets)`.

## Determinism and performance

A planning pass allocates almost nothing: states are pooled and copied with
`System.arraycopy`, commands are packed `long`s, and rollouts share one `Scratch` whose
path fields are cached across the whole pass. Rollouts are deterministic, so identical
inputs produce identical decisions — which is what makes `PlannerTest` and
`PerfProbeTest` (numbers land in `build/colo-perf.txt`) reliable.
