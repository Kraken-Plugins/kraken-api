//[kraken-api](../../../index.md)/[com.kraken.api.service.prayer](../index.md)/[PrayerService](index.md)/[toggle](toggle.md)

# toggle

[Kraken API]\
open fun [toggle](toggle.md)(prayer: Prayer): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)

Toggles a prayer on or off. This will use reflection based prayer toggles by default.

#### Return

Boolean true if the prayer was activated/deactivated successfully and false otherwise.

#### Parameters

Kraken API

| | |
|---|---|
| prayer | The Prayer to toggle |

[Kraken API]\
open fun [toggle](toggle.md)(prayer: Prayer, activate: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)

Toggles a prayer on or off.

#### Return

Boolean true if the prayer was activated/deactivated successfully and false otherwise.

#### Parameters

Kraken API

| | |
|---|---|
| prayer | The Prayer to toggle |
| activate | True if the prayer should be turned on and false if it should be turned off |
