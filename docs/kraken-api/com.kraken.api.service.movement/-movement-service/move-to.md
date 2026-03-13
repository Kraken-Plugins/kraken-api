//[kraken-api](../../../index.md)/[com.kraken.api.service.movement](../index.md)/[MovementService](index.md)/[moveTo](move-to.md)

# moveTo

[Kraken API]\
open fun [moveTo](move-to.md)(point: WorldPoint)

Moves the player to the specified WorldPoint, handling instanced areas conversion when necessary. This method ensures accurate movement by checking if the player is within an instance and applying the respective LocalPoint to WorldPoint conversion. 

The operation involves: 

- Converting the given WorldPoint to handle instanced logic if the player is in an instance.
- Obtaining a click position on the game canvas for the target point.
- Sending interaction packets to queue both mouse clicks and movement commands.

#### Parameters

Kraken API

| | |
|---|---|
| point | The WorldPoint representing the destination to move towards. |

[Kraken API]\
open fun [moveTo](move-to.md)(point: LocalPoint)

Moves the player to a specified LocalPoint. This method accounts for whether the player is within an instance and converts the LocalPoint to a WorldPoint to enable movement. If the current top-level world view is an instance, additional conversion logic is applied to ensure accuracy. 

The method performs the following actions: 

- Converts the provided LocalPoint to a WorldPoint, handling instance logic if needed.
- Determines the clickbox for the target WorldPoint to simulate a mouse click.
- Queues packets to notify the game client of the movement and corresponding mouse interaction.

#### Parameters

Kraken API

| | |
|---|---|
| point | The LocalPoint representing the target destination to move towards. |
