package com.kraken.api.core.interaction;

import com.kraken.api.core.interaction.model.ResolvedMenuAction;

import java.util.Optional;

/**
 * Strategy for resolving a {@link ResolvedMenuAction} for a specific entity type.
 *
 * @param <T> The type of game entity this resolver handles (NPC, Widget, TileObject, etc.)
 */
public interface MenuActionResolver<T> {

    /**
     * Resolves the appropriate menu action for the given entity and action string.
     *
     * @param entity The game entity to interact with
     * @param action The action string (e.g. "Attack", "Talk-to", "Examine")
     * @return An Optional containing the resolved action, or empty if resolution failed
     */
    Optional<ResolvedMenuAction> resolve(T entity, String action);

    /**
     * The entity type this resolver handles. Used for registry lookup.
     */
    Class<T> getEntityType();
}
