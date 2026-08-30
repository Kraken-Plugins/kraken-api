package com.kraken.api.query.container;

/**
 * An entity representing an item held in one of the player's item containers (inventory, bank,
 * bank-side inventory, deposit box, shop-side inventory).
 *
 * <p>Implementing this is what admits an entity type to {@link AbstractContainerQuery} and its shared
 * item-filter vocabulary ({@code inSlot}, {@code noted}, {@code stackable}, {@code quantityGreaterThan},
 * {@code withAction}, {@code hasItem}).</p>
 */
public interface ItemEntity {

    /**
     * The number of this item in its slot: the stack size for stackable or noted items, otherwise 1.
     * @return The quantity, or 0 when the item is absent.
     */
    int getQuantity();

    /**
     * The slot index the item occupies within its container.
     * @return The zero-based slot index, or -1 when unknown.
     */
    int getSlot();

    /**
     * Whether the item is a bank note rather than the physical item.
     * @return True when the item is noted.
     */
    boolean isNoted();

    /**
     * Whether multiple units of the item share a single slot (runes, arrows, noted items).
     * @return True when the item stacks.
     */
    boolean isStackable();

    /**
     * Whether the item offers the given menu action in this container, e.g. "Eat", "Drop",
     * "Withdraw-All".
     * @param action The action to check for, case-insensitive.
     * @return True when the action is available on the item.
     */
    boolean hasAction(String action);
}
