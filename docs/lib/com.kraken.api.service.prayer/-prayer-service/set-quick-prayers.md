//[lib](../../../index.md)/[com.kraken.api.service.prayer](../index.md)/[PrayerService](index.md)/[setQuickPrayers](set-quick-prayers.md)

# setQuickPrayers

[Kraken API]\
open fun [setQuickPrayers](set-quick-prayers.md)(prayers: [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[InteractablePrayer](../-interactable-prayer/index.md)&gt;)

Sets the quick prayers for the player by sending the appropriate widget action packets. If a specified prayer is already set as a quick prayer, it will be skipped.

#### Parameters

Kraken API

| | |
|---|---|
| prayers | The @varargs array of [InteractablePrayer](../-interactable-prayer/index.md) objects to be set as quick prayers. Null values and prayers that are already set will be ignored. |
