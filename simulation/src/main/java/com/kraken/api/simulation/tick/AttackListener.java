package com.kraken.api.simulation.tick;

/**
 * Test and telemetry seam: invoked once per NPC attack launch with its style (-1 while unknown, such as an orb of a
 * volley whose order is not seen yet). The default methods report the other observable engine events the trace
 * recorder serializes. Null in production, so the hot path pays one null check.
 */
public interface AttackListener {
    /**
     * An NPC launched an attack.
     * @param slot the attacker's slot
     * @param style its style, or -1 when unknown
     * @param tick the wave tick
     */
    void onNpcAttack(int slot, int style, int tick);

    /**
     * An NPC cast a heal: an instant heal or one in flight.
     * @param srcSlot the healer's slot
     * @param targetSlot the healed slot
     * @param amount hitpoints
     * @param tick the wave tick
     */
    default void onNpcHeal(int srcSlot, int targetSlot, int amount, int tick) {
    }

    /**
     * A tile targeted attack launched: the player's tile at launch (the tile it lands on may be picked later) and the
     * ticks until it lands.
     * @param srcSlot the attacker's slot
     * @param targetTile the packed tile
     * @param landDelayTicks ticks until it lands
     * @param tick the wave tick
     */
    default void onTileAttack(int srcSlot, int targetTile, int landDelayTicks, int tick) {
    }

    /**
     * A hit landed on the player after its tile and prayer checks, with both damage channels.
     * @param srcSlot the source slot (31 or more for no NPC)
     * @param style the hit's style
     * @param expected expected channel damage
     * @param worst worst case damage
     * @param tick the wave tick
     */
    default void onPlayerHit(int srcSlot, int style, int expected, int worst, int tick) {
    }
}
