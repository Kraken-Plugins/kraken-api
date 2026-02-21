package com.kraken.api.simulation;

import com.kraken.api.simulation.snapshot.SimulationSnapshot;
import lombok.Getter;
import lombok.NonNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Input bundle for simulation tree generation.
 */
@Getter
public final class SimulationScenario {
    private final SimulationSnapshot snapshot;
    private final Map<Integer, SimulationNpcProfile> npcProfilesById;
    private final SimulationNpcProfile defaultNpcProfile;

    /**
     * Creates a scenario from a snapshot and npc-id profile mapping.
     *
     * @param snapshot immutable snapshot of player, npcs, and collision.
     * @param npcProfilesById mapping keyed by npc id.
     */
    public SimulationScenario(
            @NonNull SimulationSnapshot snapshot,
            Map<Integer, SimulationNpcProfile> npcProfilesById
    ) {
        this(snapshot, npcProfilesById, SimulationNpcProfile.DEFAULT);
    }

    /**
     * Creates a scenario from a snapshot and npc-id profile mapping.
     *
     * @param snapshot immutable snapshot of player, npcs, and collision.
     * @param npcProfilesById mapping keyed by npc id.
     * @param defaultNpcProfile fallback profile when a mapping is missing.
     */
    public SimulationScenario(
            @NonNull SimulationSnapshot snapshot,
            Map<Integer, SimulationNpcProfile> npcProfilesById,
            SimulationNpcProfile defaultNpcProfile
    ) {
        this.snapshot = snapshot;
        this.defaultNpcProfile = defaultNpcProfile == null ? SimulationNpcProfile.DEFAULT : defaultNpcProfile;
        this.npcProfilesById = sanitizeMapping(npcProfilesById);
    }

    /**
     * Resolves an NPC profile for an npc id.
     *
     * @param npcId npc id.
     * @return resolved profile.
     */
    public SimulationNpcProfile resolveNpcProfile(int npcId) {
        return npcProfilesById.getOrDefault(npcId, defaultNpcProfile);
    }

    private static Map<Integer, SimulationNpcProfile> sanitizeMapping(Map<Integer, SimulationNpcProfile> mapping) {
        if (mapping == null || mapping.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Integer, SimulationNpcProfile> sanitized = new HashMap<>();
        for (Map.Entry<Integer, SimulationNpcProfile> entry : mapping.entrySet()) {
            Integer npcId = entry.getKey();
            SimulationNpcProfile profile = entry.getValue();
            if (npcId == null || npcId < 0 || profile == null) {
                continue;
            }
            sanitized.put(npcId, profile);
        }
        if (sanitized.isEmpty()) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(sanitized);
    }
}
