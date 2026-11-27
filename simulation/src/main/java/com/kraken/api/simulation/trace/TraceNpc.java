package com.kraken.api.simulation.trace;

/**
 * NPC declaration line, written the first tick an NPC appears (wave spawns before tick 0's record, scheduled and
 * modifier spawns mid trace). {@code id} is the stable trace id every tick record and event uses for this NPC; ids
 * are never reused within a wave, so a respawned NPC is a new declaration.
 */
public final class TraceNpc {
    /** Line type tag, always "npc". */
    public String type = "npc";
    /** Stable trace id that every tick record and event uses for this NPC. */
    public int id;
    /** Live game NPC id (-1 when not applicable). */
    public int npcId = -1;
    /** npcs.json key: the replayer's type lookup. */
    public String key;
    /** NPC display name. */
    public String name;
    /** Footprint edge length in tiles. */
    public int size;
    /** Maximum hit points. */
    public int maxHp;
    /** Route finding movement. */
    public boolean smart;
    /** Simulation slot the NPC was bound to (diagnostic; ids are the stable handle). */
    public int slot;
    /** Live only: the game's NPC index. */
    public int rlIndex = -1;
    /** First tick record that includes this NPC. */
    public int firstT;
}
