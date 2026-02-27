//[lib](../../../index.md)/[com.kraken.api.core.packet.entity](../index.md)/[WidgetPackets](index.md)/[queueResumeName](queue-resume-name.md)

# queueResumeName

[Kraken API]\
open fun [queueResumeName](queue-resume-name.md)(name: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html))

Queues the RESUME_NAMEDIALOG packet, sent in response to a chat dialog asking the player to enter a name (e.g., setting a clan name). 

 Note: The packet data includes the length of the string plus one for the null terminator.

#### Parameters

Kraken API

| | |
|---|---|
| name | The string name entered by the player. |
