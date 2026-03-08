# Interaction

The Kraken API interacts with the RuneLite game client purely through network packets. Network packets allow your client
to communicate with Jagex's servers to process, validate, and persist what you are doing in the game.

Packets must be instantiated once within the client before they can be used. Instantiate packets by calling:

```java
@Inject
private Context ctx;

@Override
public void startUp() {
  ctx.initializePackets();
}
```

## Interceptor Pattern

Before diving into the packet system, it's important to understand the interceptor pattern and how it mutates the game client at runtime.
The interceptor pattern is a design pattern that allows you to intercept and modify the behavior of a method call. There are several interceptors
defined in the `com.kraken.api.core.interceptor` package. These interceptors are used to modify the behavior of the game client at runtime and include
functionality like:

- Hooking into when a packet is sent to the server exposing a RuneLite event for `@Subscribe`'ing to outgoing packets
- Patching methods which load mouse hook detection tools (e.g. capturing when remote inputs from Parsec, TeamViewer, etc... are used to make clicks instead of your computer)
- Other interceptors, which may be added in the future

The Kraken API stands on a premise to **not** modify the game client to run correctly, so calling out these modifications is important so that users are aware and understand
what is happening to enable certain API functionality.

The API gives you the option on which interceptors you would like to load for your plugins. For example,

```java
@Inject
private Context ctx;

@Override
public void startUp() {
    // Load only the mouse hook DLL patch not packet interception.
    context.initializeInterceptors(
            InterceptorBuilder.builder()
                    .withPacketInterceptor(false)
                    .withMouseHookInterceptor(true)
                    .build()
    );
}
```

When `.initializeInterceptors()` is called the interceptors will run patching the runtime classes defined in the `reflection_hooks.json` file, mutating the game client
to enable the functionality you have requested.

## Packet System Overview

The packet system is designed to bypass the need for simulating mouse clicks and keyboard presses, offering a more direct and reliable way to interact with the game. It consists of several key components working together:

1.  **Packet Definitions (`PacketDefinition`)**: These define the structure of each packet type, including the packet's name, the data fields it contains, the methods used to write that data, and the associated `PacketType`.
2.  **Packet Types (`PacketType`)**: An enumeration of the various packet types sent by the game client (e.g., `OPNPC` for NPC interactions, `OPLOC` for object interactions, `IF_BUTTON` for interface buttons). Each type knows the list of parameters it requires.
3.  **Packet Definition Factory (`PacketDefFactory`)**: A factory class that creates and caches `PacketDefinition` instances for all supported packet types. It handles the mapping between high-level packet types and their specific implementations (e.g., `OPOBJ1` vs `OPOBJ2`).
4.  **Packet Client (`PacketClient`)**: The core component responsible for constructing and sending packets. It uses reflection to access internal game client methods and fields, ensuring that packets are formatted correctly and queued for transmission.
5.  **Entity Packet Helpers (`com.kraken.api.core.packet.entity`)**: High-level utility classes (like `NPCPackets`, `WidgetPackets`) that simplify the process of sending packets for specific game entities. These classes are further abstracted by the Query and Service system defined in the [API docs](docs/API.md).

## How Packets are Sent

The process of sending a packet involves the following steps:

1.  **Identify the Action**: The user (or a high-level API) determines the desired action (e.g., "Attack Goblin").
2.  **Determine Packet Type**: The system identifies the appropriate `PacketType` for the action (e.g., `OPNPC` for attacking an NPC).
3.  **Retrieve Definition**: The `PacketDefFactory` provides the `PacketDefinition` for the specific packet type and action index.
4.  **Prepare Data**: The necessary data (e.g., NPC index, item ID, widget ID) is collected.
5.  **Send Packet**: The `PacketClient` is invoked with the `PacketDefinition` and the data.
    *   It uses reflection to create a `PacketBufferNode`.
    *   It writes the data into the packet's buffer using the methods specified in the definition.
    *   It queues the packet to the client's `PacketWriter` to be sent to the server.

## Key Components

### PacketClient

The `PacketClient` is a singleton that handles the low-level details of packet construction and transmission. It:
*   Initializes by locating the necessary internal client methods (via `PacketMethodLocator`).
*   Provides the `sendPacket(PacketDefinition def, Object... objects)` method, which is the entry point for sending any packet.
*   Handles the obfuscation and reflection required to interact with the RuneLite client's internals.

### PacketType

The `PacketType` enum categorizes the different kinds of interactions. Some common types include:
*   `OPNPC`: interacting with NPCs (Attack, Talk-to, etc.).
*   `OPLOC`: Interacting with Game Objects (Open door, Mine rock, etc.).
*   `OPOBJ`: Interacting with Ground Items (Take, Examine).
*   `OPPLAYER`: Interacting with other Players (Trade, Follow).
*   `IF_BUTTON`: Clicking on Interface buttons (Inventory, Spellbook, etc.).
*   `MOVE_GAMECLICK`: Walking to a specific tile.

### Entity Packet Helpers

To make interaction easier, the API provides helper classes for specific entities. For example, `NPCPackets` provides methods like:
*   `queueNPCAction(int actionFieldNo, int npcIndex, boolean ctrlDown)`: Sends a raw action packet.
*   `queueNPCAction(NPC npc, String... actionList)`: Finds the correct action index for a given action name (e.g., "Attack") and sends the packet.
*   `queueWidgetOnNPC(NPC npc, Widget widget)`: Sends a packet to use an item/widget on an NPC.

## Example: Attacking an NPC

When you call `ctx.npcs().withName("Goblin").interact("Attack")`, the following happens internally:

1.  The `NpcQuery` finds the "Goblin" NPC.
2.  The `interact("Attack")` method on the `NpcEntity` is called.
3.  This delegates to `NPCPackets.queueNPCAction(npc, "Attack")`.
4.  `NPCPackets` looks up the "Attack" action in the NPC's composition to find its index (e.g., action 1).
5.  It calls `packetClient.sendPacket()` with the `OPNPC1` definition and the NPC's index.
6.  `PacketClient` constructs the packet and queues it to be sent to the server.
