package com.kraken.api.simulation.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.kraken.api.simulation.hits.Style;
import com.kraken.api.simulation.npc.NpcBehavior;

import java.util.Map;

/**
 * One NPC type, loaded from a content pack's npcs.json. The fields here are the mechanics every pack shares; a pack's
 * own per type settings sit in a block named after the pack (read through {@link #block}). Values carry provenance in
 * the JSON ({@code src} maps). Type index = position in the npcs.json array.
 */
public final class NpcDef {
    /** Damage lands {@link #hitDelayTicks} after launch at every distance. */
    public static final int DELAY_FIXED = 0;
    /**
     * Projectile curve: the hit lands {@code (max(projectileMin, projectileBase + projectilePerTile * d)
     * + projectileOffset) / 30} ticks after launch, d the Chebyshev distance at launch (the client's projectile end
     * time in cycles of 1/30 tick).
     */
    public static final int DELAY_PROJECTILE = 1;

    /** Prayer read at the attack tick only. */
    public static final int PRAYER_AT_LAUNCH = 0;
    /** Prayer must be up at the attack tick and at landing. */
    public static final int PRAYER_LAUNCH_AND_LAND = 1;

    /** Stable type key that npcs.json, content packs and traces name the type by. */
    public String key;
    /** Display name of the type. */
    public String name;
    /** Live game NPC ids that map to this type. */
    public int[] ids = {};
    /** Footprint edge length in tiles (the footprint is size x size). */
    public int size = 1;
    /** Standard attack range in tiles; 1 means melee adjacency. */
    public int attackRange = 1;
    /** Ticks between standard attacks. */
    public int attackSpeed = 5;
    /** Tiles moved per tick. */
    public int moveSpeed = 1;
    /** Highest damage one roll of the standard attack can deal. */
    public int maxHit;
    /** Damage rolls per standard attack, each up to {@link #maxHit} (a triple claw is 3). */
    public int rolls = 1;
    /** Maximum hit points. */
    public int hp;
    /** True when the type moves by route finding instead of the naive chase. */
    public boolean smartPath;
    /** Live ids that imply route finding for this type (a variant spawned by a modifier). */
    public int[] smartIds = {};
    /** The pack behaviour this type binds to; "none" takes the core's standard turn. */
    public String behavior = "none";
    /**
     * Attack style name read by {@link Style#parse}: "ranged", "magic" or "melee"; any other name (a volley whose
     * orbs carry their own styles) parses to -1.
     */
    public String style = "melee";
    /** "fixed" (with {@link #hitDelayTicks}) or "projectile" (with the projectile fields). */
    public String hitDelay = "fixed";
    /** Ticks from launch to landing when {@link #hitDelay} is "fixed". */
    public int hitDelayTicks;
    /** Projectile curve base duration in cycles of 1/30 tick. */
    public int projectileBase;
    /** Projectile curve cycles added per tile of Chebyshev distance. */
    public int projectilePerTile;
    /** Floor in cycles for the base plus distance term of the projectile curve. */
    public int projectileMin;
    /** Cycles added after the floor of the projectile curve is applied. */
    public int projectileOffset;
    /** "launch" or "launchAndLand". */
    public String prayerCheck = "launch";
    /**
     * Ticks from the killing hit until the body is removed. The body blocks other NPCs until it is deleted on its own
     * turn, so an NPC earlier in the turn order first steps onto it a tick later; 0 = removed at once.
     */
    public int corpseTicks = 4;
    /** Moves through other NPCs and does not block them. */
    public boolean passThrough;
    /** No occupancy footprint at all (a swarm hazard); ignores collision. */
    public boolean noBody;
    /** False for spawns that do not hold a wave open (modifier hazards, healers). */
    public Boolean countsForWaveClear;
    /** Genuine attack launch animation ids; empty = unclassified, every anim counts. */
    public int[] attackAnims = {};
    /** Block or defend animation ids the live client plays when this NPC is hit; never attacks. */
    public int[] defendAnims = {};
    /** Per type first step gate override (ticks after spawn visible); null = pipeline default. */
    public Integer gateMoveTick;
    /** Provenance notes keyed by field name. */
    public Map<String, String> src;

