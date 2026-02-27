//[lib](../../../index.md)/[com.kraken.api](../index.md)/[Context](index.md)/[initializePackets](initialize-packets.md)

# initializePackets

[Kraken API]\
open fun [initializePackets](initialize-packets.md)()

Initializes packet queueing functionality by either loading the client packet sending method from the cached json file or running an analysis on the RuneLite injected client to determine the packet sending method. 

 This is required to be called before packets can actually be sent i.e. its necessary to know the packet method in the client before calling it with reflection.
