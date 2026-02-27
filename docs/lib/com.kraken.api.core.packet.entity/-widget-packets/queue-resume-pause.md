//[lib](../../../index.md)/[com.kraken.api.core.packet.entity](../index.md)/[WidgetPackets](index.md)/[queueResumePause](queue-resume-pause.md)

# queueResumePause

[Kraken API]\
open fun [queueResumePause](queue-resume-pause.md)(widgetId: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), childId: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html))

Queues the RESUME_PAUSEBUTTON packet, typically sent when the player clicks a &quot;Click here to continue&quot; or &quot;Close&quot; button on a standard, non-interactable dialog, such as a dialogue with an NPC. The widget id should be a packed integer (containing both the group and child ids).

#### Parameters

Kraken API

| | |
|---|---|
| widgetId | The ID of the top-level widget (packed to include group and child ids). |
| childId | The ID of the child component that was clicked. |

[Kraken API]\
open fun [queueResumePause](queue-resume-pause.md)(packed: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html))

Queues the RESUME_PAUSEBUTTON packet, usually sent when interacting with non-interactable dialogs (e.g., &quot;Click here to continue&quot;). 

This method sends a packet using the provided packed widget ID.

#### Parameters

Kraken API

| | |
|---|---|
| packed | The packed widget ID, which includes both group and child IDs. |
