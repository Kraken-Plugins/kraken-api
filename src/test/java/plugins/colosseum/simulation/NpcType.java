package plugins.colosseum.simulation;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Combat and behaviour definitions for every simulated Fortis Colosseum wave NPC
 * (Sol Heredit is intentionally excluded).
 *
 * <p>Stats sourced from the OSRS Wiki monster pages (July 2026): hitpoints, footprint size,
 * attack style/range/speed and max hits. Behavioural details (manticore charge and stagger,
 * javelin sky special cadence, minotaur heal rule, warband route-finding and 6-tick global
 * attack cycle) come from the wiki strategy pages and the community wave simulator that this
 * engine's tick loop is parity-tested against.</p>
 */
@Getter
@AllArgsConstructor
public enum NpcType {
    /**
     * Manticore: charges a triple attack (10 ticks) on first line of sight, then fires three
     * orbs on consecutive ticks - ranged/magic in a fixed per-spawn order, melee always last
     * (Mantimayhem III unlocks melee-first patterns). Only one manticore may begin firing per
     * tick; other ready manticores are delayed 5 ticks. Prayer is checked on each orb's launch
     * tick (projectiles have no travel time).
     */
    MANTICORE(new int[]{12818}, "Manticore", 0, 3, 15, 10, 36, 250, NpcAttackStyle.RANGED, false, SpecialKind.MANTICORE),

    /** Serpent shaman: accurate 10-range Water Surge caster, no special mechanics. */
    SERPENT_SHAMAN(new int[]{12811}, "Serpent shaman", 1, 1, 10, 5, 28, 125, NpcAttackStyle.MAGIC, false, SpecialKind.NONE),

    /**
     * Javelin colossus: 15-range thrower. Every fifth attack is a sky javelin aimed at the
     * player's tile that lands 6 ticks later for up to 40 typeless damage - dodged by moving,
     * not by prayer.
     */
    JAVELIN_COLOSSUS(new int[]{12817}, "Javelin Colossus", 2, 3, 15, 5, 48, 220, NpcAttackStyle.RANGED, false, SpecialKind.JAVELIN_SKY),

    /** Shockwave colossus: hard-hitting 15-range mage, no special mechanics. */
    SHOCKWAVE_COLOSSUS(new int[]{12819}, "Shockwave Colossus", 3, 3, 15, 5, 56, 125, NpcAttackStyle.MAGIC, false, SpecialKind.NONE),

    /** Jaguar warrior: melee reinforcement that rolls three independent hits per attack. */
    JAGUAR_WARRIOR(new int[]{12810}, "Jaguar warrior", 4, 2, 1, 5, 47, 125, NpcAttackStyle.MELEE, false, SpecialKind.NONE),

    /**
     * Minotaur: melee reinforcement. When no player is in melee reach on its attack timer it
     * scans for a damaged NPC (below 75% max HP, centre within 7 tiles, line of sight, not a
     * minotaur) and heals it instead. Its melee damage is applied one tick after the attack.
     */
    MINOTAUR(new int[]{12812}, "Minotaur", 0, 3, 1, 5, 74, 225, NpcAttackStyle.MELEE, false, SpecialKind.MINOTAUR_HEAL),

    /** Red Flag minotaur variant: identical stats but route-finds around pillars. */
    MINOTAUR_RED_FLAG(new int[]{12813}, "Minotaur (Red Flag)", 0, 3, 1, 5, 74, 225, NpcAttackStyle.MELEE, true, SpecialKind.MINOTAUR_HEAL),

    /**
     * Fremennik berserker: route-finds around pillars, attacks only from melee reach while
     * stationary, on the shared warband 6-tick cycle. Weak to magic (one-shot by powered staves).
     */
    FREMENNIK_BERSERKER(new int[]{12816}, "Fremennik warband berserker", 6, 1, 1, 6, 29, 48, NpcAttackStyle.MELEE, true, SpecialKind.WARBAND),

    /** Fremennik seer: magic damage but otherwise identical warband behaviour; weak to ranged. */
    FREMENNIK_SEER(new int[]{12815}, "Fremennik warband seer", 7, 1, 1, 6, 12, 50, NpcAttackStyle.MAGIC, true, SpecialKind.WARBAND),

    /** Fremennik archer: ranged damage but otherwise identical warband behaviour; weak to melee. */
    FREMENNIK_ARCHER(new int[]{12814}, "Fremennik warband archer", 8, 1, 1, 6, 14, 50, NpcAttackStyle.RANGED, true, SpecialKind.WARBAND);

    /** Special behaviour hooks the tick engine dispatches on. */
    public enum SpecialKind {
        NONE,
        MANTICORE,
        JAVELIN_SKY,
        MINOTAUR_HEAL,
        WARBAND
    }

    private final int[] npcIds;
    private final String displayName;
    /**
     * Relative processing order within a tick, matching the validated community simulator
     * (manticores first, then shamans, colossi, jaguar, minotaur, warband last). Determines
     * which NPC wins contested movement.
     */
    private final int processingPriority;
    private final int size;
    private final int attackRange;
    private final int attackSpeedTicks;
    private final int maxHit;
    private final int maxHitpoints;
    private final NpcAttackStyle attackStyle;
    private final boolean smartPathing;
    private final SpecialKind specialKind;

    /**
     * Resolves a colosseum NPC type from a RuneLite NPC id.
     *
     * @param npcId RuneLite npc id.
     * @return matching type, or null when the id is not a simulated wave NPC.
     */
    public static NpcType fromNpcId(int npcId) {
        for (NpcType type : values()) {
            for (int id : type.npcIds) {
                if (id == npcId) {
                    return type;
                }
            }
        }
        return null;
    }

    /** @return true when this NPC is one of the three (or four) Fremennik warband members. */
    public boolean isWarband() {
        return specialKind == SpecialKind.WARBAND;
    }

    /** @return true when this NPC attacks with melee reach only. */
    public boolean isMeleeReach() {
        return attackRange <= 1;
    }
}
