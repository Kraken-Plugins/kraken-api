# The tick engine

> Part of the [Fortis Colosseum simulation docs](../SIMULATION.md). Source:
> `src/test/java/plugins/colosseum/simulation/`.

The heart of the system is one static method:

```java
Tick.advance(State state, long command, Scratch scratch);
```

It takes a snapshot of the fight (`State`), one tick's worth of player intent
(`PlayerCommand`, packed into a `long`), and advances the state by exactly one game tick —
NPCs move and attack, hits land, timers count down. It is deterministic: the same state
and command always produce the same result.

Everything else in the system is built on calling this method many times. The planner
simulates a 12-tick future by calling `advance` twelve times on a copy of the current
state, and it does that for dozens of candidate plans, every game tick.

## Why the code looks the way it does

The planner needs on the order of *thousands of simulated ticks per real game tick*,
inside a 15 ms budget, without stuttering the client. That single requirement explains
most of the engine's design choices:

- **No objects, just numbers.** The whole wave state is primitive fields and a few small
  primitive arrays. Copying a state (which the search does constantly) is a handful of
  `System.arraycopy` calls. After warmup the search loop allocates nothing, so the
  garbage collector never gets involved.
- **Bit-packing.** Several small numbers are stored inside one machine word. A tile
  position is two 0–63 numbers, and 6 bits hold 0–63, so a position packs into the low
  12 bits of a `short`: `packed = x | (y << 6)` ([`Coords`](../../src/test/java/plugins/colosseum/simulation/Coords.java)).
  A full player command packs into one `long`; an in-flight attack packs into one `long`.
- **Precomputation.** Anything that can be computed once and reused (collision masks,
  path distance fields) is cached in [`Scratch`](../../src/test/java/plugins/colosseum/simulation/Scratch.java)
  or precomputed in [`Grid`](../../src/test/java/plugins/colosseum/simulation/Grid.java).

Measured throughput is on the order of a microsecond per simulated tick with 9 NPCs
(`PerfProbeTest` writes current numbers to `build/colo-perf.txt`).

## How the fight is stored: `Frame` + `State`

The snapshot is split in two, by whether the data can change during a simulated future:

- **[`Frame`](../../src/test/java/plugins/colosseum/simulation/Frame.java)** — immutable
  identity data, shared by reference across every simulated future branched from one
  snapshot: the collision grid, each NPC slot's type and max HP, the player's loadout and
  max HP/prayer, the wave-start-gating flag, and the warband cycle phase. Copying a state
  never copies the frame.
- **[`State`](../../src/test/java/plugins/colosseum/simulation/State.java)** — everything
  that changes tick to tick: positions, HP, prayer points, cooldowns, supplies, in-flight
  attacks, and per-rollout score aggregates (damage taken, kills, whether the player
  died).

NPCs live in parallel arrays indexed by *slot* (up to 16 slots): `npcPos[slot]`,
`npcHp[slot]`, `npcCooldown[slot]`, plus a flags byte and an auxiliary byte that stores
per-type extras (manticore pattern and orbs remaining, or the javelin colossus's
attack-count-since-special). The frame maps each slot back to a live RuneLite NPC index
so decisions can be executed against the real game.

## The arena grid

[`Grid`](../../src/test/java/plugins/colosseum/simulation/Grid.java) is an immutable
64×64 collision map in *arena-local* coordinates ((0,0) = south-west corner). Each row is
stored as one 64-bit mask — bit `x` of row `y` says whether that tile is blocked — so a
collision check is a single bit test. Two masks are kept: one for movement blocking, one
for line-of-sight blocking (inside the colosseum every obstacle blocks both, but they are
captured independently from the client).

Large NPCs get special treatment. A 3×3 manticore occupies nine tiles, anchored at its
south-west corner; asking "can it stand here?" would naively be nine bit tests. Instead
the grid precomputes an **eroded** mask per footprint size: `blockedForSize[3]` has a bit
set exactly where a 3×3 footprint anchored on that tile would overlap an obstacle or
leave the arena. Step legality for any NPC size is again a single bit test
(`isFootprintFree`).

