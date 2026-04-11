//[kraken-api](../../../index.md)/[com.kraken.api](../index.md)/[Context](index.md)

# Context

[Kraken API]\
open class [Context](index.md)

## Constructors

| | |
|---|---|
| [Context](-context.md) | [Kraken API]<br>constructor(client: Client, clientThread: ClientThread, mouse: [VirtualMouse](../../com.kraken.api.input.mouse/-virtual-mouse/index.md), eventBus: EventBus, itemManager: ItemManager, bankService: [BankService](../../com.kraken.api.service.bank/-bank-service/index.md), packetInterceptor: [PacketInterceptor](../../com.kraken.api.core.interceptor/-packet-interceptor/index.md), interactionManager: [InteractionManager](../../com.kraken.api.core.interaction/-interaction-manager/index.md)) |

## Functions

| Name | Summary |
|---|---|
| [bank](bank.md) | [Kraken API]<br>open fun [bank](bank.md)(): [BankQuery](../../com.kraken.api.query.container.bank/-bank-query/index.md)<br>Creates a new query builder for the Bank interface. |
| [bankInventory](bank-inventory.md) | [Kraken API]<br>open fun [bankInventory](bank-inventory.md)(): [BankInventoryQuery](../../com.kraken.api.query.container.bank/-bank-inventory-query/index.md)<br>Creates a new query builder for a Bank Inventory. |
| [depositBox](deposit-box.md) | [Kraken API]<br>open fun [depositBox](deposit-box.md)(): [DepositBoxQuery](../../com.kraken.api.query.container.bank/-deposit-box-query/index.md)<br>Creates a new query builder for the Deposit box. |
| [equipment](equipment.md) | [Kraken API]<br>open fun [equipment](equipment.md)(): [EquipmentQuery](../../com.kraken.api.query.equipment/-equipment-query/index.md)<br>Creates a new query builder for the equipment interface. |
| [gameObjects](game-objects.md) | [Kraken API]<br>open fun [gameObjects](game-objects.md)(): [GameObjectQuery](../../com.kraken.api.query.gameobject/-game-object-query/index.md)<br>Creates a new query builder for game objects. |
| [getEnum](get-enum.md) | [Kraken API]<br>open fun [getEnum](get-enum.md)(enumId: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): EnumComposition<br>Retrieves an enum composition from the RuneLite client thread. |
| [getService](get-service.md) | [Kraken API]<br>open fun &lt;[T](get-service.md)&gt; [getService](get-service.md)(serviceClass: [Class](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/Class.html)&lt;[T](get-service.md)&gt;): [T](get-service.md)<br>Retrieves an instance of a specified service class. |
| [getVarbitValue](get-varbit-value.md) | [Kraken API]<br>open fun [getVarbitValue](get-varbit-value.md)(varbit: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)<br>Returns a varbit value from the RuneLite client. |
| [getVarpValue](get-varp-value.md) | [Kraken API]<br>open fun [getVarpValue](get-varp-value.md)(varp: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)<br>Returns a var player value from the RuneLite client. |
| [getWidget](get-widget.md) | [Kraken API]<br>open fun [getWidget](get-widget.md)(widgetId: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): Widget<br>Retrieves a Widget from the RuneLite client. |
| [groundItems](ground-items.md) | [Kraken API]<br>open fun [groundItems](ground-items.md)(): [GroundObjectQuery](../../com.kraken.api.query.groundobject/-ground-object-query/index.md)<br>Creates a new query builder for Ground Items. |
| [initializeInterceptors](initialize-interceptors.md) | [Kraken API]<br>open fun [initializeInterceptors](initialize-interceptors.md)()<br>Initializes all supported runtime interceptors using the default configuration.<br>[Kraken API]<br>open fun [initializeInterceptors](initialize-interceptors.md)(configuration: [InterceptorBuilder](../../com.kraken.api.core.interceptor/-interceptor-builder/index.md))<br>Initializes the configured runtime interceptors. |
| [initializePackets](initialize-packets.md) | [Kraken API]<br>open fun [initializePackets](initialize-packets.md)()<br>Initializes packet queueing functionality by either loading the client packet sending method from the cached json file or running an analysis on the RuneLite injected client to determine the packet sending method. |
| [inventory](inventory.md) | [Kraken API]<br>open fun [inventory](inventory.md)(): [InventoryQuery](../../com.kraken.api.query.container.inventory/-inventory-query/index.md)<br>Creates a new query builder for the standard Backpack Inventory. |
| [npcs](npcs.md) | [Kraken API]<br>open fun [npcs](npcs.md)(): [NpcQuery](../../com.kraken.api.query.npc/-npc-query/index.md)<br>Creates a new query builder for NPCs. |
| [players](players.md) | [Kraken API]<br>open fun [players](players.md)(): [PlayerQuery](../../com.kraken.api.query.player/-player-query/index.md)<br>Creates a new query builder for Players. |
| [runOnClientThread](run-on-client-thread.md) | [Kraken API]<br>open fun [runOnClientThread](run-on-client-thread.md)(method: [Runnable](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/Runnable.html))<br>Runs a method on the client thread without returning a result.<br>[Kraken API]<br>open fun &lt;[T](run-on-client-thread.md)&gt; [runOnClientThread](run-on-client-thread.md)(method: [Callable](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/concurrent/Callable.html)&lt;[T](run-on-client-thread.md)&gt;): [T](run-on-client-thread.md)<br>Run a method on the client thread, returning the result directly. |
| [runOnClientThreadOptional](run-on-client-thread-optional.md) | [Kraken API]<br>open fun &lt;[T](run-on-client-thread-optional.md)&gt; [runOnClientThreadOptional](run-on-client-thread-optional.md)(method: [Callable](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/concurrent/Callable.html)&lt;[T](run-on-client-thread-optional.md)&gt;): [Optional](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/Optional.html)&lt;[T](run-on-client-thread-optional.md)&gt;<br>Run a method on the client thread, returning an optional of the result. |
| [runScript](run-script.md) | [Kraken API]<br>open fun [runScript](run-script.md)(id: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html))<br>Wraps the RuneLite client's run script method scheduling the run on the client thread. |
| [widgets](widgets.md) | [Kraken API]<br>open fun [widgets](widgets.md)(): [WidgetQuery](../../com.kraken.api.query.widget/-widget-query/index.md)<br>Creates a new query builder for Widgets. |
| [worlds](worlds.md) | [Kraken API]<br>open fun [worlds](worlds.md)(): [WorldQuery](../../com.kraken.api.query.world/-world-query/index.md)<br>Creates a new query builder for Worlds. |
