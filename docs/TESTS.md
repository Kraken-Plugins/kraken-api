# Kraken API Tests

This project is unique in that it functions exclusively within a RuneLite game client environment. This means that automated tests through a framework
like JUnit don't provide as much value. Sure, you can test functionality within the API but does is **really** find NPC's within 10 tiles of your player? 
The only way to know for sure is to run tests within the game client.

You can run tests by running the main class in `PluginRunnerTest.java` with the following args: `plugins.api.ApiTestPlugin --developer-mode`. This will launch a new game client which loads a custom "Testing" plugin
called "API Tests" which you will see within RuneLite. Through this plugin you can run specific tests which cover various query and service-related classes
and dump output into the console and the overlay for PASS/FAIL.

Most of the tests are fully self-sufficient; that is, they set themselves up with the necessary in game items before running the tests. However,
there are a few conditions listed below.

## Running the whole suite

Stand at **Varrock East Bank** and tick **Run All Tests** in the General section. The runner:

1. Orders the tests to minimise walking — everything that works anywhere runs first, then each
   location is visited once, and within a location the tests that teleport or move you run last.
2. Establishes each test's declared preconditions before running it: travelling, opening the bank,
   depositing, withdrawing, equipping.
3. Records anything it cannot set up as **Skipped, with the reason** — "Rune platebody is not in the
   bank" — and carries on.

**A red result therefore always means a genuine regression.** Orange means your environment was not
ready. That distinction is the whole point of the run.

Other controls in the General section:

| Control | What it does |
| --- | --- |
| **Run Group** | Runs one category only (Query / Service / Interaction / Input). Much faster when chasing a regression in one area. |
| **Stop Run** | Cancels the run in progress. Interrupts the worker, so it stops mid-walk rather than finishing the current test. |
| **Establish Preconditions** | On by default. Turn it off to run a test exactly as it stands — the fastest way to tell whether a red is a real regression or the setup putting the world in the wrong state. |
| **Bank PIN** | Needed for automated setup if you have one; otherwise every bank test is skipped rather than hanging on the keypad. |

Ticking any individual test's checkbox still runs just that test, now with its preconditions
established too.

**The suite banks everything you are carrying and worn** — deterministic setup is only reachable from
a known-empty state. Bank anything you care about first. A full pass takes roughly 25–45 minutes;
`WorldQueryTest` and `GrandExchangeServiceTest` are excluded from bulk runs because they hop worlds
and spend real coins respectively, and stay available individually.

## Start here after a client update: `SelfCheckTest`

Run **Self Check** (General section) before anything else. The harness drives the client using the very
API it is testing, so when a primitive like `interact` regresses, every test fails during its own setup
and the results tell you nothing useful. Self Check verifies just those primitives, cheapest first, and
reports the earliest one that broke:

1. The local player and its world location are readable from a non-client thread.
2. `gameObjects().withAction("Bank")` (or `npcs().withAction("Bank")`) resolves a bank.
3. `interact("Bank")` actually opens the bank interface.
4. The bank and inventory containers agree: one item is withdrawn, the inventory count is observed to
   rise, and the item is deposited straight back.
5. The bank interface closes again.
6. The player walks to a nearby tile that `reachableTiles()` already reported as reachable.

**If Self Check fails, treat every other failure in that run as unexplained** — fix the underlying API
first. If it passes, the harness can drive the client and other failures are real signal.

- **Location**: any bank booth or banker (the hub bank works).
- **Bank**: at least one item in the bank, and no PIN (or pre-entered).
- **Inventory**: at least one free slot.
- **Note**: non-destructive. The withdrawn item is deposited back and the player moves a few tiles.

## Where to stand

The suite is deliberately concentrated into **two locations** so that sequenced runs spend as little
time walking as possible:

| Stop | Covers |
| --- | --- |
| **Varrock East Bank** (the hub) | Everything except the three below. Bank booths, bankers, guards and men for combat/NPC tests, and the Varrock Square fountain a few tiles north for `WidgetTargetGameObjectTest`. The Varrock teleport also lands here, so `SpellServiceTest` returns to the hub on its own. |
| **Grand Exchange** (~90 tiles north west) | `GrandExchangeServiceTest`, plus `DepositBoxTest` and `DepositBoxServiceTest` — the GE has bankers, clerks *and* a deposit box, so all three share one trip. |

Several tests used to require their own location for reasons unrelated to what they actually verify.
Those dependencies have been removed rather than automated around:

- `TaskChainTest` no longer walks to the **Lumbridge** canoe station (~250 tiles). It exercises the
  same `TaskChain` primitives against a bank booth. No axe needed.
- `ProcessingServiceTest` no longer needs the **Barbarian Village fire** (~150 tiles). It drives the
  same make-X interface by cutting a gem at the bank. No raw fish, no Cooking requirement.
- `AreaServiceTest` no longer uses hardcoded Varrock East coordinates; it builds every area relative
  to wherever the player is standing and runs anywhere.
