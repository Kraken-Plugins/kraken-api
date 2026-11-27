package com.kraken.api.simulation.data;

import com.google.gson.JsonObject;
import com.kraken.api.simulation.npc.PackRules;

/**
 * What a content pack supplies while its tables load: it binds a behaviour to each type that needs one
 * ({@link MechanicsData#bindBehavior}) and returns its world rules.
 */
@FunctionalInterface
public interface PackBinder {
    /**
     * Binds the pack to freshly loaded tables. Runs once, before anything simulates on them.
     * @param data the tables, behaviours not yet bound
     * @param npcFile the whole npcs.json, for the pack's own top level keys
     * @return the pack's world rules, or null for none
     */
    PackRules bind(MechanicsData data, JsonObject npcFile);
}
