//[kraken-api](../../../index.md)/[com.kraken.api.core.packet.model](../index.md)/[PacketFactory](index.md)

# PacketFactory

[Kraken API]\
open class [PacketFactory](index.md)

A factory class for creating and managing [PacketDefinition](../-packet-definition/index.md) instances for various packet types and actions. This class initializes packet definitions by fetching and parsing a JSON configuration file locally first, then falling back to a remote URL.

## Constructors

| | |
|---|---|
| [PacketFactory](-packet-factory.md) | [Kraken API]<br>constructor() |

## Functions

| Name | Summary |
|---|---|
| [getEventMouseClick](get-event-mouse-click.md) | [Kraken API]<br>open fun [getEventMouseClick](get-event-mouse-click.md)(): [PacketDefinition](../-packet-definition/index.md) |
| [getMoveGameClick](get-move-game-click.md) | [Kraken API]<br>open fun [getMoveGameClick](get-move-game-click.md)(): [PacketDefinition](../-packet-definition/index.md) |
| [getResumeCountDialog](get-resume-count-dialog.md) | [Kraken API]<br>open fun [getResumeCountDialog](get-resume-count-dialog.md)(): [PacketDefinition](../-packet-definition/index.md) |
| [getResumeObjDialog](get-resume-obj-dialog.md) | [Kraken API]<br>open fun [getResumeObjDialog](get-resume-obj-dialog.md)(): [PacketDefinition](../-packet-definition/index.md) |
| [getStringDialog](get-string-dialog.md) | [Kraken API]<br>open fun [getStringDialog](get-string-dialog.md)(): [PacketDefinition](../-packet-definition/index.md) |
| [init](init.md) | [Kraken API]<br>open fun [init](init.md)()<br>Initializes the packet factory by loading packet definitions from local resources or a remote source. |
