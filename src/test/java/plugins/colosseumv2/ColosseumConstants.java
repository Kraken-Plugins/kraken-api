package plugins.colosseumv2;

import net.runelite.api.Prayer;
import net.runelite.api.gameval.NpcID;

import java.util.Map;
import java.util.Set;

/**
 * Game constants for the Fortis Colosseum. NPC, animation, and spot animation ids are
 * sourced from the RuneLite gameval cache exports; timing constants mirror
 * {@link com.kraken.api.simulation.colosim.Simulation}.
 */
public final class ColosseumConstants {

    private ColosseumConstants() {
    }

    // =====================
    // Regions / state
    // =====================
    public static final int REGION_LOBBY = 7316;
    public static final int REGION_COLOSSEUM = 7216;

    /** Script fired when the modifier selection interface initializes. */
    public static final int SCRIPT_MODIFIER_SELECT_INIT = 4931;

    /** Varbit holding the index (1-3) of the modifier chosen on the selection interface. */
    public static final int VARBIT_MODIFIER_SELECTED = 9788;

    /** Minimus, despawns once a modifier has been selected. */
    public static final int NPC_MINIMUS = NpcID.COLOSSEUM_MASTER;

    public static final String CHAT_SOL_JUMPS_DOWN = "Sol Heredit jumps down from his seat";
    public static final String CHAT_WAVE_PREFIX = "Wave: ";
    public static final String CHAT_WAVE_COMPLETED = "completed!";

    // =====================
    // NPC ids
    // =====================
    public static final int JAGUAR_WARRIOR = NpcID.COLOSSEUM_JAGUAR_WARRIOR;            // 12810
    public static final int SERPENT_SHAMAN = NpcID.COLOSSEUM_STANDARD_MAGER;            // 12811
    public static final int MINOTAUR = NpcID.COLOSSEUM_MINOTAUR;                        // 12812
    public static final int MINOTAUR_ROUTEFIND = NpcID.COLOSSEUM_MINOTAUR_ROUTEFIND;    // 12813 (Red Flag)
    public static final int WARBAND_ARCHER = NpcID.COLOSSEUM_WARBANDER_RANGED_FEMALE;   // 12814
    public static final int WARBAND_SEER = NpcID.COLOSSEUM_WARBANDER_MAGE_MALE;         // 12815
    public static final int WARBAND_BERSERKER = NpcID.COLOSSEUM_WARBANDER_MELEE_MALE;   // 12816
    public static final int JAVELIN_COLOSSUS = NpcID.COLOSSEUM_JAVELIN_COLOSSUS;        // 12817
    public static final int MANTICORE = NpcID.COLOSSEUM_MANTICORE;                      // 12818
    public static final int SHOCKWAVE_COLOSSUS = NpcID.COLOSSEUM_SHOCKWAVE_COLOSSUS;    // 12819
    public static final int SOL_HEREDIT = NpcID.COLOSSEUM_SOL_P1;                       // 12821
    public static final int BEES = NpcID.COLOSSEUM_MODIFIER_BEES;                       // 12823
    public static final int HEALING_TOTEM = NpcID.COLOSSEUM_HEALING_TOTEM;              // 12825

    /**
     * All NPC ids tracked by the engine. Non-prayable NPCs (warband, Sol, healing totem) are
     * tracked so movement predictions account for them blocking other NPCs (corner traps).
     * Bees are excluded: they have no tile collision and cannot be prayed against.
     */
    public static final Set<Integer> TRACKED_NPC_IDS = Set.of(
            JAGUAR_WARRIOR, SERPENT_SHAMAN, MINOTAUR, MINOTAUR_ROUTEFIND,
            WARBAND_ARCHER, WARBAND_SEER, WARBAND_BERSERKER,
            JAVELIN_COLOSSUS, MANTICORE, SHOCKWAVE_COLOSSUS, SOL_HEREDIT, HEALING_TOTEM
    );

