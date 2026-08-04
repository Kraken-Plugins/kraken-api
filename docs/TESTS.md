# Kraken API Tests

This project is unique in that it functions exclusively within a RuneLite game client environment. This means that automated tests through a framework
like JUnit don't provide as much value. Sure, you can test functionality within the API but does is **really** find NPC's within 10 tiles of your player? 
The only way to know for sure is to run tests within the game client.

You can run tests by running the main class in `PluginRunnerTest.java` with the following args: `plugins.api.ApiTestPlugin --developer-mode`. This will launch a new game client which loads a custom "Testing" plugin
called "API Tests" which you will see within RuneLite. Through this plugin you can run specific tests which cover various query and service-related classes
and dump output into the console and the overlay for PASS/FAIL.

Most of the tests are fully self-sufficient; that is, they set themselves up with the necessary in game items before running the tests. However,
there are a few conditions listed below.

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

- **Location**: any bank booth or banker (Varrock West Bank works).
- **Bank**: at least one item in the bank, and no PIN (or pre-entered).
- **Inventory**: at least one free slot.
- **Note**: non-destructive. The withdrawn item is deposited back and the player moves a few tiles.

## General Requirements

- **Location**: Most of the tests are designed to be run from **Varrock West Bank**.
- **NPCs**: Nearby "Guard" NPCs must be present (Varrock West Bank has them).
- **Players**: Some tests require other players to be nearby (e.g., `PlayerTest`).
- **Bank**: The player must be near a bank booth (Varrock West Bank) and have either no PIN set or have pre-entered their bank pin unless otherwise specified in the test docs below.

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
    - Raw Salmon (at least 2)
    - Raw Trout (at least 2)
- **Runes**:
    - Law runes (at least 1)
    - Fire runes (at least 4 but preferably more)
    - Air runes (at least 3)

## Skill & Spellbook Requirements

- **Prayer**: Protect from Melee prayer must be unlocked at level 43 prayer.
- **Spellbook**: Must be on the **Standard Spellbook**.
- **Magic Level**: High enough to cast Varrock Teleport (Level 25).
- **Cooking Level**: Cooking level of 25 to cook salmon.


## Widget Action Test Requirements

Widget actions require that you have the following items in your inventory:
- Empty bucket
- Uncut sapphire
- Chisel
- Air, Mind, and Fire runes

You must be near Varrock east bank to run these tests as it requires a nearby guard and fountain.

## Specific Test Requirements

### `ProcessingServiceTest`
- **Location**: Must be near the **Barbarian Village fire** (permanent fire).
- **Inventory**: Requires at least 2 Raw Salmon and 2 Raw Trout in inventory.

### `InventoryTest`
- **Location**: Any bank which is currently open
- **Inventory**: You cannot have a gold bar or a sapphire in your inventory

### `TaskChainTest`
- **Location**: Expects to run near **Lumbridge**.
- **Requirements**:
    - Must be able to chop a tree (Axe in inventory or equipped).
    - Must be near a "Canoe Station" (Lumbridge has one).

### `DialogueServiceTest`
- **Location**: Near a Banker (Varrock West Bank works).
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
- **Location**: Requires you to be in varrock east bank to see overlays of various areas.

### `DpsServiceTest`
Runs entirely on free-to-play gear and NPCs, so it can be run on an F2P world or account.

- **Location**: Any bank booth with a free-to-play combat NPC nearby. Varrock West Bank works: it has bank booths and Guards. The test picks the nearest **Guard**, **Man**, or **Goblin** as the target it calculates against, and fails if none of them are in the scene.
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
- **Location**: Must be at a **deposit box** (e.g., Edgeville or Castle Wars).
- **State**: The deposit box interface must be open before the test runs.
- **Equipment**: You must be wearing an item in your head slot like a coif, full helm etc...
- **Inventory**: The following items must be in your **inventory** before opening the deposit box:
  - **Coins** (any amount) — used to test `depositOne` on a stackable item
  - **Swordfish** (at least 5 noted) — used to test `depositFive`
  - **Lobster** (at least 10 noted) — used to test `depositTen`
  - **Fire runes** (at least 3) — used to test `depositX`
  - **Law runes** (at least 1) — used to test `depositAll` and verify removal
- **Note**: These items are consumed from your inventory into the deposit box during the test. They can be reclaimed from the bank afterward.