Grids are built two ways: from the live client collision map
(`Grid.fromCollisionFlags`, used by `Capture` — anchored so the 64×64 window covers the
player's map region, which for the colosseum is the whole arena), or synthetically from a
list of blocked tiles (`Grid.synthetic`, used by tests).

## Line of sight

[`LineOfSight`](../../src/test/java/plugins/colosseum/simulation/LineOfSight.java)
implements the same fixed-point ray-walk the OSRS client uses (and the community
simulator validated): step tile by tile from attacker to target; if any stepped tile
blocks sight, there is no line of sight. Details that matter in play:

- For a multi-tile NPC, the ray is cast from the footprint tile *closest to the target*.
- **Melee never raycasts.** Melee reach (range 1) requires standing orthogonally adjacent
  to the attacker's bounding box — a melee NPC can never attack diagonally.
- A target standing underneath a large NPC's footprint can't be attacked by it.
- Range is checked with Chebyshev distance ("board distance", where a diagonal step
  counts as 1) from the nearest footprint tile.

## What the player can do in one tick: `PlayerCommand`

[`PlayerCommand`](../../src/test/java/plugins/colosseum/simulation/PlayerCommand.java)
packs one tick of player intent into a single `long`, mirroring what a real player can
queue in one tick:

| Field | Meaning |
|---|---|
| Move destination (+ run flag) | One ground click, or "stop moving" |
| Overhead prayer | Keep current / off / Protect from Melee / Missiles / Magic |
| Gear set | Keep current, or switch to set 0–2 (all equips resolve the same tick) |
| Consumables | Eat food, eat combo food (karambwan), sip brew, sip restore — each at most once |
| Attack target | Keep current / clear / attack NPC slot N |
| Special attack | Use spec on the next player attack |

`PlayerCommand.NONE` means "keep doing whatever previous ticks set up" — an in-progress
walk continues, an engaged fight keeps attacking on cooldown. Builder-style helpers
(`withOverhead`, `withMove`, `withConsumables`, …) OR the pieces together.

Two interaction rules carry over from the real client: a ground click cancels the current
attack interaction, and an attack click cancels the current walking path.

## Anatomy of one simulated tick

`Tick.advance` replicates the real engine's processing order — client input, then NPC
turns, then the player's turn. The order is not a detail; it is what makes prayer
flicking and tick-eating work exactly like they do in game.

**Phase 1 — client input.** The command applies: prayer switches, gear swap, eating and
sipping, movement/attack clicks. Because the real client processes your clicks at the
start of the *next* tick, a decision made while watching tick T−1 takes effect here at
the start of tick T — so the prayer you click "for" a tick is active for everything that
happens during it.

**Phase 2 — NPC turns.** First, hits the *player* launched on earlier ticks land on NPCs
now — so an NPC killed this tick never gets its turn. Then each NPC, in a fixed
processing order (manticores first, warband last, matching the community simulator —
this decides who wins contested tiles):

1. counts its attack cooldown down;
2. if it has line of sight (and range) to the player, it **holds position**; otherwise
   it takes one step toward the player (see [NPC movement](#npc-movement) below);
3. manticores that just gained line of sight begin their charge, as a group;
4. NPCs with line of sight and an expired cooldown launch an attack.

Protection prayers are evaluated **at launch**: the prayer active at the start of the
attack tick decides whether the hit is nullified, and the launched hit carries its final
damage. Praying after the launch does nothing, even if the projectile lands later. The
one exception is the minotaur's melee (checked on landing, one tick later).

**Phase 3 — player turn.** Queued hits landing this tick apply to the player — *after*
phase 1's healing, which is why tick-eating works (eat on the tick a big hit lands and
the heal counts first). Then the player moves (1 tile walking, 2 running, along a
breadth-first-search path toward the clicked destination), then attacks if engaged, in
range, and off cooldown.

**Phase 4 — housekeeping.** Eat/potion/attack timers tick down, prayer points drain, run
energy drains or regenerates, special attack energy regenerates.

## NPC behaviour

### Stats

Encoded in [`NpcType`](../../src/test/java/plugins/colosseum/simulation/NpcType.java)
(sourced from the OSRS Wiki):

| NPC | Size | Style | Range | Speed (ticks) | Max hit | HP | Pathing | Special |
|---|---|---|---|---|---|---|---|---|
| Manticore | 3×3 | Ranged/Magic/Melee orbs | 15 | 10 | 36 / 31 / 31 per orb | 250 | naive | Charged triple attack |
| Serpent shaman | 1×1 | Magic | 10 | 5 | 28 | 125 | naive | — |
| Javelin Colossus | 3×3 | Ranged | 15 | 5 | 48 | 220 | naive | Sky javelin every 5th attack |
| Shockwave Colossus | 3×3 | Magic | 15 | 5 | 56 | 125 | naive | — |
| Jaguar warrior | 2×2 | Melee | 1 | 5 | 47 ×3 hits | 125 | naive | Rolls three hits per attack |
| Minotaur | 3×3 | Melee | 1 | 5 | 74 | 225 | naive | Heals other NPCs |
| Minotaur (Red Flag) | 3×3 | Melee | 1 | 5 | 74 | 225 | smart | Heals other NPCs |
| Fremennik berserker | 1×1 | Melee | 1 | 6 | 29 | 48 | smart | Warband 6-tick cycle |
| Fremennik seer | 1×1 | Magic | 1 | 6 | 12 | 50 | smart | Warband 6-tick cycle |
| Fremennik archer | 1×1 | Ranged | 1 | 6 | 14 | 50 | smart | Warband 6-tick cycle |

Note the Fremennik warband members deal magic/ranged/melee *damage* but all attack only
from melee reach (range 1) — the style matters for which prayer blocks them, not for how
far they can hit.

### Wave-start gates

At the start of a wave (validated against the community simulator), NPCs are briefly
inert: no movement on tick 0, no line-of-sight acquisition before tick 2, no attacks
before tick 3. The gates only apply when the state was captured at a wave start
(`Frame.waveStartGates`).

### NPC movement

Two movement models, matching observed behaviour:

- **Naive chase** (most NPCs): step diagonally toward the player if that tile is legal,
  otherwise try the x-axis step, then the y-axis step. This is intentionally dumb — it is
  what the real NPCs do, and it is why they get stuck ("hug") on pillars, which is the
  core of colosseum cover play.
- **Smart pathing** (Fremennik warband, Red Flag minotaur): these route *around*
  obstacles. The engine gives them a breadth-first-search **approach field** — a
  flood-fill, outward from every tile adjacent to the player, of "how many steps to reach
  melee range from here" — and each tick they step to the neighbouring tile with the
  lowest remaining distance. Tie-breaking prefers the same diagonal-then-cardinal order
  as the naive chase.

NPCs never step onto each other or onto the player, and a size-1 NPC cannot cut corners
diagonally.

### Manticores: the charged triple attack

The colosseum's signature mechanic, modelled in full:

1. The first time a manticore has line of sight to the player (tick 3 or later), it
   begins a **10-tick charge**. All manticores that begin charging on the same tick share
   one attack pattern.
2. The pattern is the order of its three orbs. Below the Mantimayhem III modifier it is
   either **ranged → magic → melee** or **magic → ranged → melee** (melee always last).
   The pattern is visible in game as a spot-animation during the charge, which is how the
   live tracker learns it.
3. When its charge (or its 10-tick attack cooldown, for repeat attacks) expires, it fires
   a **triple**: three orbs launched on consecutive ticks. Orbs have no travel time —
   prayer is checked on each orb's *launch* tick, so blocking a whole triple means
   switching prayers three ticks in a row in pattern order (`MechanicsTest`
   demonstrates this).
4. **Only one manticore may begin firing per tick.** Other ready manticores are pushed
   back 5 ticks. This stagger is what makes multi-manticore waves flickable at all.
5. Once a triple has started, the remaining orbs launch on schedule even if the player
   breaks line of sight.

When a manticore's pattern is unknown (tracker hasn't seen the spot-anim), the engine
plans conservatively: the last orb is known to be melee (below Mantimayhem III), and the
first two are treated as ranged-or-magic with expected damage averaged over both — while
worst-case damage assumes the worst.

### Javelin Colossus: the sky javelin

Every 5th attack, instead of a normal throw, it launches a javelin into the sky aimed at
the tile the player is standing on *at launch*. It lands **6 ticks later** for up to 40
typeless damage — typeless meaning no prayer protects against it. The dodge is simply not
being on the targeted tile when it lands; the engine records the targeted tile on the
pending hit and applies damage only if the player is still there.

### Minotaur: the healer

On its attack timer, a minotaur with the player in melee reach attacks — and its melee
damage lands **one tick after** the attack, with prayer checked on the landing tick. That
makes it the one colosseum hit that can be both tick-eaten and late-prayed.

If no player is in melee reach when its timer expires, it scans for another NPC to heal
instead: below 75% max HP, centre within 7 tiles of the minotaur's centre, line of sight,
and not itself a minotaur. It heals the most-damaged qualifying NPC. The wiki says it
"heals continuously to full" without a number; the engine uses a deliberately high 30 HP
per cycle so plans respect that a defended NPC effectively can't be killed.

### Fremennik warband: the metronome

All warband members attack on a shared fixed 6-tick cycle relative to the wave start —
they attack only on ticks where `tick % 6` equals the wave's observed phase, only from
melee reach, and only if they did not move that tick. Combined with smart pathing, they
behave like a slow, synchronized pincer. They are fragile (48–50 HP) and each is weak to
a different combat style, which is why the planner kills them first.

### Jaguar warrior

A melee reinforcement that rolls three independent hits per attack; the engine models the
combined attack (its max-hit stat × 3) as a single launch.

## The player model

- **Movement.** A movement click builds a breadth-first-search distance field over
  player-legal steps toward the destination (diagonal steps require both flanking
  cardinal tiles to be free — the standard player movement rule), then the player walks
  downhill along it: 1 tile per tick walking, 2 running. Fields are cached per
  destination in `Scratch` and shared across all rollouts of a planning pass.
- **Run energy.** Stored as 0–10,000 internal units (100 = 1%). Running drains
  `(60 + 67·weight/64) · (300 − agility)/300` units per tick (×0.3 with a stamina effect
  active); not running regenerates `agility/10 + 15` units per tick.
- **Combat.** The player attacks when engaged, in range with line of sight, and off
  cooldown. Damage is the loadout's *expected damage* for the current gear set against
  the target's type (see [`LoadoutConfig`](../../src/test/java/plugins/colosseum/simulation/LoadoutConfig.java)),
  doubled by a special attack (which costs 50 energy). The attack cooldown is the gear
  set's attack speed.
- **Hit delays.** Projectiles take time: at Chebyshev distance `d`, ranged hits land
  after `1 + (3+d)/6` ticks and magic after `1 + (1+d)/3` (integer division); melee is
  instant. Player hits land one tick later than the formula because NPCs are processed
  before players within a tick (the "receiver processed earlier" rule).
- **Eating.** Primary food (default: anglerfish-style, heals 22) sets a 3-tick food
  delay and pushes the attack cooldown back 3; combo food (karambwan, heals 18) has its
  own 3-tick delay and pushes attacks back 2 — and can be eaten the same tick as primary
  food, the classic combo-eat. Brews (16 HP/dose) and restores (31 prayer/dose) share a
  3-tick potion delay.
- **Prayer drain.** One active overhead drains 12 drain-effect points per tick against a
  resistance of `2 × prayer bonus + 60`; each time the accumulated drain exceeds the
  resistance, one prayer point is lost. Switching prayers costs nothing; the activation
  tick itself doesn't drain.
- **Special attack.** Regenerates 10% every 50 ticks (every 25 with a Lightbearer).

## Damage accounting: expected value plus a worst-case floor

The engine does not roll random damage. Every hit is tracked two ways at once:

- **Expected damage** — accuracy × average roll, the statistical mean. Unprayed NPC hits
  deal `maxHit × 0.30` expected damage by default (colosseum NPCs are accurate; the
  factor is configurable in `LoadoutConfig`). This is what actually reduces simulated
  HP, and what the planner optimises.
- **Worst-case burst** — the sum of *maximum* rolls of everything landing on a given
  tick. The state tracks `worstCaseHpFloor`: the lowest value of "HP minus that tick's
  worst-case burst" seen anywhere in the rollout. If the floor goes below zero, the
  player *could have* died on some tick even if the expected trajectory survives — and
  the scorer punishes that heavily.

This split is why the planner behaves like a careful human: it plays the averages for
damage output but refuses plans that leave it one unlucky roll from death.

In-flight attacks live in a small ring buffer of packed `long`s on the state, each
carrying: landing tick, worst-case damage, style, source slot, targeted tile (for sky
javelins), whether prayer was already resolved at launch, and expected damage.

## Model simplifications

Known, deliberate simplifications — kept because the planner replans from a fresh
snapshot every tick, so small model errors do not compound:

- Damage is expected-value, not rolled; the worst-case floor guards lethality.
- Player damage per gear set is a flat expected value per NPC type — tune
  `LoadoutConfig.GearSet#expectedDamageByType` to your actual gear.
- Brews do not model stat drain; restores only restore prayer points.
- The minotaur heal amount (30/cycle) is an estimate on the high side.
- The Mantimayhem modifier tier is currently always captured as 0 (melee-first manticore
  patterns exist in the engine but are never produced by live capture).
- Sol Heredit and wave-12 mechanics are entirely out of scope.

## Scratch: reusable working memory

[`Scratch`](../../src/test/java/plugins/colosseum/simulation/Scratch.java) holds the
engine's temporary memory so the hot loop stays allocation-free: BFS queues, a small LRU
cache of player path fields keyed by destination, per-size NPC approach fields keyed by
player position, and the per-tick "which NPCs moved" flags. One instance per planning
thread; call `invalidate()` when the grid changes. It also exposes an optional
`attackListener` hook that tests use to record every NPC attack launch.
