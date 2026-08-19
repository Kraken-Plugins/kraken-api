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
so `AlKharidGate` overlays VitaLite's 10gp / varp 273 / Prince Ali Rescue rule before search and
again at click time.

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

Both fail immediately with `TRANSPORT_UNSUPPORTED` and a reason. `WalkerConfig` turns `useCanoes`
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

The Al Kharid gate is not in the dataset as a fare. `AlKharidGate` supplies that rule at plan time
(dropping the four edges when the player has neither 10 coins nor a free gate) and rewrites the
click to the live objects: `Pay-toll(10gp)` on `44598`/`44599` while it charges, `Open` on those
same ids once it is free. The shortest-path TSV still says `Open Gate 44050`. Execute uses the
rewritten string, and `chooseAction` also falls back to `Pay-toll(10gp)` if a leftover `Open` meets
the live menu. A stale route with no coins still aborts with `TRANSPORT_REQUIREMENTS_UNMET`.

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
