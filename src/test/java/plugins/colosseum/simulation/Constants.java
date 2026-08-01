package plugins.colosseum.simulation;

/**
 * Timing and behaviour constants for the Fortis Colosseum simulation.
 *
 * <p>Values come from the OSRS Wiki and the community wave simulator this engine is
 * parity-tested against. Anything marked as an estimate is called out explicitly.</p>
 */
public final class Constants {
    /** Ticks a manticore takes to charge its triple attack after first gaining line of sight. */
    public static final int MANTICORE_CHARGE_TICKS = 10;

    /** Delay added to other ready manticores when one manticore begins firing. */
    public static final int MANTICORE_STAGGER_TICKS = 5;

    /** Number of orbs in a manticore triple attack, launched on consecutive ticks. */
    public static final int MANTICORE_ORB_COUNT = 3;

    /** Wave-start gating (validated by the community simulator): no NPC movement on tick 0. */
    public static final int WAVE_START_MOVE_GATE_TICKS = 1;

    /** Wave-start gating: NPCs cannot acquire line of sight before tick 2. */
    public static final int WAVE_START_LOS_GATE_TICKS = 2;

    /** Wave-start gating: NPCs cannot attack before tick 3. */
    public static final int WAVE_START_ATTACK_GATE_TICKS = 3;

    /** Every N-th javelin colossus attack is the undodgeable-by-prayer sky javelin. */
    public static final int JAVELIN_SPECIAL_EVERY_N_ATTACKS = 5;

    /** Ticks between a sky javelin launch and it landing on the targeted tile. */
    public static final int JAVELIN_SKY_LAND_DELAY_TICKS = 6;

    /** Max typeless damage from a sky javelin when standing on the targeted tile. */
    public static final int JAVELIN_SKY_MAX_DAMAGE = 40;

    /** Minotaur heal: target centre must be within this many tiles of the minotaur centre. */
    public static final int MINOTAUR_HEAL_RANGE = 7;

    /** Minotaur heal: only NPCs below this fraction of max hitpoints are healed. */
    public static final double MINOTAUR_HEAL_THRESHOLD = 0.75;

    /**
     * Hitpoints restored per minotaur heal cycle. The wiki states it "heals continuously to
     * full" without a number; this per-cycle estimate is deliberately high so plans respect
     * that a healing minotaur nullifies damage on its target.
     */
    public static final int MINOTAUR_HEAL_PER_CYCLE = 30;

    /** Minotaur melee damage is applied this many ticks after its attack (tick-eatable). */
    public static final int MINOTAUR_HIT_DELAY_TICKS = 1;

    /** Reinforcements arrive this many ticks (40 seconds) after the wave starts. */
    public static final int REINFORCEMENT_TICKS = 67;

    /** Default warband attack-cycle phase: attacks land on ticks where tick % 6 == phase. */
    public static final int DEFAULT_WARBAND_CYCLE_PHASE = 3;

    /** Warband members share a fixed 6-tick attack cycle relative to wave start. */
    public static final int WARBAND_CYCLE_TICKS = 6;

    /** Fortis Colosseum arena region id. */
    public static final int ARENA_REGION_ID = 7216;

    /** Prayer drain-effect points per tick for one active overhead protection prayer. */
    public static final int PROTECTION_PRAYER_DRAIN_EFFECT = 12;

    /** Special attack energy regeneration interval in ticks (10% per interval). */
    public static final int SPEC_REGEN_INTERVAL_TICKS = 50;

    /** Ring buffer capacity for in-flight hits; oldest entries are dropped beyond this. */
    public static final int MAX_PENDING_HITS = 24;

    /** Maximum NPC slots a single simulation state tracks. */
    public static final int MAX_NPCS = 16;

    private Constants() {
    }
}
