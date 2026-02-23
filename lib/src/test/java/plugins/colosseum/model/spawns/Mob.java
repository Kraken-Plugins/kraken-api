package plugins.colosseum.model.spawns;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.runelite.api.NPC;
import net.runelite.api.Prayer;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Getter
@AllArgsConstructor
public enum Mob {
    SERPENT_SHAMAN(12811, 10, 5, 28, Prayer.PROTECT_FROM_MAGIC),
    JAVELIN_COLOSSUS(12817, 15, 5, 54, Prayer.PROTECT_FROM_MISSILES),
    JAGUAR_WARRIOR(12810, 1, 5, 47, Prayer.PROTECT_FROM_MELEE),
    MANTICORE(12818, 15, 10, 98, null),
    MINOTAUR(12812, 1, 5, 74, Prayer.PROTECT_FROM_MELEE),
    SHOCKWAVE_COLOSSUS(12819, 15, 5, 56, Prayer.PROTECT_FROM_MAGIC),
    BEES(12823, 1, 5, 20, Prayer.PROTECT_FROM_MELEE),
    FREMENIK_WARBAND(12816, 1, 5, 20, Prayer.PROTECT_FROM_MELEE), // TODO Not sure if this is needed
    SOL_HEREDIT(12821, 1, 6, 55, Prayer.PROTECT_FROM_MELEE), // Not sure if needed
    REINFORCEMENT_SHAMAN(12811, 10, 5, 28, Prayer.PROTECT_FROM_MAGIC);

    private static final Map<Integer, Mob> MOB_BY_ID = new HashMap<>();

    static {
        for (Mob value : values()) {
            MOB_BY_ID.putIfAbsent(value.id, value);
        }
    }

    private final int id;
    private final int attackRange;
    private final int attackSpeed;
    private final int maxHit;
    private final Prayer prayer;

    /**
     * Resolves a tracked Colosseum mob profile from an in-game NPC instance.
     *
     * @param npc The NPC to resolve.
     * @return The matching mob profile, or {@code null} when the NPC is not tracked.
     */
    public static Mob fromNpc(NPC npc) {
        if (npc == null) {
            return null;
        }

        String name = npc.getName() == null ? "" : npc.getName().toLowerCase(Locale.ROOT);
        if (npc.getId() == SERPENT_SHAMAN.id && name.contains("reinforcement")) {
            return REINFORCEMENT_SHAMAN;
        }

        Mob byId = MOB_BY_ID.get(npc.getId());
        if (byId != null) {
            return byId;
        }

        return byName(name);
    }

    /**
     * Returns {@code true} if this mob profile is the manticore.
     *
     * @return Whether this profile represents a manticore.
     */
    public boolean isManticore() {
        return this == MANTICORE;
    }

    /**
     * Returns {@code true} if this mob profile is the jaguar warrior.
     *
     * @return Whether this profile represents a jaguar warrior.
     */
    public boolean isJaguarWarrior() {
        return this == JAGUAR_WARRIOR;
    }

    private static Mob byName(String npcName) {
        if (npcName == null || npcName.isEmpty()) {
            return null;
        }

        if (npcName.contains("manticore")) {
            return MANTICORE;
        }
        if (npcName.contains("jaguar")) {
            return JAGUAR_WARRIOR;
        }
        if (npcName.contains("javelin")) {
            return JAVELIN_COLOSSUS;
        }
        if (npcName.contains("shockwave")) {
            return SHOCKWAVE_COLOSSUS;
        }
        if (npcName.contains("minotaur")) {
            return MINOTAUR;
        }
        if (npcName.contains("reinforcement")) {
            return REINFORCEMENT_SHAMAN;
        }
        if (npcName.contains("shaman")) {
            return SERPENT_SHAMAN;
        }
        return null;
    }
}
