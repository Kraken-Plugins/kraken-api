# Interaction

The Kraken API interacts with the RuneLite game client using reflection to call a method within the client called `doAction`. The `doAction` method is a 
"choke point" in the client for a vast majority of menu action interactions: clicking NPC's, game objects, widgets, ground items, interfaces (again widgets), and more. 

Kraken still utilizes some network packets to directly communicate with Jagex's servers to process certain actions like movement, spoofed mouse clicks, and
some niche dialogue operations. By leveraging `doAction` for the heavy lifting for ~85% of interactions and the network packets for the remaining ~15%, the Kraken API 
takes significantly less time and effort to update when the game obfuscation changes.

Both the `doAction` hooks and the packet definitions come from `hooks.json`, which `HooksLoader` reads once, and everything is prepared when the `Context` class is built by Guice.
There is nothing to initialize in your plugin:

```java
@Inject
private Context ctx;
```

## Interaction Manager

A vast majority of actions happen via the `InteractionManager` (`com.kraken.api.core.interaction`). Typically, as a script developer, you will not need to interact with this class directly
as the interactions themselves are abstracted by the Query and Service system defined in the [API docs](API.md).

The `InteractionManager` exposes a set of overloaded `interact()` methods that accept RuneLite objects (`NPC`, `Player`, `TileObject`, `Widget`, container items,
and widget-on-target combinations). Each call is:

1. Resolved by `MenuActionResolverRegistry`, which picks the resolver for that entity type. The resolver looks the action name up on the entity (an NPC's composition, an object's definition, a widget's actions) and produces a `ResolvedMenuAction`: the menu opcode, identifier, and parameters `doAction` expects.
2. Dispatched by `InteractionDispatcher`, which queues an `EVENT_MOUSE_CLICK` packet at the entity's clickbox so the server sees a click where the action happened, then hands the resolved action to `DoActionInvoker`.
3. Invoked by `DoActionInvoker`, a reflective call into the client's obfuscated `doAction` using the class and method names in `hooks.json`.

The client then validates, constructs, and queues the packet exactly as it would for a real click. `interact(...)` returns `false` when nothing was sent (no
matching action, entity gone, hooks missing), so retrying on a `false` result is safe.

## Example: Attacking an NPC

When you call `ctx.npcs().withName("Goblin").interact("Attack")`, the following happens internally:

1.  The `NpcQuery` finds the "Goblin" NPC.
2.  The `interact("Attack")` method on the `NpcEntity` is called and delegates to the `InteractionManager`.
3.  The NPC resolver finds "Attack" in the NPC's composition to determine its action index and builds the `ResolvedMenuAction` (an `NPC_*_OPTION` opcode with the NPC's index).
4.  `InteractionDispatcher` queues a spoofed mouse click on the NPC's clickbox and `DoActionInvoker` calls `doAction` with the resolved parameters.
5.  The client builds the `OPNPC` packet and queues it to be sent to the server.

## Packet System Overview

The packet system covers the actions that do not go through `doAction`. It consists of several key components working together:

1.  **Packet Definitions (`PacketDefinition`)**: These define the structure of each packet type, including the packet's name, the data fields it contains, the methods used to write that data, and the associated `PacketType`. They are loaded from the `packets` section of `hooks.json`.
2.  **Packet Types (`PacketType`)**: An enumeration of the client packet kinds the API knows the shape of (`OPNPC`, `OPLOC`, `IF_BUTTON`, `MOVE_GAMECLICK`, and so on). Only the five with definitions in `hooks.json` are built and sent by the API: `EVENT_MOUSE_CLICK`, `MOVE_GAMECLICK`, `RESUME_COUNTDIALOG`, `RESUME_OBJDIALOG` and `RESUME_STRINGDIALOG`. Everything else is handled by `doAction`.
3.  **Packet Definition Factory (`PacketFactory`)**: A factory class that creates and caches `PacketDefinition` instances for the supported packet types.
4.  **Packet Client (`PacketClient`)**: The core component responsible for constructing and sending packets. It uses the `reflectionHooks` from `hooks.json` (packet writer, buffer node, `addNode`, Isaac cipher) to access the client's internals so that packets are formatted correctly and queued for transmission.
5.  **Entity Packet Helpers (`com.kraken.api.core.packet.entity`)**: `MousePackets`, `MovementPackets` and `WidgetPackets` wrap the packet client for the specific cases above. These classes are further abstracted by the Query and Service system.

## How Packets are Sent

The process of sending a packet involves the following steps:

1.  **Identify the Action**: The user (or a high-level API) determines the desired action (e.g., "walk to this tile").
2.  **Determine Packet Type**: The system identifies the appropriate `PacketType` for the action (e.g., `MOVE_GAMECLICK`).
3.  **Retrieve Definition**: The `PacketFactory` provides the `PacketDefinition` for the packet type.
4.  **Prepare Data**: The necessary data (e.g., the target coordinates) is collected.
5.  **Send Packet**: The `PacketClient` is invoked with the `PacketDefinition` and the data.
    *   It uses reflection to create a `PacketBufferNode`.
    *   It writes the data into the packet's buffer using the methods specified in the definition.
    *   It queues the packet to the client's `PacketWriter` to be sent to the server.

## Key Parts

### PacketClient

The `PacketClient` is a singleton that handles the low-level details of packet construction and transmission. It:
*   Locates the necessary internal client methods and fields from the mappings `HooksLoader` provides.
*   Provides the `sendPacket(PacketDefinition def, Object... objects)` method, which is the entry point for sending any packet.
*   Handles the obfuscation and reflection required to interact with the RuneLite client's internals.

### Entity Packet Helpers

To make the packet layer easier to use, the API provides helper classes for specific cases. For example, `MovementPackets` provides methods like:
*   `queueMovement(WorldPoint location)`: Finds the correct x and y coordinates and sends the packet to move your player.
*   `queueResumeObj(int itemId)`: Sends a packet to select an object from a list (i.e. selecting an item to buy from the GE)

`MousePackets.queueClickPacket(x, y)` sends the spoofed click that precedes every `doAction` call.

## Learning More

The [client development](https://kraken-plugins.com/docs/client-development/intro.html) section explains what `doAction`, packet buffers and Isaac ciphers are, and how the
mappings in `hooks.json` are found in the obfuscated client. See the [packets guide](https://kraken-plugins.com/docs/client-development/mapping/packets.html) for a table of every
packet type and its parameters.
