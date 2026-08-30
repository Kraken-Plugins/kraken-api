package com.kraken.api.core;

import net.runelite.api.coords.WorldPoint;

/**
 * A game entity that occupies a tile in the loaded scene.
 *
 * <p>Implementing this is what admits an entity type to {@link AbstractSpatialQuery} and its shared
 * spatial vocabulary ({@code within}, {@code withinArea}, {@code at}, {@code reachable},
 * {@code sortByDistance}, {@code nearest}).</p>
 */
public interface Locatable {

    /**
     * The entity's current world location, as the client reports it for the top-level world view.
     *
     * <p>This is the same coordinate space the local player's own location is reported in — inside
     * instanced regions both are instance coordinates — so distances and area tests between an entity
     * and the player remain valid everywhere, including raids and other instances. For multi-tile
     * objects this is the south-west tile.</p>
     *
     * @return The entity's world location, or {@code null} when the entity has despawned.
     */
    WorldPoint getWorldLocation();
}
