package com.kraken.api.query.graphicsobject;

import com.kraken.api.Context;
import com.kraken.api.core.AbstractEntity;
import com.kraken.api.core.Locatable;
import net.runelite.api.GraphicsObject;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;

/**
 * A graphics object playing on a tile: an AOE impact marker, a spell effect, a boss telegraph.
 *
 * <p>Graphics objects are observations, not menu targets — {@link #interact(String)} always returns
 * {@code false}. Their value is spatial: bosses telegraph area attacks by spawning one on the tiles
 * that are about to be dangerous.</p>
 */
public class GraphicsObjectEntity extends AbstractEntity<GraphicsObject> implements Locatable {

    public GraphicsObjectEntity(Context ctx, GraphicsObject raw) {
        super(ctx, raw);
    }

    @Override
    public int getId() {
        GraphicsObject g = raw();
        return g != null ? g.getId() : -1;
    }

    /**
     * Graphics objects have no name; this always returns {@code null}, so name-based filters never
     * match them. Filter by {@code withId} instead.
     * @return null, always.
     */
    @Override
    public String getName() {
        return null;
    }

    /**
     * Graphics objects cannot be interacted with; nothing is ever dispatched.
     * @param action Ignored.
     * @return false, always.
     */
    @Override
    public boolean interact(String action) {
        return false;
    }

    /**
     * The tile the graphics object is playing on.
     * @return The world point of the effect, or {@code null} when it has finished.
     */
    @Override
    public WorldPoint getWorldLocation() {
        GraphicsObject g = raw();
        if (g == null) return null;
        LocalPoint location = g.getLocation();
        if (location == null) return null;
        return WorldPoint.fromLocal(ctx.getClient(), location.getX(), location.getY(), g.getLevel());
    }

    /**
     * The client cycle the effect started on, comparable against {@code Client.getGameCycle()} to
     * work out how long it has been playing.
     * @return The start cycle.
     */
    public int getStartCycle() {
        GraphicsObject g = raw();
        return g != null ? g.getStartCycle() : 0;
    }
}
