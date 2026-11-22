# Walker

`Walker` (`com.kraken.api.service.walker.Walker`) walks the player anywhere in the game world,
operating whatever doors, boats, shortcuts and teleports the route needs on the way.

It composes the two pieces that already existed rather than replacing either: `GlobalPathfinder`
plans a route, `MovementService` sends the clicks, and the walker runs the loop between them —
plan, walk the part of the route the client has loaded, operate the transport at the end of it,
re-plan from wherever the player ended up.

The scene is 104 tiles square. If the next path tile is outside it — a compressed route whose first
step is the castle stairs, eighty tiles south — the walker clicks the scene-edge tile toward that
step rather than treating an empty clip as arrival. If they are already standing on that tile — a
staircase origin whose next waypoint is the same x/y on another plane, or the scene edge itself —
the approach is finished and the transport is operated; that is not a failed walk. Stall detection
uses 2D distance, because `WorldPoint.distanceTo` is `Integer.MAX_VALUE` across planes and would
otherwise count every round toward an upstairs bank as progress.

An incomplete plan is not walked. The search returns the closest tile it found when it cannot
reach the destination — Mudskipper Point when Karamja needs a 30gp sailor and the inventory has
none. Following that stub cannot create a boat that is not in the graph, so the walk ends with
`NO_ROUTE` without moving. A home teleport is still considered first when the courtyard would
leave a complete remaining walk. An incomplete stub whose last tile is already within walk
tolerance is followed, because arriving there is success.

## Global search execution

All blocking `GlobalPathfinder` entry points reject the client thread with `IllegalStateException`.
Run them from a script loop or another worker. Static resource loading happens on that worker;
live transport eligibility is captured into a separate configuration for each request on the client
thread. Searches do not hold a monitor while waiting for capture, and another request cannot refresh
the configuration a search is using. `lastResult` is the last request to finish.

`GlobalPathfinderConfig` has two hard search limits: `maxSearchMillis` (default 10,000 ms, monotonic)
and `maxSearchNodes` (default 1,000,000 allocated graph nodes, including queued transports).
Both must be positive. The existing `calculationCutoffMillis` remains a separate no-progress limit.
Limits apply to graph expansion, excluding resource loading, client capture and result construction;
checks occur between expansions and before every node allocation. Thread interruption and script
cancellation stop expansion too. A limit returns an incomplete best-effort `PathResult`; list-returning
helpers return an empty list for incomplete routes. These are cooperative limits, not JVM preemption.

## Usage

```java
@Inject
private Walker walker;

WalkResult result = walker.walkTo(new WorldPoint(3164, 3486, 0));
if (!result.isSuccess()) {
    log.warn("did not arrive: {}", result.getReason());
}
```

Overloads take a tolerance in tiles, or a full `WalkerConfig`:

```java
walker.walkTo(destination, 5);

walker.walkTo(destination, WalkerConfig.builder()
        .tolerance(2)
        .timeoutMillis(60_000)
        .pathfinderConfig(GlobalPathfinderConfig.builder()
                .useFairyRings(false)
                .avoidWilderness(true)
                .build())
        .build());
```

`WalkerConfig` carries the `GlobalPathfinderConfig` deliberately: the two have to agree. Turning a
transport type off there stops the planner proposing it, which is how a transport the walker cannot
operate is kept out of a route rather than failing mid-walk. Canoes and minigame teleports default
off for that reason; a custom pathfinder config can turn them back on, and the walk then fails with
`TRANSPORT_UNSUPPORTED` if the route still uses one.

On a free-to-play world `PathfinderLiveConfig` also turns off members-only types (agility shortcuts,
fairy rings, jewellery, canoes, and the rest) even if the caller left them on. Boats, ships and
standard-book spells stay available. Plan-only callers of `GlobalPathfinder` are not adjusted unless
they resolve the config themselves.

When teleportation items are on, charged jewellery in inventory or worn is used — a Ring of wealth
(1) is a Grand Exchange teleport. The shortest-path overlay default is permanent items only, which
would skip the ring and walk a glider instead. Those item teleports have no origin tile, so a hop
that is not a walking neighbour is matched against usable teleports. Without that, the plan is an
11-tile jump to the GE with `0 transports` and the walker walks north instead of rubbing the ring.
If the player is already on that island, the landing is walkable: the walker walks rather than
rubbing. Skipping the glory as an already-open door is what stalled a twelve-tile walk on Musa Point.

