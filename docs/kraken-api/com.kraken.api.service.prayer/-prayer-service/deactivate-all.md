//[kraken-api](../../../index.md)/[com.kraken.api.service.prayer](../index.md)/[PrayerService](index.md)/[deactivateAll](deactivate-all.md)

# deactivateAll

[Kraken API]\
open fun [deactivateAll](deactivate-all.md)(keepPreserveOn: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html), onlyProtectionPrayers: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html), maxActions: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)

Deactivates prayers based on the provided parameters. This method can limit the deactivation to specific types of prayers and ensure certain prayers are preserved. 

This method iterates through all available `Prayer` values, selectively deactivating prayers according to the following conditions: 

- It skips prayers with names containing &quot;RP_&quot; (Ruinous Prayers).
- If `keepPreserveOn` is `true`, the `PRESERVE` prayer will not be deactivated.
- If `onlyProtectionPrayers` is `true`, only prayers categorized as protection prayers will be deactivated.
- Deactivation will stop once the number of actions taken reaches `maxActions`.

#### Return

`true` if all applicable prayers were successfully processed within the `maxActions` limit; `false` if the method aborted early due to reaching the `maxActions` limit.

#### Parameters

Kraken API

| | |
|---|---|
| keepPreserveOn | a boolean indicating whether the `PRESERVE` prayer should remain active. |
| onlyProtectionPrayers | a boolean indicating whether only protection prayers should be deactivated. |
| maxActions | the maximum number of deactivation actions to perform. |

[Kraken API]\
open fun [deactivateAll](deactivate-all.md)(keepPreserve: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)

Deactivates all prayers. This method allows specifying whether to keep the preserve prayer on when deactivating other prayers

#### Return

true if the deactivation process completes successfully, false otherwise.

#### Parameters

Kraken API

| | |
|---|---|
| keepPreserve | true to retain specified preserved states during deactivation, false to deactivate entirely without preservation. |

[Kraken API]\
open fun [deactivateAll](deactivate-all.md)()

Deactivates all prayers (including preserve)
