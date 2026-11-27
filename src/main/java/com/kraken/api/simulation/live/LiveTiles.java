package com.kraken.api.simulation.live;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;

/** Reading the tiles the server simulates from the client. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class LiveTiles {

    /**
     * An actor's server tile (its SW anchor) in instance template coordinates. {@code getLocalLocation()} is the
     * interpolated render position: it lags the true tile for fast movers and rounds a large body's centre.
     * {@code getWorldLocation()} reads the server pathing tile but in instance coordinates, so it maps back through
     * a local point; when that mapping fails the render read stands in.
     * @param client the client
     * @param actor the actor
     * @return the template point
     */
    public static WorldPoint serverTile(Client client, Actor actor) {
        LocalPoint lp = LocalPoint.fromWorld(client.getTopLevelWorldView(), actor.getWorldLocation());
        if (lp == null) {
            return WorldPoint.fromLocalInstance(client, actor.getLocalLocation());
        }
        return WorldPoint.fromLocalInstance(client, lp);
    }
}
