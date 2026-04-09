package com.kraken.api.query.groundobject;

import com.kraken.api.Context;
import com.kraken.api.core.AbstractEntity;
import com.kraken.api.service.tile.GameArea;

public class GroundObjectEntity extends AbstractEntity<GroundItem> {

    public GroundObjectEntity(Context ctx, GroundItem raw) {
        super(ctx, raw);
    }


    @Override
    public int getId() {
        GroundItem raw = raw();
        return raw != null ? raw.getItemId() : -1;
    }

    @Override
    public String getName() {
        GroundItem raw = raw();
        return raw != null ? raw.getName() : null;
    }

    /**
     * Interacts with the ground object using the requested menu action.
     * @param action The menu action to trigger (e.g. "Take" or "Examine")
     * @return True if the interaction is successful and false otherwise
     */
    @Override
    public boolean interact(String action) {
        GroundItem raw = raw();
        if (raw == null) return false;
        ctx.getInteractionManager().interact(raw, action);
        return true;
    }

    /**
     * Checks if the ground object is within the game area.
     * @param area The {@link GameArea} to check.
     * @return True if the ground object is within the game area and false otherwise.
     */
    public boolean isInArea(GameArea area) {
        if (area == null) return false;
        return area.contains(raw().getLocation());
    }


    /**
     * Takes an item from the ground to be placed in the players inventory.
     * @return True if the action was successful and false otherwise.
     */
    public boolean take() {
        return interact("take"); // Can be any string it doesn't matter, for readability its "take"
    }
}
