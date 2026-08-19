# Walker

`Walker` (`com.kraken.api.service.walker.Walker`) walks the player anywhere in the game world,
operating whatever doors, boats, shortcuts and teleports the route needs on the way.

It composes the two pieces that already existed rather than replacing either: `GlobalPathfinder`
plans a route, `MovementService` sends the clicks, and the walker runs the loop between them —
plan, walk the part of the route the client has loaded, operate the transport at the end of it,
re-plan from wherever the player ended up.

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
operate is kept out of a route rather than failing mid-walk.

## Threading

Every call blocks and must be made off the client thread. Waiting is a no-op on the client thread,
which would turn the loop into a spin that fails in milliseconds, so the walker refuses to run there
and returns `CALLED_ON_CLIENT_THREAD`.

The walker also refuses to plan from inside an instance, because instanced coordinates do not
correspond to the static collision map.

## Results

`WalkResult` says what happened rather than returning a bare boolean, because "did not arrive" has
several meaningfully different causes:

| Outcome | Meaning |
| --- | --- |
| `SUCCESS` | The player reached the destination. |
| `NO_ROUTE` | The pathfinder found no route. |
| `TRANSPORT_REQUIREMENTS_UNMET` | A transport needs an item, level, quest or world state the player lacks. The reason names it. |
| `TRANSPORT_UNSUPPORTED` | The route needs a transport kind the walker cannot yet operate. |
| `TRANSPORT_FAILED` | A transport was attempted but the player did not get through. |
| `STALLED` | The player stopped making progress. |
| `TIMED_OUT` | The time or round budget ran out. |
| `IN_INSTANCE`, `CALLED_ON_CLIENT_THREAD`, `UNKNOWN_POSITION` | Refused before starting. |

## How transports are executed

The transport dataset ships with the pinned `shortest-path` dependency and describes twenty six
kinds of transport. They are operated in far fewer ways, so dispatch is by *shape*
(`TransportShapes`), not by type:

| Shape | Types | Implemented |
| --- | --- | --- |
| `SINGLE_CLICK` | doors, gates, stairs, agility and grapple shortcuts, levers, portals, POH portals, jewellery boxes, ships | yes |
| `CLICK_THEN_DIALOGUE` | boats, charter ships, magic carpets | yes |
| `HUB_DIALOGUE` | spirit trees, minecarts, wilderness obelisks | yes |
| `CLICK_THEN_WIDGET` | gnome gliders, hot air balloons, magic mushtrees, quetzals | yes |
| `FAIRY_RING` | fairy rings | yes |
| `ITEM_SUBOP` | teleport items, quetzal whistle, seasonal transports | yes |
| `SPELL` | teleport spells and home teleports | yes |
| `CANOE` | canoes | **no** |
| `GROUPING_TELEPORT` | minigame teleports | **no** |

`TransportShapesTest` asserts every type maps to a shape, so a dependency bump that adds a transport
kind fails the build instead of failing in-game.

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

**Spirit trees, minecarts and wilderness obelisks** offer their stops as numbered chat options, so
they share `HubDialogueHandler`, which reuses `DialogueService` — chosen by name where the name
matches, by position otherwise.

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

Both fail immediately with `TRANSPORT_UNSUPPORTED` and a reason. Turning `useCanoes` or
`useTeleportationMinigames` off in `GlobalPathfinderConfig` keeps them out of routes entirely, which
is the better option if you would rather route around them than fail.

### Turning two strings into a click

A transport's executable detail is stored as unstructured text under a column headed
`menuOption menuTarget objectID`, for example `Open Door 9398`. Only the trailing integer is
unambiguous: both the option and the target can contain spaces with no delimiter, so
`Al Kharid Amulet of Glory 13523` is the option `Al Kharid` on `Amulet of Glory`, not `Al`.

`ObjectInfo` therefore parses in two stages — extract the id, then re-split the remainder once the
entity has been resolved and its real name is known. `TransportEntityResolver` also compares menu
text ignoring hyphens and spacing, because the dataset writes `Climb Down Ladder` where the client's
action is `Climb-down`.

## Doors the planner does not know about

The transport dataset lists roughly two hundred doors for the whole game — the ones that *gate* a
route. Every other closed door is just a wall to the planner: it routes around one where it can, and
never reports it, so there is nothing for the walker to highlight or click. The result is a walk that
quietly stops getting anywhere.

`ObstacleRecovery` handles this, and it works ahead of the walk rather than after it fails. Before a
leg is traversed, each waypoint is tested with `TileService.isTileReachable`, which floods the **live
scene** rather than the shipped collision map — so the client already knows the gate is shut. The leg
is cut short at the first unreachable waypoint, and whatever stands there is opened before setting
off again.

That ordering is the whole point. Waiting for movement to fail costs a full retry cycle — about
twenty seconds of walking into a wall — before anything is learned.

Three rules keep it from misfiring:

- **Nothing happens while the player is moving.** A tile that is unreachable from here is often
  reachable a few steps along, and acting on that reading would click a door already being walked
  through.
- **The obstacle must stand on the blocked tile itself or under the player.** A door occupies one of
  the two tiles it separates, so those are the only two worth checking. Being strict is what makes an
  over-eager reachability reading harmless: if nothing openable is exactly there, nothing happens.
- **The action is chosen by name**, preferring "Open" over "Climb-over" on a gate that offers both. A
  locked door offering only "Pick-lock" is reported as no way through rather than guessed at.

`traversePath` returning false is kept as a backstop, for a door that shuts mid-leg.

This follows VitaLite's division of labour — its dataset covers planning-relevant doors and
`WalkerPath.handlePassThroughObjects` handles the rest at runtime — including its proactive
reachability check and its guard against acting while moving. It differs deliberately in one place:
VitaLite clicks menu action index 0 whatever that happens to be, where this chooses by name and
refuses a door it cannot open.

## Requirements

Before operating a transport the walker re-checks its requirements against live state
(`TransportRequirements`, `PlayerStateReader`). The planner already filters by requirement, but
state drifts between planning and arrival — a fare gets spent, a tab gets used — and re-checking
turns a silent failure into `TRANSPORT_REQUIREMENTS_UNMET` naming what is missing.

One requirement is not in the dataset at all: fairy rings need a dramen or lunar staff unless the
Lumbridge elite diary is complete. The pathfinder hardcodes that in its own config, so
`TransportRequirements` applies the same rule, or a fairy ring route would pass the check and then
fail at the ring.

## Related

- `TileObjectQuery` (`ctx.tileObjects()`) was added for this: `GameObjectQuery` reads only
  `tile.getGameObjects()`, and a large share of doors and gates are `WallObject`s.
- `SceneWindow` holds the scene-clipping arithmetic that lets a short-range click cover a long route.
