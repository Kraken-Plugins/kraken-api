//[lib](../../../index.md)/[com.kraken.api.service.prayer](../index.md)/[PrayerService](index.md)

# PrayerService

[Kraken API]\
open class [PrayerService](index.md)

## Constructors

| | |
|---|---|
| [PrayerService](-prayer-service.md) | [Kraken API]<br>constructor() |

## Functions

| Name | Summary |
|---|---|
| [activatePrayer](activate-prayer.md) | [Kraken API]<br>open fun [activatePrayer](activate-prayer.md)(prayer: Prayer)<br>Wrapper method which turns a prayer on. |
| [deactivateAll](deactivate-all.md) | [Kraken API]<br>open fun [deactivateAll](deactivate-all.md)()<br>Deactivates all prayers (including preserve)<br>[Kraken API]<br>open fun [deactivateAll](deactivate-all.md)(keepPreserve: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>Deactivates all prayers.<br>[Kraken API]<br>open fun [deactivateAll](deactivate-all.md)(keepPreserveOn: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html), onlyProtectionPrayers: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html), maxActions: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>Deactivates prayers based on the provided parameters. |
| [deactivatePrayer](deactivate-prayer.md) | [Kraken API]<br>open fun [deactivatePrayer](deactivate-prayer.md)(prayer: Prayer): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>Wrapper method which turns a prayer off. |
| [deactivateProtectionPrayers](deactivate-protection-prayers.md) | [Kraken API]<br>open fun [deactivateProtectionPrayers](deactivate-protection-prayers.md)()<br>Deactivates all protection prayers (protect from range, melee, and magic) but will keep other prayers like preserve and protect item active. |
| [disableQuickPrayers](disable-quick-prayers.md) | [Kraken API]<br>open fun [disableQuickPrayers](disable-quick-prayers.md)()<br>Turns off the set quick prayers |
| [enableQuickPrayers](enable-quick-prayers.md) | [Kraken API]<br>open fun [enableQuickPrayers](enable-quick-prayers.md)()<br>Turns on the set quick prayers |
| [flickQuickPrayers](flick-quick-prayers.md) | [Kraken API]<br>open fun [flickQuickPrayers](flick-quick-prayers.md)()<br>Performs a quick flick of the player's quick prayers. |
| [getActiveOverhead](get-active-overhead.md) | [Kraken API]<br>open fun [getActiveOverhead](get-active-overhead.md)(): [InteractablePrayer](../-interactable-prayer/index.md)<br>Retrieves the currently active overhead [InteractablePrayer](../-interactable-prayer/index.md). |
| [getActivePrayers](get-active-prayers.md) | [Kraken API]<br>open fun [getActivePrayers](get-active-prayers.md)(): [List](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/List.html)&lt;[InteractablePrayer](../-interactable-prayer/index.md)&gt;<br>Retrieves a list of all currently active prayers. |
| [isActive](is-active.md) | [Kraken API]<br>open fun [isActive](is-active.md)(prayer: Prayer): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>Returns true if the prayer is active and false otherwise. |
| [isOutOfPrayer](is-out-of-prayer.md) | [Kraken API]<br>open fun [isOutOfPrayer](is-out-of-prayer.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>Checks if the player is out of prayer. |
| [isQuickPrayerEnabled](is-quick-prayer-enabled.md) | [Kraken API]<br>open fun [isQuickPrayerEnabled](is-quick-prayer-enabled.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>Determines if quick prayers are currently enabled for the player. |
| [isQuickPrayerSet](is-quick-prayer-set.md) | [Kraken API]<br>open fun [isQuickPrayerSet](is-quick-prayer-set.md)(prayer: [InteractablePrayer](../-interactable-prayer/index.md)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>Checks if a given quick prayer is set |
| [oneTickFlick](one-tick-flick.md) | [Kraken API]<br>open fun [oneTickFlick](one-tick-flick.md)(disableAll: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html), prayers: [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;Prayer&gt;)<br>Performs a one-tick prayer flick, enabling and disabling specified prayers within the same game tick. |
| [setQuickPrayers](set-quick-prayers.md) | [Kraken API]<br>open fun [setQuickPrayers](set-quick-prayers.md)(prayers: [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[InteractablePrayer](../-interactable-prayer/index.md)&gt;)<br>Sets the quick prayers for the player by sending the appropriate widget action packets. |
| [toggle](toggle.md) | [Kraken API]<br>open fun [toggle](toggle.md)(prayer: Prayer): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>open fun [toggle](toggle.md)(prayer: Prayer, activate: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>Toggles a prayer on or off. |
| [toggleQuickPrayers](toggle-quick-prayers.md) | [Kraken API]<br>open fun [toggleQuickPrayers](toggle-quick-prayers.md)()<br>Toggles the set quick prayers. |