    // =====================
    // Animations (gameval AnimationID)
    // =====================
    public static final int ANIM_SHAMAN_CAST = 10859;               // NPC_SERPENT_MAGER_CASTING
    public static final int ANIM_JAGUAR_ATTACK = 10847;             // NPC_JAGUAR_RANGER_CLAWS_ATTACK
    public static final int ANIM_MINOTAUR_MELEE = 10843;            // NPC_MINOTAUR_BOSS_ATTACK_MELEE
    public static final int ANIM_MINOTAUR_HEAL = 10844;             // NPC_MINOTAUR_BOSS_ATTACK_MAGIC (heals allies, not an attack)
    public static final int ANIM_JAVELIN_RANGE = 10892;             // NPC_COLOSSI_JAVELIN_01_RANGE_ATTACK
    public static final int ANIM_JAVELIN_ARTILLERY = 10893;         // NPC_COLOSSI_JAVELIN_01_ARTILLERY_ATTACK (prayer-ignoring)
    public static final int ANIM_SHOCKWAVE_CLAP = 10903;            // NPC_COLOSSI_SHOCKWAVE_01_CLAPATTACK
    public static final int ANIM_MANTICORE_CHARGE = 10868;          // NPC_MANTICORE_01_TRIPLE_CHARGE
    public static final int ANIM_MANTICORE_THROW = 10869;           // NPC_MANTICORE_01_TRIPLE_THROW

    // =====================
    // Manticore orb spot animations (gameval SpotanimID)
    // =====================
    public static final int GRAPHIC_MANTICORE_MAGE = 2681;          // VFX_MANTICORE_01_PROJECTILE_MAGIC_01
    public static final int GRAPHIC_MANTICORE_RANGE = 2683;         // VFX_MANTICORE_01_PROJECTILE_RANGED_01
    public static final int GRAPHIC_MANTICORE_MELEE = 2685;         // VFX_MANTICORE_01_PROJECTILE_MELEE_01

    // =====================
    // Timing (mirrors com.kraken.api.simulation.colosim.Simulation)
    // =====================
    /** Ticks between a manticore starting to charge and launching its first orb. */
    public static final int MANTICORE_CHARGE_TICKS = 10;

    /** Orbs in a manticore volley, landing on consecutive ticks. */
    public static final int MANTICORE_VOLLEY_SIZE = 3;

    /** Cooldown applied to a ready manticore when a different manticore fires first. */
    public static final int MANTICORE_STAGGER_DELAY = 5;

    /** Ticks after a wave starts before enemies may attack. */
    public static final int DELAY_FIRST_ATTACK_TICKS = 3;

    /** Max hit of a single manticore orb, used for same-tick prayer conflict resolution. */
    public static final int MANTICORE_ORB_MAX_HIT = 36;

    /** Horizon (ticks) for forward movement simulation / jaguar attack prediction. */
    public static final int PATH_PREDICTION_HORIZON = 30;

    /**
     * Generic protection prayer to hold at the start of each wave, countering enemies that
     * spawn with immediate line of sight. Keyed by wave number.
     */
    public static final Map<Integer, Prayer> WAVE_PRE_PRAYER_MAP = Map.ofEntries(
            Map.entry(1, Prayer.PROTECT_FROM_MAGIC),
            Map.entry(2, Prayer.PROTECT_FROM_MISSILES),
            Map.entry(3, Prayer.PROTECT_FROM_MISSILES),
            Map.entry(4, Prayer.PROTECT_FROM_MAGIC),
            Map.entry(5, Prayer.PROTECT_FROM_MISSILES),
            Map.entry(6, Prayer.PROTECT_FROM_MISSILES),
            Map.entry(7, Prayer.PROTECT_FROM_MAGIC),
            Map.entry(8, Prayer.PROTECT_FROM_MAGIC),
            Map.entry(9, Prayer.PROTECT_FROM_MISSILES),
            Map.entry(10, Prayer.PROTECT_FROM_MISSILES),
            Map.entry(11, Prayer.PROTECT_FROM_MAGIC)
    );
}
