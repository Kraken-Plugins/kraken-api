package com.kraken.api.simulation.data;

import com.kraken.api.simulation.trace.TracePack;

/**
 * A loaded content pack: one encounter's mechanics tables with its behaviours bound, and how its state reads and
 * writes in wave traces. Packs load through {@link MechanicsData#load} with their own resource root.
 */
public interface ContentPack {
    /**
     * The pack's name: its block name in npcs.json and the {@code pack} field of the traces it writes.
     * @return the name
     */
    String name();

    /**
     * The pack's tables.
     * @return the data
     */
    MechanicsData data();

    /**
     * The pack's trace columns, replay forcing and diff channels.
     * @return the trace hooks
     */
    default TracePack trace() {
        return TracePack.NONE;
    }
}
