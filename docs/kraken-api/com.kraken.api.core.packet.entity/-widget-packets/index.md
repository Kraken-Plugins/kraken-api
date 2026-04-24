//[kraken-api](../../../index.md)/[com.kraken.api.core.packet.entity](../index.md)/[WidgetPackets](index.md)

# WidgetPackets

[Kraken API]\
open class [WidgetPackets](index.md)

A high-level utility class for sending widget-related game packets. This class abstracts the complexity of constructing and sending packets related to widget (interface) interactions, such as clicking buttons. It uses a [PacketClient](../../com.kraken.api.core.packet/-packet-client/index.md) provider to send the low-level packets, which are defined by the [PacketFactory](../../com.kraken.api.core.packet/-packet-factory/index.md).

## Constructors

| | |
|---|---|
| [WidgetPackets](-widget-packets.md) | [Kraken API]<br>constructor() |

## Functions

| Name | Summary |
|---|---|
| [queueResumeCount](queue-resume-count.md) | [Kraken API]<br>open fun [queueResumeCount](queue-resume-count.md)(id: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html))<br>Queues the RESUME_COUNTDIALOG packet, sent in response to a numerical input dialog (e.g., &quot;How many?&quot; or &quot;Enter amount&quot;). |
| [queueResumeObj](queue-resume-obj.md) | [Kraken API]<br>open fun [queueResumeObj](queue-resume-obj.md)(value: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html))<br>Queues the RESUME_OBJDIALOG packet, typically sent as a continuation packet after selecting an option in a multi-choice dialog, where the value represents an item ID or object ID relevant to the dialog option. |