    /** Parsed {@link #style}: a {@link Style} code, or -1 for a name the core does not know. */
    public transient int styleCode;
    /** Parsed {@link #hitDelay}: {@link #DELAY_FIXED} or {@link #DELAY_PROJECTILE}. */
    public transient int hitDelayCode;
    /** Parsed {@link #prayerCheck}: {@link #PRAYER_AT_LAUNCH} or {@link #PRAYER_LAUNCH_AND_LAND}. */
    public transient int prayerCheckCode;
    /** True when a living NPC of this type holds the wave open (from {@link #countsForWaveClear}). */
    public transient boolean waveClearing;
    /** The behaviour the pack bound, or null for the standard turn. */
    public transient NpcBehavior behaviorImpl;
    /** {@link NpcBehavior} hook bits the bound behaviour overrides. */
    public transient int hookMask;
    /** The type's raw JSON entry, kept for the pack's own block. */
    public transient JsonObject source;

    void finish() {
        styleCode = Style.parse(style);
        switch (hitDelay) {
            case "projectile":
                hitDelayCode = DELAY_PROJECTILE;
                break;
            case "fixed":
                hitDelayCode = DELAY_FIXED;
                break;
            default:
                throw new IllegalArgumentException("unknown hitDelay '" + hitDelay + "' for " + key);
        }
        prayerCheckCode = "launchAndLand".equals(prayerCheck) ? PRAYER_LAUNCH_AND_LAND : PRAYER_AT_LAUNCH;
        waveClearing = countsForWaveClear == null || countsForWaveClear;
    }

    /**
     * The pack's own settings for this type.
     * @param pack the block name, the pack's name
     * @return the block, or null when the entry has none
     */
    public JsonObject block(String pack) {
        JsonElement e = source == null ? null : source.get(pack);
        return e != null && e.isJsonObject() ? e.getAsJsonObject() : null;
    }

    /**
     * Ticks from launch to the hitsplat for this NPC's standard attack at the given Chebyshev distance.
     * @param chebyshevDist the distance from the player to the nearest footprint tile at launch
     * @return the delay in ticks
     */
    public int hitDelayTicks(int chebyshevDist) {
        if (hitDelayCode == DELAY_PROJECTILE) {
            return (Math.max(projectileMin, projectileBase + projectilePerTile * chebyshevDist) + projectileOffset) / 30;
        }
        return hitDelayTicks;
    }

    /**
     * Whether an animation is one of the type's defend animations.
     * @param animId the animation id
     * @return true for a defend animation
     */
    public boolean isDefendAnim(int animId) {
        return contains(defendAnims, animId);
    }

    /**
     * True when the anim is a classified attack, or the type has no classification yet (empty list = every non
     * defend anim counts as an attack).
     * @param animId the animation id
     * @return true when the animation counts as an attack launch
     */
    public boolean isAttackAnim(int animId) {
        return attackAnims.length == 0 || contains(attackAnims, animId);
    }

    /**
     * First step gate in ticks after the spawn visible tick.
     * @param flags the pipeline flags holding the default
     * @return the per type override, or the pipeline default
     */
    public int moveGate(PipelineFlags flags) {
        return gateMoveTick != null ? gateMoveTick : flags.gateMoveTick;
    }

    /**
     * Whether an NPC of this type with a given live id route finds.
     * @param liveId the live NPC id
     * @return true for smart path types and their smart variants
     */
    public boolean smartFor(int liveId) {
        return smartPath || contains(smartIds, liveId);
    }

    /**
     * Whether an id list holds an id.
     * @param ids the list
     * @param id the id
     * @return true when present
     */
    public static boolean contains(int[] ids, int id) {
        for (int candidate : ids) {
            if (candidate == id) {
                return true;
            }
        }
        return false;
    }
}
