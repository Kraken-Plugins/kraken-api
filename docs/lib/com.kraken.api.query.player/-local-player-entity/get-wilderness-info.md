//[lib](../../../index.md)/[com.kraken.api.query.player](../index.md)/[LocalPlayerEntity](index.md)/[getWildernessInfo](get-wilderness-info.md)

# getWildernessInfo

[Kraken API]\
open fun [getWildernessInfo](get-wilderness-info.md)(): [WildernessInfo](../-wilderness-info/index.md)

Retrieves information about the current wilderness state for the player, including the wilderness level and the player's combat level, based on data from the game client. 

The method checks the visibility and content of the wilderness-related widget to assess the wilderness level. Situations such as protection areas, Deadman mode, or wilderness-free zones are handled specifically.

This method provides information as follows:

- If the wilderness widget is not visible or is malformed, the wilderness level is set to 0 with a combat level of -1.
- If the widget text indicates a protected or guarded area, the wilderness level is also set to 0 with a combat level of -1.
- If in Deadman mode, the wilderness level is set to Integer.MAX_VALUE, and the player's combat level is returned.
- For other scenarios, the wilderness level is calculated based on the widget text or the player's current position in the world.

#### Return

`WildernessInfo` object representing: 

- The calculated wilderness level, depending on the game's widget state and player position.
- The player's combat level, if applicable. Returns -1 in cases where the wilderness level cannot be determined.
