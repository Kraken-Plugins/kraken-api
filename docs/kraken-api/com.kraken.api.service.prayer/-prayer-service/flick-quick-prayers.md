//[kraken-api](../../../index.md)/[com.kraken.api.service.prayer](../index.md)/[PrayerService](index.md)/[flickQuickPrayers](flick-quick-prayers.md)

# flickQuickPrayers

[Kraken API]\
open fun [flickQuickPrayers](flick-quick-prayers.md)()

Performs a quick flick of the player's quick prayers. 

This method is designed to activate and deactivate the player's quick prayers in a single operation. If quick prayers are currently disabled, they will be turned on by invoking `enableQuickPrayers()`. If quick prayers are already enabled, the method performs a rapid sequence of toggles by calling `toggleQuickPrayers()` twice.

The behavior of this method ensures that quick prayers are briefly flicked and restored, which may be used in scenarios such as prayer management or minimizing prayer drain during certain game activities.

- If quick prayers are not enabled, they are activated, and no toggle operation is performed.
- If quick prayers are enabled, they are toggled off and back on in quick succession.
