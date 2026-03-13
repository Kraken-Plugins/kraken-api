//[kraken-api](../../../index.md)/[com.kraken.api.core.packet](../index.md)/[PacketMethodLocator](index.md)/[initialize](initialize.md)

# initialize

[Kraken API]\
open fun [initialize](initialize.md)(client: Client)

Initializes the packet method locator. This is the main entry point. It will attempt to load from the cached JSON file {runelite version}-{client revision}.json or perform a full client analysis if no valid cache is found.

#### Parameters

Kraken API

| | |
|---|---|
| client | The RuneLite Client instance. |
