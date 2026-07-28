package plugins.colosseumv2.model.spawns;

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
    SERPENT_SHAMAN(12811, 10, 5, 28, Prayer.PROTECT_FROM_MAGIC, 28, new int[]{10859}),
    JAVELIN_COLOSSUS(12817, 15, 5, 54, Prayer.PROTECT_FROM_MISSILES, 54, new int[]{10892, 10893}),
    JAGUAR_WARRIOR(12810, 1, 5, 47, Prayer.PROTECT_FROM_MELEE, 47, new int[]{10847}),
    // Manticore volley totals ~98 across three orbs; a single orb caps at ~36 which is what
    // same-tick prayer conflicts must compare against.
    MANTICORE(12818, 15, 10, 98, null, 36, new int[]{10869}),
    MINOTAUR(12812, 1, 5, 74, Prayer.PROTECT_FROM_MELEE, 74, new int[]{10843}),
    SHOCKWAVE_COLOSSUS(12819, 15, 5, 56, Prayer.PROTECT_FROM_MAGIC, 56, new int[]{10903}),
    BEES(12823, 1, 5, 20, Prayer.PROTECT_FROM_MELEE, 20, new int[0]),
    FREMENIK_WARBAND(12816, 1, 5, 20, Prayer.PROTECT_FROM_MELEE, 20, new int[0]), // TODO Not sure if this is needed
    SOL_HEREDIT(12821, 1, 6, 55, Prayer.PROTECT_FROM_MELEE, 55, new int[0]), // Not sure if needed
    REINFORCEMENT_SHAMAN(12811, 10, 5, 28, Prayer.PROTECT_FROM_MAGIC, 28, new int[]{10859});

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
     * Largest single hit this mob can deal on one tick. Differs from {@link #maxHit} only for
     * the manticore, whose {@code maxHit} reflects a full three-orb volley.
     */
    private final int conflictMaxHit;

    /**
     * Animation ids played on the tick this mob's attack damage is rolled.
     */
    private final int[] attackAnimations;

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
     * Resolves a mob profile from a raw NPC id and (optionally) name.
     *
     * @param npcId The NPC id.
     * @param name  The NPC name, may be {@code null}.
     * @return The matching mob profile, or {@code null} when untracked.
     */
    public static Mob fromIdAndName(int npcId, String name) {
        String lower = name == null ? "" : name.toLowerCase(Locale.ROOT);
        if (npcId == SERPENT_SHAMAN.id && lower.contains("reinforcement")) {
            return REINFORCEMENT_SHAMAN;
        }

        Mob byId = MOB_BY_ID.get(npcId);
        if (byId != null) {
            return byId;
        }

        return byName(lower);
    }

    /**
     * Returns {@code true} when the given animation id is one of this mob's attack animations.
     *
     * @param animationId The animation id to test.
     * @return Whether the animation marks an attack tick.
     */
    public boolean isAttackAnimation(int animationId) {
        if (animationId <= 0) {
            return false;
        }
        for (int attackAnimation : attackAnimations) {
            if (attackAnimation == animationId) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns {@code true} when this mob can only attack from cardinal melee adjacency.
     *
     * @return Whether this profile is a pure melee attacker.
     */
    public boolean isMeleeOnly() {
        return attackRange <= 1;
    }

    /**
     * Returns {@code true} when the plugin should manage protection prayers for this mob.
     * The Fremennik warband is explicitly out of scope, bees cannot be prayed against, and
     * Sol Heredit's boss mechanics are not handled by the auto-prayer engine.
     *
     * @return Whether this mob participates in automatic prayer handling.
     */
    public boolean isPrayable() {
        if (this == BEES || this == FREMENIK_WARBAND || this == SOL_HEREDIT) {
            return false;
        }
        return prayer != null || this == MANTICORE;
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
