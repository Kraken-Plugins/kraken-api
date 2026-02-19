# Simulation Engine

The `sim` package of the API includes classes for simulating game ticks, NPC pathing, movement, line of sight, and players. This is useful for advanced
plugins which evaluate hundreds of potential outcomes every game tick to determine the best "decision". e.g. Inferno and Colosseum plugins.

The simulation engine supports:

- Player and NPC pathing mechanics
- Simulating walking and running movement
- Basic LoS (advanced is still being implemented)
- Placing NPCs at locations
- Different NPC sizes
- Obstacles (based on collision maps)

![sim-example-image](../images/sim.png)

Currently, there isn't a full-fledged API for performing a simulation in the context of the game; however, 
this is actively being developed and is open to some contributions!

## ColoSim

I have created a port of the [OSRS Colosseum Line of Sight Simulator found here](https://los.colosim.com/) to java
within the `com.kraken.api.sim.colosim` package. It shows an example of a simulation for the Colosseum NPCs but 
is not a generic simulation engine which can be used in other contexts or within the context of a RuneLite plugin.

I've added it to the API as an example which hopefully I and others can build upon to create an open source 
generic simulation engine for Old School RuneScape!

