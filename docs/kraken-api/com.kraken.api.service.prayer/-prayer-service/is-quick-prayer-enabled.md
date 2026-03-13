//[kraken-api](../../../index.md)/[com.kraken.api.service.prayer](../index.md)/[PrayerService](index.md)/[isQuickPrayerEnabled](is-quick-prayer-enabled.md)

# isQuickPrayerEnabled

[Kraken API]\
open fun [isQuickPrayerEnabled](is-quick-prayer-enabled.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)

Determines if quick prayers are currently enabled for the player. 

The method checks the value of the @VarbitID.QUICKPRAYER_ACTIVE game state variable to assess whether quick prayers are active. A return value of `true` indicates that quick prayers are enabled, while `false` signifies that they are disabled.

#### Return

`true` if quick prayers are enabled, `false` otherwise.
