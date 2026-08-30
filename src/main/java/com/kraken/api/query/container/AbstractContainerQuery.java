package com.kraken.api.query.container;

import com.kraken.api.Context;
import com.kraken.api.core.AbstractQuery;
import com.kraken.api.core.Interactable;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Base class for queries over the player's item containers: inventory, bank, bank-side inventory,
 * deposit box, and shop-side inventory.
 *
 * <p>This is the single owner of the item-filter vocabulary, so every container query offers the same
 * filters with the same names and semantics. Queries keep only their genuinely container-specific
 * filters (food, withdraw modes, sellability, and so on).</p>
 *
 * @param <T> The type of entity being queried, which must expose item properties
 * @param <Q> The concrete query class
 * @param <R> The raw backing type
 */
public abstract class AbstractContainerQuery<T extends Interactable<R> & ItemEntity, Q extends AbstractContainerQuery<T, Q, R>, R> extends AbstractQuery<T, Q, R> {

    public AbstractContainerQuery(Context ctx) {
        super(ctx);
    }

    /**
     * Filters for the item occupying a specific slot of the container.
     * @param slot The zero-based slot index.
     * @return Q the item in that slot, or nothing when the slot is empty.
     */
    public Q inSlot(int slot) {
        return filter(t -> t.getSlot() == slot);
    }

    /**
     * Filters for items that are bank notes.
     * @return Q noted items.
     */
    public Q noted() {
        return filter(ItemEntity::isNoted);
    }

    /**
     * Filters for items that are not bank notes.
     * @return Q un-noted items.
     */
    public Q unnoted() {
        return filter(t -> !t.isNoted());
    }

    /**
     * Filters for items that stack (runes, arrows, noted items).
     * @return Q stackable items.
     */
    public Q stackable() {
        return filter(ItemEntity::isStackable);
    }

    /**
     * Filters for items whose quantity is strictly greater than the given amount.
     * @param amount The exclusive lower bound on quantity.
     * @return Q items held in greater quantity than the amount.
     */
    public Q quantityGreaterThan(int amount) {
        return filter(t -> t.getQuantity() > amount);
    }

    /**
     * Filters for items offering a specific menu action in this container, e.g. "Eat", "Drop",
     * "Withdraw-All".
     * @param action The action to filter for, case-insensitive.
     * @return Q items with the action.
     */
    public Q withAction(String action) {
        return filter(t -> t.hasAction(action));
    }

    /**
     * Returns true when the container holds an item with the given id, respecting any filters already
     * applied.
     * @param id The item id to search for.
     * @return True when the item is present.
     */
    public boolean hasItem(int id) {
        return withId(id).isPresent();
    }

    /**
     * Returns true when the container holds an item with the given name, respecting any filters
     * already applied. Case-insensitive, full name required.
     * @param name The item name to search for.
     * @return True when the item is present.
     */
    public boolean hasItem(String name) {
        if (name == null || name.isEmpty()) return false;
        return withName(name).isPresent();
    }

    /**
     * Returns true only when the container holds every one of the given item ids.
     * @param ids The item ids to search for. An empty argument list yields true.
     * @return True when every id is present.
     */
    public boolean hasItems(int... ids) {
        if (ids == null || ids.length == 0) return true;

        Set<Integer> held = stream().map(Interactable::getId).collect(Collectors.toSet());
        for (int id : ids) {
            if (!held.contains(id)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns true only when the container holds every one of the given item ids.
     * @param ids The item ids to search for. {@code null} or empty yields true.
     * @return True when every id is present.
     */
    public boolean hasItems(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) return true;
        return hasItems(ids.stream().mapToInt(Integer::intValue).toArray());
    }

    /**
     * Returns true only when the container holds every one of the given item names.
     * Case-insensitive, full names required.
     * @param names The item names to search for. An empty argument list yields true.
     * @return True when every name is present.
     */
    public boolean hasItems(String... names) {
        if (names == null || names.length == 0) return true;

        Set<String> held = stream()
                .map(Interactable::getName)
                .filter(Objects::nonNull)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        for (String name : names) {
            if (name == null) continue;
            if (!held.contains(name.toLowerCase())) {
                return false;
            }
        }
        return true;
    }
}