- `GameObjectTest` no longer needs an Oak tree nearby.

## General Requirements

- **Location**: Start at **Varrock East Bank** unless a test below says otherwise.
- **NPCs**: Nearby "Guard" NPCs must be present (Varrock Square has them).
- **Players**: Some tests require other players to be nearby (e.g., `PlayerTest`).
- **Bank**: The player must be near a bank booth and have either no PIN set or have pre-entered their bank pin unless otherwise specified in the test docs below.

## Inventory & Bank Requirements

The following items must be present in your **Bank**:

- **Armor/Weapons**:
    - Rune Full Helm
    - Rune Platebody
    - Rune Platelegs
    - Rune Scimitar
    - Bronze Scimitar (`DpsServiceTest`)
    - Amulet of Power (`DpsServiceTest`)
    - Shortbow (`DpsServiceTest`)
    - Iron Arrows (at least 10, `DpsServiceTest`)
- **Food**:
    - Lobster (at least 5)
    - Swordfish (at least 5)
- **Runes**:
    - Law runes (at least 1)
    - Fire runes (at least 4 but preferably more)
    - Air runes (at least 3)
    - Mind runes (at least 10)
- **Tools & misc**:
    - Chisel (`ProcessingServiceTest`, `WidgetTargetWidgetTest`)
    - Uncut sapphires, at least 2 (`ProcessingServiceTest` consumes one, `WidgetTargetWidgetTest` uses one)
    - Empty bucket (`WidgetTargetGameObjectTest`)
    - Ring of dueling (`WidgetSubActionTest`)

## Skill & Spellbook Requirements

- **Prayer**: Protect from Melee prayer must be unlocked at level 43 prayer.
- **Spellbook**: Must be on the **Standard Spellbook**.
- **Magic Level**: High enough to cast Varrock Teleport (Level 25).
- **Crafting Level**: Level 20 to cut a sapphire (`ProcessingServiceTest`).

## Specific Test Requirements

### `ProcessingServiceTest`
- **Location**: At the hub bank. No fire or range needed.
- **Inventory**: A chisel and at least one uncut sapphire, plus a free inventory slot.
- **Level**: 20 Crafting.
- **Note**: Consumes one uncut sapphire. Asserts the make-X interface opens, that the quantity varc
  round trips, and that a cut sapphire actually appears — none of which the old cooking version checked.

### `InventoryTest`
- **Location**: Any bank which is currently open
- **Inventory**: You cannot have a gold bar or a sapphire in your inventory

### `TaskChainTest`
- **Location**: At the hub bank, with a few tiles of open ground to walk on.
- **State**: Start with the bank **closed** — the chain asserts that it can open it. If it is open the
  test closes it first.
- **Note**: Walks a short distance, opens the bank and closes it again, then verifies the world really
  matches what `TaskChain.execute()` reported.

### `DialogueServiceTest`
- **Location**: Near a Banker (the hub bank works).
- **State**: Ensure no dialogue is currently open before starting.

### `MouseTest`
- **Configuration**: If using `REPLAY` strategy, a recording named "test" must exist. If using `LINEAR`, no specific setup is needed other than valid targets nearby.

### `PathfinderServiceTest`
- **Location**: Starts near Varrock East Bank (specifically `WorldPoint(3253, 3421, 0)`).
- **Note**: This test moves the player.

### `MovementServiceTest`
- **Interaction**: Requires user interaction (Shift + Right Click 'Walk here' -> 'Set' on a tile) to begin movement.

### `GroundObjectTest`
- **Inventory**: Requires at least one item that can be dropped (e.g., a fish or rune).

### `EquipmentTest`
- **Bank**: Requires Rune armor set (Helm, Body, Legs, Scimitar) to be in the bank.
- **State**: Player should ideally NOT be wearing the Rune armor at the start (the test will withdraw and equip it).

### `SpellServiceTest`
- **Bank**: Requires Fire, Air, and Law runes in the bank.
- **Level**: Level 25 magic to cast Varrock Teleport
- **State**: Bank must be openable (No PIN or pre-entered PIN).

### `CameraServiceTest`
- **Interaction**: Requires a target tile to be selected via the plugin overlay/interaction before the test proceeds.

### `GrandExchangeServiceTest`
- **Location**: Start at the grand exchange near the bankers and clerks. This does not require that the bank be open
- **Bank**: Requires at least three fire runes. This will sell the fire runes at a loss and by fire runes for 15 gp each (to test instabuy).

### `AreaServiceTest`
- **Location**: Anywhere with open, walkable ground — every area is built relative to the player, so
  the rendered overlays follow you rather than sitting at fixed Varrock East coordinates.
- **Note**: Enable the area overlay in Overlay Settings to see the reachable, polygon and radius areas.

### Widget interaction tests

These cover the four shapes of widget interaction. All except `WidgetSubActionTest` run at the hub.

