package com.kraken.api.core;

public interface Interactable<T> {
    /**
     * Interacts with the entity using the given action verb.
     *
     * <p>Returns false when nothing was sent to the server — the entity was null or had despawned, or
     * the requested action does not exist on it. Callers may rely on this to drive retries: a false
     * return means the game state is unchanged by this call.</p>
     *
     * @param action The menu action to trigger (e.g., "Attack", "Talk-to", "Take")
     * @return true if the action resolved and was dispatched to the client's engine
     */
    boolean interact(String action);

    /**
     * Returns the wrapped (raw) RuneLite API object for this interactable game entity. This
     * is useful to provide easy access to familiar and underlying RuneLite game data. For example:
     * an {@code EquipmentEntity} will expose the RuneLite {@code Widget} object for the interactable piece of equipment.
     * @return T wrapped RuneLite API object.
     */
    T raw();

    /**
     * The item ID for the wrapped game entity
     * @return int Item id
     */
    int getId();

    /**
     * The game entities name.
     * @return The name of the game entity i.e. NPC name for NPC's, item name for ContainerItem's, and GameObject name
     * for various game objects.
     */
    String getName();
}