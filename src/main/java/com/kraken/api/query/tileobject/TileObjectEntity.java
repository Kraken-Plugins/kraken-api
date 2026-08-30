package com.kraken.api.query.tileobject;

import com.kraken.api.Context;
import com.kraken.api.core.AbstractEntity;
import com.kraken.api.core.Locatable;
import com.kraken.api.service.tile.GameArea;
import net.runelite.api.ObjectComposition;
import net.runelite.api.TileObject;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.widgets.Widget;

/**
 * Wraps any scenery object, whatever kind of tile object the client happens to model it as.
 *
 * <p>{@link com.kraken.api.query.gameobject.GameObjectEntity} covers only {@code GameObject}. Doors,
 * gates and fences are frequently {@code WallObject}s, and archways and similar decoration are
 * {@code DecorativeObject}s, so anything that needs to open a door has to work at the
 * {@link TileObject} level.</p>
 */
public class TileObjectEntity extends AbstractEntity<TileObject> implements Locatable {

    public TileObjectEntity(Context ctx, TileObject raw) {
        super(ctx, raw);
    }

    @Override
    public int getId() {
        TileObject raw = raw();
        return raw != null ? raw.getId() : -1;
    }

    @Override
    public String getName() {
        ObjectComposition composition = getObjectComposition();
        if (composition != null && composition.getName() != null) {
            return composition.getName();
        }
        return "Unknown (no composition)";
    }

    /**
     * Returns the object composition, resolving impostors where the object has them.
     *
     * <p>Impostors matter here because a door's id changes between its open and closed states, and the
     * transport dataset records one of the two.</p>
     *
     * @return the composition, or null when it could not be read
     */
    public ObjectComposition getObjectComposition() {
        TileObject raw = raw();
        if (raw == null) {
            return null;
        }

        ObjectComposition def = ctx.runOnClientThread(() -> ctx.getClient().getObjectDefinition(raw.getId()));
        if (def == null) {
            return null;
        }

        if (def.getImpostorIds() != null && def.getImpostor() != null) {
            return ctx.runOnClientThread(def::getImpostor);
        }

        return def;
    }

    /**
     * Returns the tile this object stands on.
     *
     * @return the object's world location, or null when the object has despawned
     */
    @Override
    public WorldPoint getWorldLocation() {
        TileObject raw = raw();
        return raw != null ? raw.getWorldLocation() : null;
    }

    @Override
    public boolean interact(String action) {
        TileObject raw = raw();
        if (raw == null) {
            return false;
        }
        return ctx.getInteractionManager().interact(raw, action);
    }

    /**
     * Checks whether the object stands inside the given area.
     *
     * @param area the area to test against
     * @return true when the object is inside the area
     */
    public boolean isInArea(GameArea area) {
        TileObject raw = raw();
        if (area == null || raw == null) {
            return false;
        }
        return area.contains(raw.getWorldLocation());
    }

    /**
     * Uses a widget on this object, for example an item on a door.
     *
     * @param widget the widget to use on the object
     * @return true when the interaction was dispatched
     */
    public boolean useWidget(Widget widget) {
        TileObject raw = raw();
        if (raw == null) {
            return false;
        }
        return ctx.getInteractionManager().interact(widget, raw);
    }
}
