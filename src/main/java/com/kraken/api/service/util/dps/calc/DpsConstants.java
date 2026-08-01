package com.kraken.api.service.util.dps.calc;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * NPC id lists and shared constants used by special-case handling in the DPS calculation,
 * ported from the OSRS wiki DPS calculator's constants.ts.
 */
public final class DpsConstants {

    private DpsConstants() {
    }

    public static final double SECONDS_PER_TICK = 0.6;
    public static final int DEFAULT_ATTACK_SPEED = 4;

    private static Set<Integer> ids(Integer... values) {
        return Collections.unmodifiableSet(new HashSet<>(Arrays.asList(values)));
    }

    private static Set<Integer> union(Set<Integer> a, Set<Integer> b) {
        Set<Integer> out = new HashSet<>(a);
        out.addAll(b);
        return Collections.unmodifiableSet(out);
    }

    public static final Set<Integer> BLOWPIPE_IDS = ids(12926, 28688, 31575, 31579, 31583);

    // Tombs of Amascut
    public static final Set<Integer> AKKHA_IDS = ids(11789, 11790, 11791, 11792, 11793, 11794, 11795, 11796);
    public static final Set<Integer> AKKHA_SHADOW_IDS = ids(11797, 11798, 11799);
    public static final Set<Integer> BABA_IDS = ids(11778, 11779, 11780);
    public static final Set<Integer> KEPHRI_SHIELDED_IDS = ids(11719);
    public static final Set<Integer> KEPHRI_UNSHIELDED_IDS = ids(11721);
    public static final Set<Integer> KEPHRI_OVERLORD_IDS = ids(11724, 11725, 11726);
    public static final Set<Integer> ZEBAK_IDS = ids(11730, 11732, 11733);
    public static final Set<Integer> TOA_OBELISK_IDS = ids(11751, 11750, 11752);
    public static final Set<Integer> P2_WARDEN_IDS = ids(11753, 11754, 11756, 11757);
    public static final Set<Integer> P3_WARDEN_IDS = ids(11761, 11763, 11762, 11764);
    public static final Set<Integer> TOA_WARDEN_CORE_EJECTED_IDS = ids(11755, 11758);

    public static final Set<Integer> TOMBS_OF_AMASCUT_MONSTER_IDS;

    static {
        Set<Integer> toa = new HashSet<>();
        toa.addAll(AKKHA_IDS);
        toa.addAll(AKKHA_SHADOW_IDS);
        toa.addAll(BABA_IDS);
        toa.addAll(KEPHRI_SHIELDED_IDS);
        toa.addAll(KEPHRI_UNSHIELDED_IDS);
        toa.addAll(KEPHRI_OVERLORD_IDS);
        toa.addAll(ZEBAK_IDS);
        toa.addAll(TOA_OBELISK_IDS);
        toa.addAll(P2_WARDEN_IDS);
        toa.addAll(TOA_WARDEN_CORE_EJECTED_IDS);
        toa.addAll(P3_WARDEN_IDS);
        TOMBS_OF_AMASCUT_MONSTER_IDS = Collections.unmodifiableSet(toa);
    }

    // Theatre of Blood
    public static final Set<Integer> VERZIK_P1_IDS = ids(10830, 10831, 10832, 8369, 8370, 8371, 10847, 10848, 10849);
    public static final Set<Integer> VERZIK_IDS = union(VERZIK_P1_IDS,
            ids(10833, 10834, 10835, 8372, 8373, 8374, 10850, 10851, 10852));
    public static final Set<Integer> SOTETSEG_IDS = ids(8387, 8388, 10867, 10868);

    // Chambers of Xeric
    public static final Set<Integer> TEKTON_IDS = ids(7540, 7543, 7544, 7545);
    public static final Set<Integer> GUARDIAN_IDS = ids(7569, 7571, 7570, 7572);
    public static final Set<Integer> OLM_HEAD_IDS = ids(7551, 7554);
    public static final Set<Integer> OLM_MELEE_HAND_IDS = ids(7552, 7555);
    public static final Set<Integer> OLM_MAGE_HAND_IDS = ids(7550, 7553);
    public static final Set<Integer> ABYSSAL_PORTAL_IDS = ids(7533);
    public static final Set<Integer> GLOWING_CRYSTAL_IDS = ids(7568);
    public static final Set<Integer> ICE_DEMON_IDS = ids(7584, 7585);
    public static final Set<Integer> VESPULA_IDS = ids(7530, 7531, 7532);

    public static final Set<Integer> FRAGMENT_OF_SEREN_IDS = ids(8917, 8918, 8919, 8920);
    public static final Set<Integer> NIGHTMARE_IDS = ids(
            378, 9425, 9426, 9427, 9428, 9429, 9430, 9431, 9432, 9433, 9460,
            377, 9423, 9416, 9417, 9418, 9419, 9420, 9421, 9422, 9424, 11153, 11154, 11155);
    public static final Set<Integer> NIGHTMARE_TOTEM_IDS = ids(9434, 9437, 9440, 9443, 9435, 9438, 9441, 9444);
    public static final Set<Integer> NEX_IDS = ids(11278, 11279, 11280, 11281, 11282);

    /** NPCs that calculate their magical defence using the defence stat. */
    public static final Set<Integer> USES_DEFENCE_LEVEL_FOR_MAGIC_DEFENCE_NPC_IDS;