Jewellery display info names the stop (`"Amulet of glory: Al Kharid"`). In the inventory the widget
only offers Wear / Rub, so Rub must pick that option rather than the default — the item definition
also lists worn destinations such as `Karamja`, and clicking those as a top-level inventory action
fails immediately. Worn jewellery is the other way around: the destinations are on the equipment
widget, which has no menu until that tab is selected. `ItemTeleportHandler` opens the right tab and
uses the live widget actions. Tablets are a bare name (`"Lumbridge tablet"`): Break *is* the teleport.
Treating the tablet name as a submenu clicked Break and then failed the walk in Lumbridge because
no chat option called `"Lumbridge tablet"` exists. The handler only waits for a destination list when
display info has a colon.

`currencyThreshold` is a willing-to-spend cap on currencies the player already holds, not a substitute
for the coin purse. The planner already requires the real inventory count for dataset rows that list
coins (the Shantay pass). The Al Kharid gate is the exception: shortest-path lists it as a free Open,
so bundled registry rows take its edges over and state VitaLite's 10gp / varp 273 / Prince Ali
Rescue rule as requirements, checked before search and again at click time (see
[Hand written rows](#hand-written-rows)).

## Threading

Every call blocks and must be made off the client thread. Waiting is a no-op on the client thread,
which would turn the loop into a spin that fails in milliseconds, so the walker refuses to run there
and returns `CALLED_ON_CLIENT_THREAD`.

The walker also refuses to plan from inside an instance, because instanced coordinates do not
correspond to the static collision map.

The Lumbridge home teleport is executable (`TELEPORTATION_SPELL_HOME`) but the planner never picks
it: walking is cheaper than the thirty-minute cooldown. When the player is on the standard book,
`AIDE_TELE_TIMER` is clear, they are more than fifty tiles from the courtyard `(3222, 3218, 0)`,
and the dense path from the courtyard is shorter than the walk from here, the walker casts it first
and re-plans from Lumbridge. Farm to the castle bank uses it; farm to the Grand Exchange does not.
Musa Point to the Karamja dungeon does not: 2D distance to `(2856, 9574)` is dominated by the
underground `y + 6400`, which made Lumbridge look closer than a fifty-tile climb. CS2 `isCastable`
does not know about that timer (home teleport has no runes), so cooldown is the varp, matching
VitaLite. A skip after those gates is logged with the reason.

## Results

`WalkResult` says what happened rather than returning a bare boolean, because "did not arrive" has
several meaningfully different causes:

| Outcome | Meaning |
| --- | --- |
| `SUCCESS` | The player reached the destination. |
| `NO_ROUTE` | The pathfinder found no complete route. An incomplete stub (closest land across water, for example) is not walked. |
| `TRANSPORT_REQUIREMENTS_UNMET` | A transport needs an item, level, quest or world state the player lacks. The reason names it. |
| `TRANSPORT_UNSUPPORTED` | The route needs a transport kind the walker cannot yet operate. |
| `TRANSPORT_FAILED` | A transport was attempted but the player did not get through. |
| `STALLED` | The player stopped making progress, or a door/gate on the route could not be opened. The reason names the tile and the object. |
| `TIMED_OUT` | The time or round budget ran out. |
| `IN_INSTANCE`, `CALLED_ON_CLIENT_THREAD`, `UNKNOWN_POSITION` | Refused before starting. |

## How transports are executed

The transport dataset ships with the pinned `shortest-path` dependency and describes twenty six
kinds of transport. They are operated in far fewer ways, so dispatch is by *shape*
(`TransportShapes`), not by type:

| Shape | Types | Implemented |
| --- | --- | --- |
| `SINGLE_CLICK` | doors, gates, stairs, agility and grapple shortcuts, levers, portals, POH portals, jewellery boxes | yes |
| `CLEAR_THEN_WALK` | no dataset type; registry rows only (the webs) | yes |
| `CLICK_THEN_DIALOGUE` | boats, ships, charter ships, magic carpets | yes |
| `HUB_DIALOGUE` | wilderness obelisks | yes |
| `HUB_RESUME_PAUSE` | spirit trees, minecarts | yes |
| `CLICK_THEN_WIDGET` | gnome gliders, hot air balloons, magic mushtrees, quetzals | yes |
| `FAIRY_RING` | fairy rings | yes |
| `ITEM_SUBOP` | teleport items, quetzal whistle, seasonal transports | yes |
| `SPELL` | teleport spells and home teleports | yes |
| `CANOE` | canoes | **no** |
| `GROUPING_TELEPORT` | minigame teleports | **no** |

`TransportShapesTest` asserts every type maps to a shape, so a dependency bump that adds a transport
kind fails the build instead of failing in-game.

`CLEAR_THEN_WALK` is for an obstacle that is cleared and then walked through. A web is the example:
slashing it neither moves the player nor reliably works, so `ClearThenWalkHandler` repeats the menu
action (up to five times) until the far tile is reachable in the live scene and then walks the
player across. A web someone already slashed offers no `Slash`, so it is walked through without a
click. The executor never counts such a transport as "already open" on its own, because reporting
it crossed without moving would leave the next round planning the same edge from the same tile.
The dataset files its webs under the plain transport type, which maps to `SINGLE_CLICK`, so the
webs are bundled registry rows that take those edges over (see
[Hand written rows](#hand-written-rows)).

Ships are a conversation, not a single click. The dataset names a destination option on a specific
NPC id (`Musa Point Captain Tobias 14979`); the live F2P sailor is often the older `Travel` NPC
(`14978`). The walker finds the sailor by name and reads the **transformed** menu — the same list
the click resolver uses — so it clicks `Musa Point` only when that action is actually there, and
`Travel` otherwise, then answers `Yes please` if chat opens.

A few journeys put up a warning overlay after the first click rather than a chat option. Entrana
boats title that overlay `WARNING`; the wilderness ditch titles it `Wilderness Warning` and asks
`Enter Wilderness`. `WarningWidgets` matches the bare `WARNING` title exactly — not a substring of a
menu action — and separately accepts overlay text that contains the word `warning`. The spellbook's
`Warnings` filter is excluded either way: treating it as the overlay cancelled staircase climbs.
Named buttons (`Continue`, `Yes`, `Enter Wilderness`, `Proceed regardless`) are tried first; if the
overlay has none of those yet, resume-pause option 1 is sent on the parent. Walking that parent
widget happens on the client thread — `getParent()` asserts if it is called from the walker's wait
loop. The overlay often appears a tick after the object click, so dismiss runs while waiting for
arrival, not only once immediately after interacting.

### Hub transports

Hub transports store their stops as *nodes*, and the dataset expands them into every permutation —
56 fairy rings become 3,078 edges. That expansion is also what makes them executable: it fills in
`Display info` per destination, which the raw rows leave empty. An expanded fairy ring edge carries
the code of the ring it leads to (`"A L Q"`), and a spirit tree edge carries its menu entry
(`"6: Prifddinas"`). No separate destination tables are needed.

`DisplayInfo` parses the three forms this takes — a bare name, a numbered entry, or a ring code.

**Fairy rings** select through the travel log, which offers one component per code and needs no
knowledge of where the dials currently point; setting the dials by hand is the fallback.
`FairyRingWidgets` resolves codes to components by name against the client's own constants rather
than a copied id table, and `FairyRingWidgetsTest` sweeps all 64 codes so a client update that
renames one fails the build. Two destinations are not codes: Zanaris is an option on the ring
itself, and one entry chains several codes for a multi-hop journey that a single selection cannot
express.

**Spirit trees and minecarts** write their stops as numbered entries (`"4: Grand Exchange"`), but
the live list is not chat. After `Travel` they open packed widget `12255235` (group 187, child 3).
`HubResumePauseHandler` waits for that widget and selects with resume-pause using the dataset
position minus one — Grand Exchange is index 3, matching VitaLite. Waiting for
`DialogueService` never sees this list, so a Stronghold tree used to time out after the click.

**Wilderness obelisks** really are numbered chat options, so they keep `HubDialogueHandler`, which
reuses `DialogueService` — chosen by name where the name matches, by position otherwise.

**Gliders, balloons, mushtrees and quetzals** open a dedicated interface. `WidgetSelectHandler`
covers both layouts it comes in: gliders, balloons and mushtrees give each destination its own
component, which `HubWidgets` names; quetzals build their list at runtime, so the entry is found by
the text it shows. `HubWidgets` also holds the small translation tables for the places where the
dataset and the interface disagree on a name — a glider stop is `"Ta Quir Priw"` in the dataset and
`GRANDTREE` in the interface. `HubWidgetsTest` sweeps every stop of every such hub.

### Still unsupported

**Canoes** are not a destination choice but a build sequence: fell a tree, shape the log into one of
four hulls, then float it, gated on a woodcutting level and an axe. 45 edges.

**Minigame teleports** are driven from the grouping tab, which `InterfaceTab` does not name. 21
edges, and every destination is reachable another way.

Both fail immediately with `TRANSPORT_UNSUPPORTED` and a reason, unless a plugin has registered a
handler for the shape (see [Adding transports at runtime](#adding-transports-at-runtime)). `WalkerConfig` turns `useCanoes`
and `useTeleportationMinigames` off by default so a default walk routes around them instead. A
custom `GlobalPathfinderConfig` can turn them back on; the walk then fails at fire time if the
route still uses one. Plan-only callers of `GlobalPathfinder` keep the library defaults (both on).

### Turning two strings into a click

A transport's executable detail is stored as unstructured text under a column headed
`menuOption menuTarget objectID`, for example `Open Door 9398`. Only the trailing integer is
unambiguous: both the option and the target can contain spaces with no delimiter, so
`Al Kharid Amulet of Glory 13523` is the option `Al Kharid` on `Amulet of Glory`, not `Al`.

`ObjectInfo` therefore parses in two stages — extract the id, then re-split the remainder once the
entity has been resolved and its real name is known. `TransportEntityResolver` also compares menu
text ignoring hyphens and spacing, because the dataset writes `Climb Down Ladder` where the client's
action is `Climb-down`. Scenery is found by id within four tiles of the origin, then by name within
ten — the Stronghold spirit tree sits five tiles south of its dataset origin, and id `26260` is not
always the live object.

## Adding transports at runtime

`TransportRegistry` (`com.kraken.api.service.walker.transport.registry`) holds transports and
handlers that are added while the client runs. There is one registry per client, shared by every
plugin, so a transport one plugin registers is planned through and operated by every walk from the
next search on. Nothing in the registry package uses a shortest-path type except
`ShortestPathBridge`, so plugins compile against Kraken types only.

```java
@Inject
private TransportRegistry transports;

private TransportRegistry.Registration templeEntrance;

@Override
protected void startUp() {
    templeEntrance = transports.register(TransportDefinition
            .object("my-plugin:temple-entrance", origin, destination, objectId, "Enter")
            .requirements(DefinitionRequirements.builder()
                    .membersOnly(true)
                    .quest(Quest.CHILDREN_OF_THE_SUN)
                    .item(DefinitionRequirements.RequiredItem.of(ItemID.COINS, 10))
                    .build())
            .build());
}

@Override
protected void shutDown() {
    templeEntrance.close();
}
```

Close the handle in `shutDown()`. A definition can hold a lambda, and the registry would otherwise
keep the plugin's classes alive after it stops. `unregisterAll("my-plugin")` removes every
definition whose id starts with `my-plugin:`.

### What a definition says

| Field | Meaning |
| --- | --- |
| `id` | Stable key written `owner:name`. Registering the same id again replaces the definition. |
| `origin`, `destination` | The tile the transport is entered from and the tile it leads to. A definition with no origin is a teleport usable from anywhere. |
| `category` | Which planning option turns it on and off. `BOAT` follows `useBoats`. `GENERAL` is always planned, and is the default for everything except an item teleport, a spell and a minigame teleport, which take the category of their shape. |
| `shape` | How it is operated, when no handler is given. |
| `menuOption`, `menuTarget`, `entityId` | What to click. The id is tried first, then the name. For an item teleport `menuOption` is the action on the item. |
| `displayInfo` | The destination to choose: a chat option, a numbered hub entry, a fairy ring code, or the stop an item lists. For a spell it is the spell's name. |
| `itemIds` | The items that carry an item teleport. Any one of them works, worn or carried. |
| `maxWildernessLevel` | The deepest wilderness level a teleport usable from anywhere works at. Defaults to 20. |
| `duration` | Game ticks. It is the planning cost and sets the arrival timeout. Defaults to 1. |
| `bidirectional` | Also usable from the destination back to the origin. |
| `replaces` | Removes the dataset's transports between the same two tiles from every search, including searches where this definition's requirements are not met. |
| `requirements` | Skill levels, quests, items, varbits, varplayers and members only. |
| `availableWhen` | An extra condition for what requirements cannot express. It runs on the client thread at every search, so keep it quick. |
| `handler` | A `TransportHandler` that operates this transport in place of the handler for its shape. |

`TransportDefinition.object`, `npc`, `npcDialogue`, `itemTeleport` and `spellTeleport` start a
builder for the common cases. A definition without a handler is declarative: it reuses the handler
for its shape, and a click goes through the same `TransportEntityResolver` the dataset rows use.
`SINGLE_CLICK`, `CLEAR_THEN_WALK`, `CLICK_THEN_DIALOGUE`, `HUB_DIALOGUE`, `HUB_RESUME_PAUSE`,
`FAIRY_RING`, `ITEM_SUBOP` and `SPELL` work that way. The handlers for the other shapes read detail only the
dataset carries, so a definition with one of those shapes needs a handler. A fairy ring defined
this way does not get the staff or diary rule the dataset's rings get; state it in the definition's
requirements or `availableWhen`.

A handler blocks off the client thread, like the built in ones, and only operates the transport.
Arrival is judged by `TransportExecutor` the same way for every transport. A handler that cannot
operate its transport returns false, and can say why first with `context.fail("...")`: the walk
then ends with `TRANSPORT_FAILED` and that reason. A registered handler that throws fails that one
crossing with `TRANSPORT_FAILED` and the exception in the reason. A stopping script still unwinds
through it.

```java
transports.register(TransportDefinition.builder()
        .id("my-plugin:rope-swing")
        .origin(origin)
        .destination(destination)
        .handler(context -> context.getCtx().tileObjects().withId(objectId).interact("Swing-on"))
        .build());
```

### Teleports usable from anywhere

A definition with no origin is a teleport the player can use wherever they stand. The planner
considers it from every tile of a search, the way it considers the dataset's tablets, jewellery and
spells, and stops considering it above the definition's `maxWildernessLevel`.

```java
// A tablet: holding it is the requirement, and Break is one of the actions the handler tries.
transports.register(TransportDefinition
        .itemTeleport("my-plugin:varrock-tablet", new WorldPoint(3213, 3424, 0), ItemID.POH_TABLET_VARROCKTELEPORT)
        .build());

// Jewellery: every charge level is listed, the stop is chosen behind Rub, and it works up to level 30.
transports.register(TransportDefinition
        .itemTeleport("my-plugin:glory-edgeville", new WorldPoint(3087, 3496, 0), 1712, 1710, 1708, 1706)
        .menuOption("Rub")
        .displayInfo("Edgeville")
        .maxWildernessLevel(30)
        .build());

// A spell, named as the spellbook shows it.
transports.register(TransportDefinition
        .spellTeleport("my-plugin:varrock-spell", new WorldPoint(3213, 3424, 0), "Varrock Teleport")
        .build());
```

An item teleport requires one of its `itemIds` to be worn or carried, without that being repeated in
its requirements. `ItemTeleportHandler` clicks the stop when the item's live menu offers it
directly, as worn jewellery does, and otherwise tries `menuOption` and then the usual teleport
actions (Rub, Teleport, Operate, Break, Invoke, Play), choosing the stop from the sub menu or the
conversation that follows.

A spell teleport states no runes, level or spellbook. It is planned only while the client reports
the spell as castable (`MagicService.castRefusal`), which already accounts for staves, tomes, the
rune pouch and combination runes, and the walker asks again before it casts. The name has to match
a spell in the `Standard`, `Ancient`, `Lunar` or `Arceuus` enums.

A teleport usable from anywhere cannot be `bidirectional` and cannot `replace` a dataset
transport, since both need an origin tile. While the player is aboard a boat at sea no teleport is
planned, which is the rule the planner applies to its own. A shape that clicks something in the
scene needs an origin; without one, give the definition `ITEM_SUBOP`, `SPELL`, `GROUPING_TELEPORT`
(with a handler registered for that shape) or a handler of its own.

### Requirements are checked twice

When a search captures state, the registry drops every definition whose category is off, whose
requirements are not met, or whose `availableWhen` is false. Player state is read once for all
definitions. The walker checks the same requirements again just before it operates the transport
and ends with `TRANSPORT_REQUIREMENTS_UNMET` naming what is missing. For a teleport usable from
anywhere that second check includes the wilderness level the player now stands at. A condition that
throws counts as false, so one plugin's fault cannot stop every walk.

### Handlers for a whole shape

`registerHandler(shape, handler)` replaces the built in handler for a shape. That is how canoes or
minigame teleports can be supplied without changing the API: with a handler registered for `CANOE`,
a route through a canoe no longer ends with `TRANSPORT_UNSUPPORTED`. The latest registration wins,
and closing its handle brings back the one before it.

### Hiding a dataset transport

`disableEdge(origin, destination)` removes the dataset's transports from one tile to another from
every search until its handle is closed. Registered definitions between those tiles are not
affected. Use `replaces` on a definition when the point is to take an edge over.

### Transports bundled with the API

`src/main/resources/walker/transports.json` ships inside the jar and is read through the class
loader when the registry is created. It is a JSON array of declarative definitions:

```json
[
  {
    "id": "kraken:example-entrance",
    "origin": {"x": 1312, "y": 3685, "plane": 0},
    "destination": {"x": 1312, "y": 10086, "plane": 0},
    "category": "GENERAL",
    "shape": "SINGLE_CLICK",
    "menuOption": "Enter",
    "menuTarget": "Lizard dwelling",
    "entityId": 34405,
    "duration": 1,
    "bidirectional": false,
    "replaces": false,
    "requirements": {
      "membersOnly": true,
      "skills": {"AGILITY": 50},
      "quests": ["CHILDREN_OF_THE_SUN"],
      "items": [{"anyOf": [995], "quantity": 10}],
      "varbits": [{"id": 100, "comparison": "AT_LEAST", "value": 7}],
      "varPlayers": []
    }
  }
]
```

Only `id`, `origin`, `destination`, `shape` and something to click are required. A row without an
`origin` is a teleport usable from anywhere, and names its `itemIds` or, for a spell, its
`displayInfo`:

```json
{
  "id": "kraken:example-glory-edgeville",
  "destination": {"x": 3087, "y": 3496, "plane": 0},
  "shape": "ITEM_SUBOP",
  "itemIds": [1712, 1710, 1708, 1706],
  "menuOption": "Rub",
  "displayInfo": "Edgeville",
  "maxWildernessLevel": 30,
  "duration": 4
}
```

The file is read strictly, because reading around a spelling mistake would load a gated transport
without its gate. A row is logged and skipped when it has a key the loader does not know, a
category, shape, skill, quest or comparison that is not a known name, a missing required field, or
a shape that needs a handler (a file cannot carry one). Each row is judged on its own, so the rest
still load. `BundledTransportsTest` fails the build when any row of the shipped file is skipped or
two rows share an id. The two rows above are examples of the format, not shipped data.

A plugin can keep its own transports in a file of the same format. `BundledTransports.parse(json)`
reads such a file into definitions to register, and `BundledTransports.toJson(definition)` writes a
definition as one row, leaving out whatever the loader fills in by default.

```java
String json = new String(getClass().getResourceAsStream("/my-transports.json").readAllBytes(), StandardCharsets.UTF_8);
for (TransportDefinition definition : BundledTransports.parse(json)) {
    registrations.add(transports.register(definition));
}
```

The Transports tool of the Kraken Developer Tools plugin writes such rows. It records a transport
while you use it by hand (the object or NPC, the menu action, both tiles and the duration), lets
you add the requirements, registers the row in the live registry, walks through it to prove it,
saves it into a file one line per row, and copies it as a `TransportDefinition` builder call. Its
audit checks every registered transport near the player with `TransportEntityResolver.locate`, the
same lookup a crossing uses, and lists the rows whose object or NPC is gone or no longer offers the
action.

#### What the bundled file holds

The file ships with the transports that upstream [shortest-path](https://github.com/Skretzo/shortest-path)
has gained since the fork the walker plans with was pinned, limited to the ones a single click
operates: stairs, ladders, cave entrances, agility shortcuts, portals, and a few teleport items.
Each row was checked against the game cache. For an object, the object has to stand within four
tiles of the origin and offer the menu action, and the row records the id the object is placed
under, which is the id the walker searches the scene for. For a teleport item, the item's menu has
to list the stop or one of the usual teleport actions. None of that proves a row in a live game, so
a walk through a bundled transport can still end in `TRANSPORT_FAILED`.

Those generated rows have ids starting with `kraken:upstream-`. To take only them out until the
client restarts, for every plugin, since the registry is shared:

```java
for (TransportDefinition definition : transports.snapshot().getDefinitions()) {
    if (definition.getId().startsWith("kraken:upstream-")) {
        transports.unregister(definition.getId());
    }
}
```

Every bundled row belongs to the owner `kraken`, so `transports.unregisterAll("kraken")` removes
the whole file, the hand written rows below included. The walker then falls back to what the
dataset says for those: the Al Kharid gate as a free `Open` with no toll check, and each web as one
click that is never walked through.

#### Hand written rows

A few rows are written by hand to correct single transports of the dataset. Each sets `replaces`,
so the dataset's own edge between the same two tiles is hidden from every search.

**The Al Kharid toll gate.** The dataset lists the gate's four edges as `Open Gate 44050` and
`44051` with no coins, quest or varp, while the live objects are `44598` and `44599` and charge
`Pay-toll(10gp)` until varp 273 (the Prince Ali Rescue progress varp) reaches 100 or the quest is
finished. Three bidirectional rows per gate state the rule (ids
`kraken:al-kharid-gate-<south|north>-<toll|paid|quest>`):

| Row | Clicks | Requires |
| --- | --- | --- |
| `toll` | `Pay-toll(10gp)` | 10 coins carried, and varp 273 at most 99 |
| `paid` | `Open` | varp 273 at least 100 |
| `quest` | `Open` | Prince Ali Rescue finished |

A player who can neither pay nor pass for free has no usable row and no dataset edge, so the
planner routes around the gate. The toll rows come first in the file: if a toll row and a quest
row are ever usable together, the later registration is the one operated, which is the free
`Open`.

**The webs.** The dataset's fourteen `Slash Web 733` edges are seven bidirectional
`CLEAR_THEN_WALK` rows (ids `kraken:web-<origin>-to-<destination>`).

`HandWrittenTransportsTest` reads the pinned dataset and fails when one of those dataset edges
moves, disappears, gains its own requirements, or when the dataset gains a web that has no row.
That is the signal to update or delete the row.

`scripts/update_walker_transports.py` regenerates those rows and says why it leaves the others
out: hub networks, boats and anything that needs a conversation, objects that offer several
destinations, obstacles that are cleared instead of crossed (rockfalls, locks, levers),
requirements the format cannot state, rows upstream marks as unconfirmed, and rows the cache does
not confirm. Its rows have ids starting with `kraken:upstream-`. A row with any other id is kept as
it is when the script runs again, ahead of the generated rows and in its existing order, so rows
written by hand belong under a different prefix. The cleared obstacles could be written as
`CLEAR_THEN_WALK` rows now, but the script still leaves them out: only the webs have been operated
that way.

```bash
# 1. dump the game cache with RuneLite's cache library (see the header of the Java file)
java -Xmx3g -cp "libs/*" scripts/WalkerCacheDump.java cache-copy cache-dump
# 2. rewrite the generated rows from a checkout of upstream shortest-path
python3 scripts/update_walker_transports.py ../shortest-path --cache-dump cache-dump
# 3. every written row has to load
./gradlew test --tests '*BundledTransportsTest'
```

### How a registered transport reaches a route

`GlobalPathfinder` asks `ShortestPathBridge` to apply the registry after the search's own
configuration has been refreshed. The bridge takes one immutable
snapshot of the registry, removes the hidden dataset edges, and adds an edge for each usable
definition ahead of whatever else leaves its origin tile. A definition without an origin has no
tile to be filed under. It is compiled as a teleport and handed to the search's own configuration
(`RequestPathfinderConfig`), which offers it beside the dataset's teleports, registered ones first.
A registration made while a search is running cannot change that search.
`TransportUsage.getDefinition()` returns the definition behind a route step, or null for a dataset
edge. For a teleport usable from anywhere the step's type is a teleport type and its origin is the
tile the route leaves from.

When several transports join the same two tiles, a registered one is operated in preference to the
dataset's, and the latest registration in preference to an earlier one. A transport that is
unregistered after a route was planned through it is not operated: the walk ends with
`TRANSPORT_FAILED` saying so, and the next walk plans without it.

One limit applies today. A registered transport's requirements are checked against what the
player carries and wears, so `includeBankPath` does not plan a bank visit to fetch an item for it.

## Doors the planner does not know about

The transport dataset lists roughly two hundred doors for the whole game — the ones that *gate* a
route. Every other closed door is just a wall to the planner: it routes around one where it can, and
never reports it, so there is nothing for the walker to highlight or click. The result is a walk that
quietly stops getting anywhere.

`ObstacleRecovery` handles this, and it works ahead of the walk rather than after it fails. Before a
leg is traversed, every waypoint on the scene-clipped path is tested with
`TileService.isTileReachable`, which floods the **live scene** rather than the shipped collision map
— so the client already knows the gate is shut. Checking only a short prefix would miss a gate near
the destination, and the movement primitive would then click through it and time out. The leg is cut
short at the first unreachable waypoint. If a door or gate stands on that tile (or the previous path
tile) and the player is within ten tiles, it is clicked from here — the client walks them to it. A
compressed path can skip from the player's tile to a bank fifty tiles across a river: that is not a
closed door, so the walker steps toward it instead of aborting. Opening is not the end of the leg:
reachability is re-checked and the remainder is walked in the same call, so a courtyard door does
not leave the player standing on the near side while the stall budget runs out.

That ordering is the whole point. Waiting for movement to fail costs a full retry cycle — about
twenty seconds of walking into a wall — before anything is learned.

Four rules keep it from misfiring:

- **Nothing happens while the player is moving.** A tile that is unreachable from here is often
  reachable a few steps along, and acting on that reading would click a door already being walked
  through.
- **The player must be close enough to click, or next to a blockage with nothing to open.** An
  openable door within ten tiles is clicked from here. A far unreachable waypoint with no door is
  walked toward — a reachable prefix, or a closer reachable tile on this side of whatever is in the
  way. Failing to find a door on the far bank must not abort the walk.
- **The obstacle must stand on the blocked tile itself or the previous tile on the path.** A door
  occupies one of the two tiles it separates, so those are the only two worth checking. Being strict
  is what makes an over-eager reachability reading harmless: if nothing openable is exactly there,
  nothing happens.
- **The action is chosen by name**, preferring "Open" over "Climb-over" on a gate that offers both. A
  locked door offering only "Pick-lock" is reported as no way through rather than guessed at.

`traversePath` returning false is kept as a backstop, for a door that shuts mid-leg. If an adjacent
obstruction cannot be cleared, the walk ends with `STALLED` rather than burning stall rounds. The
log and `WalkResult.getReason()` name the unreachable tile, the object (name and id), and why it
could not be opened — nothing matching door/gate/curtain, no open action, the click did not
dispatch, or the tile never became reachable. A far unreachable waypoint does not produce that
result; the walker walks toward it instead.

This follows VitaLite's division of labour — its dataset covers planning-relevant doors and
`WalkerPath.handlePassThroughObjects` handles the rest at runtime — including its proactive
reachability check and its guard against acting while moving. It differs deliberately in one place:
VitaLite clicks menu action index 0 whatever that happens to be, where this chooses by name and
refuses a door it cannot open.

## How a crossing is judged

After a handler dispatches a click or a destination choice, `TransportExecutor` waits for evidence
the player got through. Standing still on the origin is not arrival, even when the destination is
the next tile — dataset doors are often one tile apart, and treating that proximity as success is
what would make a closed door look like a crossing. A door another player already opened is the
other way around: the destination is the next tile and already reachable, so the executor returns
without clicking. The open door has a different object id and no "Open" action, and operating it
would fail. Reachable somewhere in the scene is not enough — the Varrock underwall's far side is
reachable by walking around, and skipping Climb-into is what stalled on the tunnel origin. The same
flood must not end the wait after the click: that is what clicked Climb-into again while the first
climb was still playing, and Cross again while walking the last tile to the wilderness ditch.
Approach walking stops on the tile before the origin: that tile is often the object itself (the
wilderness ditch), and Walk-here never stands on it. The fire radius of two tiles is enough to
operate from beside it. Dest walking uses the walker's tolerance (three tiles by default) so a
one-tile miss of the Set tile is not a MovementService retry. After a shortcut the walker waits
until the player is idle before the next walk click, and the executor waits idle again before
operating a transport. A jewellery landing on the same island is also reachable; that is not an
open door. The walker walks the path instead of rubbing the glory. Any of these counts as a real
crossing:

- the destination is the next tile and already reachable in the live scene (an open door)
- the player left the origin, is within three tiles of the destination on the same plane, **and**
  that destination is reachable from where they stand (the far bank of a ditch, not this bank)
- the player's plane changed
- the player moved more than sixteen tiles (a teleport or hub ride)
- the destination is the next tile and became reachable after the click (a door that opened underfoot)

The walker then re-plans from wherever they actually are.

## Requirements

Before operating a transport the walker re-checks its requirements against live state
(`TransportRequirements`, `PlayerStateReader`). The planner already filters by requirement, but
state drifts between planning and arrival — a fare gets spent, a tab gets used — and re-checking
turns a silent failure into `TRANSPORT_REQUIREMENTS_UNMET` naming what is missing.

The Al Kharid gate is not in the dataset as a fare. Bundled registry rows supply that rule (see
[Hand written rows](#hand-written-rows)): they hide the dataset's four edges, are planned only when
the player carries 10 coins or the gate is free, and click the live objects, `Pay-toll(10gp)` on
`44598`/`44599` while it charges and `Open` on those same ids once it is free. The shortest-path TSV
still says `Open Gate 44050`. `chooseAction` also falls back to `Pay-toll(10gp)` if an `Open` meets
the live menu, which only matters once the bundled rows have been unregistered. A stale route with
no coins still aborts with `TRANSPORT_REQUIREMENTS_UNMET`.

Wilderness is the planner's 0 / 20 / 30 / 31 bucket (`WildernessChecker`), not a y-only level.
Lunar Isle and Wintertodt sit north of y 3520 and are not wilderness; treating them as deep wildy
would refuse teleports the planner had already routed.

One requirement is not in the dataset at all: fairy rings need a dramen or lunar staff unless the
Lumbridge elite diary is complete. The pathfinder hardcodes that in its own config, so
`TransportRequirements` applies the same rule, or a fairy ring route would pass the check and then
fail at the ring.

## Related

- `TileObjectQuery` (`ctx.tileObjects()`) was added for this: `GameObjectQuery` reads only
  `tile.getGameObjects()`, and a large share of doors and gates are `WallObject`s.
- `SceneWindow` holds the scene-clipping arithmetic that lets a short-range click cover a long route.
