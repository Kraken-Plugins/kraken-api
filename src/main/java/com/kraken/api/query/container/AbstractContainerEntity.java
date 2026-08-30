package com.kraken.api.query.container;

import com.kraken.api.Context;
import com.kraken.api.core.AbstractEntity;

import java.util.Arrays;

/**
 * Base class for entities backed by a {@link ContainerItem}: inventory, bank-side inventory, deposit
 * box, and shop-side inventory items. Supplies the shared identity, interaction, and
 * {@link ItemEntity} accessors so each container entity only carries its container-specific verbs.
 */
public abstract class AbstractContainerEntity extends AbstractEntity<ContainerItem> implements ItemEntity {

    public AbstractContainerEntity(Context ctx, ContainerItem raw) {
        super(ctx, raw);
    }

    @Override
    public String getName() {
        ContainerItem item = raw();
        return item != null ? item.getName() : null;
    }

    @Override
    public int getId() {
        ContainerItem item = raw();
        return item != null ? item.getId() : -1;
    }

    @Override
    public boolean interact(String action) {
        ContainerItem item = raw();
        if (item == null) return false;
        return ctx.getInteractionManager().interact(item, action);
    }

    @Override
    public int getQuantity() {
        ContainerItem item = raw();
        return item != null ? item.getQuantity() : 0;
    }

    @Override
    public int getSlot() {
        ContainerItem item = raw();
        return item != null ? item.getSlot() : -1;
    }

    @Override
    public boolean isNoted() {
        ContainerItem item = raw();
        return item != null && item.isNoted();
    }

    @Override
    public boolean isStackable() {
        ContainerItem item = raw();
        return item != null && item.isStackable();
    }

    @Override
    public boolean hasAction(String action) {
        ContainerItem item = raw();
        if (item == null || item.getInventoryActions() == null) return false;
        return Arrays.stream(item.getInventoryActions())
                .anyMatch(a -> a != null && a.equalsIgnoreCase(action));
    }
}
