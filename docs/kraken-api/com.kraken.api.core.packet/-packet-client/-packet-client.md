//[kraken-api](../../../index.md)/[com.kraken.api.core.packet](../index.md)/[PacketClient](index.md)/[PacketClient](-packet-client.md)

# PacketClient

[Kraken API]\
constructor(client: Client)

Creates a new PacketSender. This constructor initializes packet queueing functionality by either loading the client packet sending method from the cached json file or running an analysis on the RuneLite injected client to determine the packet sending method.

#### Parameters

Kraken API

| | |
|---|---|
| client | The RuneLite Client instance. |