| Test | Needs | Behaviour |
| --- | --- | --- |
| `WidgetActionTest` | A weapon with a special attack | Clicks the spec orb. Fails if spec is *already* enabled, so start with it off. |
| `WidgetTargetWidgetTest` | Chisel + uncut sapphire | Item on item. |
| `WidgetTargetGameObjectTest` | Empty bucket, Varrock Square fountain | Item on object. Asserts a **Bucket of water** actually appears. |
| `WidgetTargetNpcTest` | Air/Mind/Fire runes, nearby Guard | Casts Fire Strike on an NPC. |
| `WidgetSubActionTest` | Ring of dueling | Nested sub action. **Teleports you to Emir's Arena** in Al Kharid, so run it last. Previously targeted Fortis Colosseum, which is members only. |

### `DpsServiceTest`
Runs entirely on free-to-play gear and NPCs, so it can be run on an F2P world or account.

- **Location**: Any bank booth with a free-to-play combat NPC nearby. The hub bank works: it has bank booths and Guards. The test picks the nearest **Guard**, **Man**, or **Goblin** as the target it calculates against, and fails if none of them are in the scene.
- **Bank**: Must be openable (no PIN or pre-entered PIN) and contain the full free-to-play kit below. The test deposits your entire inventory *and* everything you are wearing before withdrawing it, so bank anything you want to keep out of the way first.
    - Bronze scimitar
    - Rune scimitar
    - Amulet of power
    - Rune full helm, Rune platebody, Rune platelegs
    - Shortbow
    - Iron arrows (10 are withdrawn)
- **Levels**: 40 Attack (rune scimitar), 40 Defence (rune armour), 1 Ranged (shortbow and iron arrows). Below 40 Attack/Defence the equip steps fail because the kit cannot be worn.
- **State**: No skill boosts should be wearing off mid-run. The test compares live DPS readings taken seconds apart and expects the only thing changing between them to be the gear.
- **Note**: The test leaves you wearing the shortbow, iron arrows and amulet of power, with the rest of the kit in your inventory. Nothing is dropped or consumed.

What it covers:

| Stage | What is checked |
| --- | --- |
| Data lookups | `item`, `monster` (by id, name and live NPC), `monstersByName` and `categorize`. Verifies gear is classified into the right style (scimitar → MELEE, shortbow and arrows → RANGED, staff → MAGIC, rune armour → NONE) and that unknown items/monsters return null. |
| Loadout DPS | Builds loadouts by hand at your live skill levels and compares them: a rune scimitar out-DPSes a bronze one and kills faster, an amulet of power raises the attack roll and DPS, and rune armour — which has no offensive melee bonus — leaves melee DPS untouched. Also checks the `DpsResult` fields (4 tick scimitar, 3 tick rapid shortbow, accuracy as a probability) and that `calculator()`, `calculate(loadout, monster)` and `calculate(loadout, npcId)` all agree. |
| Equipping gear | Strips the player, then equips the bronze scimitar → rune scimitar → amulet of power → rune armour, re-reading live DPS with `calculateCurrent` at every step and asserting it moves the way each piece implies. Also verifies `currentEquipment`, `currentLoadout` and `availableGear` read back what is actually worn and carried. |
| Gear search | Runs `findBestGear(npc)`, then applies the change set it returns (equipping `itemsToEquip`, emptying `slotsToRemove`) and verifies the gear now worn *is* the loadout the search chose and reproduces the DPS it reported. Asserts the best melee setup picks the rune scimitar over the bronze one, the best ranged setup picks the shortbow with iron arrows, and that no magic setup is offered when no magic weapon is available. Finally re-runs the search restricted to `GearCategory.RANGED` via `findBestGear(monster, config)` to cover a change set that has to swap weapons rather than only strip gear. |

Because the search prunes on gear category, the best melee loadout it returns is a rune scimitar and amulet of power with the **rune armour stripped off** — the armour is purely defensive, so removing it costs nothing and the search will not keep it. That is expected, not a bug.

### `DepositBoxQueryTest`
- **Location**: Must be at a **deposit box**. Use the **Grand Exchange** one so this shares a trip with
  `GrandExchangeServiceTest` and `DepositBoxServiceTest`.
- **State**: The deposit box interface must be open before the test runs.
- **Equipment**: You must be wearing an item in your head slot like a coif, full helm etc...
- **Inventory**: The following items must be in your **inventory** before opening the deposit box:
  - **Coins** (any amount) — used to test `depositOne` on a stackable item
  - **Swordfish** (at least 5 noted) — used to test `depositFive`
  - **Lobster** (at least 10 noted) — used to test `depositTen`
  - **Fire runes** (at least 3) — used to test `depositX`
  - **Law runes** (at least 1) — used to test `depositAll` and verify removal
- **Note**: These items are consumed from your inventory into the deposit box during the test. They can be reclaimed from the bank afterward.