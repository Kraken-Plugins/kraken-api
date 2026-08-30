package com.kraken.api.query.player;

import com.kraken.api.Context;
import com.kraken.api.core.AbstractEntity;
import com.kraken.api.core.Locatable;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;

public class PlayerEntity extends AbstractEntity<Player> implements Locatable {
    public PlayerEntity(Context ctx, Player raw) {
        super(ctx, raw);
    }

    @Override
    public WorldPoint getWorldLocation() {
        Player p = raw();
        return p != null ? p.getWorldLocation() : null;
    }

    @Override
    public int getId() {
        Player p = raw();
        return p != null ? p.getId() : -1;
    }

    @Override
    public String getName() {
        Player p = raw();
        return p != null ? p.getName() : null;
    }

    @Override
    public boolean interact(String action) {
        Player p = raw();
        if (p == null) return false;

        return ctx.getInteractionManager().interact(p, action);
    }

    /**
     * Returns true if the player has a Skull icon above their head and false otherwise.
     * @return boolean True if the player is skulled
     */
    public boolean isSkulled() {
        return raw().getSkullIcon() != -1;
    }
}