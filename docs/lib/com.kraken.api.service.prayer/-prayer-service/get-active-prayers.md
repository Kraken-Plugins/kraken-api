//[lib](../../../index.md)/[com.kraken.api.service.prayer](../index.md)/[PrayerService](index.md)/[getActivePrayers](get-active-prayers.md)

# getActivePrayers

[Kraken API]\
open fun [getActivePrayers](get-active-prayers.md)(): [List](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/List.html)&lt;[InteractablePrayer](../-interactable-prayer/index.md)&gt;

Retrieves a list of all currently active prayers. 

This method iterates through all available [InteractablePrayer](../-interactable-prayer/index.md) instances and checks if they are active by invoking `isActive()` on each. Any prayers identified as active are added to the returned list.

#### Return

A [List](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/List.html) of [InteractablePrayer](../-interactable-prayer/index.md) objects that are currently active.
