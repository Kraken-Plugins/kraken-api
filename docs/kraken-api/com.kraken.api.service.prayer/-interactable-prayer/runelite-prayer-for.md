//[kraken-api](../../../index.md)/[com.kraken.api.service.prayer](../index.md)/[InteractablePrayer](index.md)/[runelitePrayerFor](runelite-prayer-for.md)

# runelitePrayerFor

[Kraken API]\
open fun [runelitePrayerFor](runelite-prayer-for.md)(p: [InteractablePrayer](index.md)): Prayer

Maps an @InteractablePrayer to its corresponding @Prayer from the RuneLite API. If no matching prayer is found, returns `null`. 

This method iterates through all values of the @Prayer enum and matches them based on their `varbit` value.

#### Return

The @Prayer instance matching the specified @InteractablePrayer. Returns `null` if no match is found.

#### Parameters

Kraken API

| | |
|---|---|
| p | The @InteractablePrayer instance whose corresponding @Prayer needs to be determined. Must not be null. |
