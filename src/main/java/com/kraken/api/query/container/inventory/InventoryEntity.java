package com.kraken.api.query.container.inventory;

import com.kraken.api.Context;
import com.kraken.api.query.container.AbstractContainerEntity;
import com.kraken.api.query.container.ContainerItem;
import net.runelite.api.GameObject;
import net.runelite.api.NPC;

public class InventoryEntity extends AbstractContainerEntity {
    public InventoryEntity(Context ctx, ContainerItem raw) {
        super(ctx, raw);
    }

    /**
     * Uses one inventory item on another.
     * @param other The other inventory item to be used on.
     * @return True if the combination action was successful and false otherwise
     */
    public boolean combineWith(ContainerItem other) {
        ContainerItem item = raw();
        if(item.getWidget() != null && other.getWidget() != null){
            return ctx.getInteractionManager().interact(item.getWidget(), other.getWidget());
        }
        return false;
    }

    /**
     * Uses one item in the inventory on the other. This is a shallow wrapper
     * around {@code combineWith()}
     * @param other The other inventory item to be used on.
     * @return True if the use on item was successful and false otherwise
     */
    public boolean useOn(ContainerItem other) {
        return combineWith(other);
    }

    /**
     * Uses one item in the inventory on an NPC.
     * @param npc The NPC to use the inventory item on.
     * @return True if the use on item was successful and false otherwise
     */
    public boolean useOn(NPC npc) {
        ContainerItem item = raw();

        if(item.getWidget() != null && npc != null) {
            return ctx.getInteractionManager().interact(item.getWidget(), npc);
        }
        return false;
    }

    /**
     * Uses one item in the inventory on a Game object.
     * @param gameObject The GameObject to use the inventory item on.
     * @return True if the use on item was successful and false otherwise
     */
    public boolean useOn(GameObject gameObject) {
        ContainerItem item = raw();

        if(item.getWidget() != null && gameObject != null) {
            return ctx.getInteractionManager().interact(item.getWidget(), gameObject);
        }
        return false;
    }

    /**
     * Drops the item from the inventory.
     * @return True if the item was successfully dropped, false otherwise.
     */
    public boolean drop() {
        return interact("Drop");
    }
}