    static {
        Set<Integer> s = new HashSet<>();
        s.addAll(ICE_DEMON_IDS);
        s.addAll(VERZIK_IDS);
        s.addAll(FRAGMENT_OF_SEREN_IDS);
        s.addAll(Arrays.asList(11709, 11712, 9118));
        USES_DEFENCE_LEVEL_FOR_MAGIC_DEFENCE_NPC_IDS = Collections.unmodifiableSet(s);
    }

    public static final Set<Integer> DUSK_IDS = ids(7851, 7854, 7855, 7882, 7883, 7886, 7887, 7888, 7889);
    public static final Set<Integer> WARRIORS_GUILD_CYCLOPES = ids(
            2463, 2465, 2467, 2464, 2466, 2468, 2137, 2138, 2139, 2140, 2141, 2142);
    public static final Set<Integer> ZULRAH_IDS = ids(2042, 2043, 2044);

    public static final Set<Integer> IMMUNE_TO_MELEE_DAMAGE_NPC_IDS;

    static {
        Set<Integer> s = new HashSet<>(Arrays.asList(494, 7706, 7708, 12214, 12215, 12219));
        s.addAll(ABYSSAL_PORTAL_IDS);
        s.addAll(ZULRAH_IDS);
        IMMUNE_TO_MELEE_DAMAGE_NPC_IDS = Collections.unmodifiableSet(s);
    }

    public static final Set<Integer> IMMUNE_TO_NON_SALAMANDER_MELEE_DAMAGE_NPC_IDS = ids(
            3169, 3170, 3171, 3172, 3173, 3174, 3175, 3176, 3177, 3178, 3179, 3180, 3181, 3182, 3183, 7037);

    public static final Set<Integer> IMMUNE_TO_RANGED_DAMAGE_NPC_IDS;
    public static final Set<Integer> IMMUNE_TO_BURN_DAMAGE_NPC_IDS;
    public static final Set<Integer> IMMUNE_TO_MAGIC_DAMAGE_NPC_IDS;

    static {
        Set<Integer> ranged = new HashSet<>();
        ranged.addAll(TEKTON_IDS);
        ranged.addAll(DUSK_IDS);
        ranged.addAll(GLOWING_CRYSTAL_IDS);
        ranged.addAll(WARRIORS_GUILD_CYCLOPES);
        IMMUNE_TO_RANGED_DAMAGE_NPC_IDS = Collections.unmodifiableSet(ranged);
        IMMUNE_TO_BURN_DAMAGE_NPC_IDS = IMMUNE_TO_RANGED_DAMAGE_NPC_IDS;

        Set<Integer> magic = new HashSet<>();
        magic.addAll(DUSK_IDS);
        magic.addAll(WARRIORS_GUILD_CYCLOPES);
        IMMUNE_TO_MAGIC_DAMAGE_NPC_IDS = Collections.unmodifiableSet(magic);
    }

    public static final Set<Integer> BA_ATTACKER_MONSTERS = ids(
            1667, 5739, 5740, 5741, 5742, 5743, 5744, 5745, 5746, 5747,
            1668, 5757, 5758, 5759, 5760, 5761, 5762, 5763, 5764, 5765);

    public static final Set<Integer> VARDORVIS_IDS = ids(12223, 12224, 12228, 12425, 12426, 13656);
    public static final Set<Integer> ARAXXOR_IDS = ids(13668);
    public static final Set<Integer> TITAN_BOSS_IDS = ids(12596, 14147);
    public static final Set<Integer> TITAN_ELEMENTAL_IDS = ids(14150, 14151);
    public static final Set<Integer> YAMA_IDS = ids(14176);
    public static final Set<Integer> YAMA_VOID_FLARE_IDS = ids(14179);
    public static final Set<Integer> HUEYCOATL_HEAD_IDS = ids(14009, 14010, 14013);
    public static final Set<Integer> HUEYCOATL_TAIL_IDS = ids(14014);
    public static final Set<Integer> HUEYCOATL_IDS = union(union(HUEYCOATL_HEAD_IDS, ids(14017)), HUEYCOATL_TAIL_IDS);
    public static final Set<Integer> ABYSSAL_SIRE_TRANSITION_IDS = ids(5886, 5889, 5891);
    public static final Set<Integer> DOOM_OF_MOKHAIOTL_IDS = ids(14707);
    public static final Set<Integer> ECLIPSE_MOON_IDS = ids(13012);
    public static final Set<Integer> INFINITE_HEALTH_MONSTERS = ids(14779);

    /** NPCs that always die in one hit from a player attack. */
    public static final Set<Integer> ONE_HIT_MONSTERS = ids(7223, 8584, 11193);

    /** NPCs the player always max hits against when using the matching combat style. */
    public static final Set<Integer> ALWAYS_MAX_HIT_MELEE = union(ids(11710, 11713, 12814), union(TOA_WARDEN_CORE_EJECTED_IDS, YAMA_VOID_FLARE_IDS));
    public static final Set<Integer> ALWAYS_MAX_HIT_RANGED = union(ids(11711, 11714, 12815, 11717, 11715), YAMA_VOID_FLARE_IDS);
    public static final Set<Integer> ALWAYS_MAX_HIT_MAGIC = union(ids(11709, 11712, 12816, 14151, 14150), YAMA_VOID_FLARE_IDS);

    /** NPCs the player has 100% accuracy against. */
    public static final Set<Integer> GUARANTEED_ACCURACY_MONSTERS = ids(5916);
}
