//[kraken-api](../../../index.md)/[com.kraken.api.service.prayer](../index.md)/[PrayerService](index.md)/[oneTickFlick](one-tick-flick.md)

# oneTickFlick

[Kraken API]\
open fun [oneTickFlick](one-tick-flick.md)(disableAll: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html), prayers: [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;Prayer&gt;)

Performs a one-tick prayer flick, enabling and disabling specified prayers within the same game tick. This ensures minimal prayer drain while maintaining the effects of the selected prayers. 

The method first identifies any currently active prayers and deactivates them if needed. Then, the specified prayers in the @&lt;varargs&gt; parameter are activated. If `disableAll` is set to `true`, all active prayers are deactivated before activating the specified ones; otherwise, only the prayers included in the list are toggled.

#### Parameters

Kraken API

| | |
|---|---|
| disableAll | A `boolean` value indicating if all currently active prayers should be disabled before activating the specified prayers. If `true`, all existing prayers (except Ruinous Prayers) are deactivated. |
| prayers | A varargs list of `Prayer` objects that should be activated during the one-tick flick process. These prayers are toggled on after any necessary deactivations are completed. If no prayers are provided, no activation takes place. |
