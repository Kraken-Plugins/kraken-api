//[kraken-api](../../../index.md)/[com.kraken.api.service.prayer](../index.md)/[PrayerService](index.md)/[getActiveOverhead](get-active-overhead.md)

# getActiveOverhead

[Kraken API]\
open fun [getActiveOverhead](get-active-overhead.md)(): [InteractablePrayer](../-interactable-prayer/index.md)

Retrieves the currently active overhead [InteractablePrayer](../-interactable-prayer/index.md). 

This method iterates through all available [InteractablePrayer](../-interactable-prayer/index.md) values and checks each one for the following conditions: 

- The prayer is currently active, as determined by its state.
- The prayer is classified as an overhead prayer based on additional logic.

 If a prayer meets both conditions, it is returned immediately. If no active overhead prayer is found, the method returns `null`.

#### Return

The active overhead [InteractablePrayer](../-interactable-prayer/index.md), or `null` if no overhead prayer is active.
