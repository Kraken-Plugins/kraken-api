package com.kraken.api.simulation.hits;

import com.kraken.api.simulation.world.Frame;
import com.kraken.api.simulation.world.SimState;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * In flight hit encoding and the dual channel damage bookkeeping. A pending hit packs into one long: ticks until
 * landing (counted down at tick start, landing at 0 or less), style, per roll expected and worst damage, source
 * slot, a 12 bit target (a tile for tile targeted hits, an NPC slot for player hits), roll count, behaviour flags,
 * and for a tile targeted hit whose tile is not picked yet, the remaining delay at which it picks.
 *
 * <p>Prayer for an NPC attack usually resolves at launch, so a fully blocked launch is simply never pushed, and only
 * {@link #checkAtLand} hits carry a style to the landing tick. A {@link #tilePending} hit takes the player's tile
 * when its remaining delay reaches its reveal delay.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Hits {
    private static final int SHIFT_STYLE = 5;
    private static final int SHIFT_EXPECTED = 7;
    private static final int SHIFT_WORST = 16;
    private static final int SHIFT_SRC = 25;
    private static final int SHIFT_TARGET = 30;
    private static final int SHIFT_ROLLS = 42;
    private static final long FLAG_CHECK_AT_LAND = 1L << 44;
    private static final long FLAG_VS_NPC = 1L << 45;
    private static final long FLAG_HEAL = 1L << 46;
    private static final long FLAG_TILE_TARGETED = 1L << 47;
    private static final long FLAG_APPLY_VENOM = 1L << 48;
    private static final long FLAG_TILE_PENDING = 1L << 49;
    private static final int SHIFT_REVEAL = 50;
    private static final long TARGET_MASK = 0xfffL << SHIFT_TARGET;
    private static final long REVEAL_MASK = 0x1fL << SHIFT_REVEAL;

    /** Source slot of the player's own hits (outside the NPC slots). */
    public static final int SRC_PLAYER = 31;

    /**
     * Packs a hit.
     * @param delay ticks until it lands, 0 to 31
     * @param style its {@link Style}
     * @param expected per roll expected damage, 0 to 511
     * @param worst per roll worst case damage, 0 to 511
     * @param srcSlot the source slot, or {@link #SRC_PLAYER}
     * @param target an NPC slot or a packed tile
     * @param rolls damage rolls, 1 to 4
     * @return the packed hit
     */
    public static long encode(int delay, int style, int expected, int worst, int srcSlot, int target, int rolls) {
        return (delay & 0x1f)
                | (long) (style & 3) << SHIFT_STYLE
                | (long) (expected & 0x1ff) << SHIFT_EXPECTED
                | (long) (worst & 0x1ff) << SHIFT_WORST
                | (long) (srcSlot & 0x1f) << SHIFT_SRC
                | (long) (target & 0xfff) << SHIFT_TARGET
                | (long) ((rolls - 1) & 3) << SHIFT_ROLLS;
    }

    /**
     * Marks a hit the overhead must also block at landing.
     * @param hit the hit
     * @return the marked hit
     */
    public static long checkAtLand(long hit) {
        return hit | FLAG_CHECK_AT_LAND;
    }

    /**
     * Marks a hit that lands on the NPC slot in its target.
     * @param hit the hit
     * @return the marked hit
     */
    public static long vsNpc(long hit) {
        return hit | FLAG_VS_NPC;
    }

    /**
     * Marks a heal that lands on the NPC slot in its target, its expected damage being the amount.
     * @param hit the hit
     * @return the marked hit
     */
    public static long heal(long hit) {
        return hit | FLAG_VS_NPC | FLAG_HEAL;
    }

    /**
     * Marks a hit that lands on the tile in its target, hitting the player only when the player stands there.
     * @param hit the hit
     * @return the marked hit
     */
    public static long tileTargeted(long hit) {
        return hit | FLAG_TILE_TARGETED;
    }

    /**
     * Marks a hit that venoms the player when it lands.
     * @param hit the hit
     * @return the marked hit
     */
    public static long applyVenom(long hit) {
        return hit | FLAG_APPLY_VENOM;
    }

    /**
     * Marks a tile targeted hit whose tile is not picked yet: it takes the player's start of tick tile on the tick
     * its remaining delay falls to {@code revealDelay} (see {@link #pickTiles}).
     * @param hit the hit
     * @param revealDelay the remaining delay at which the tile is picked, 0 to 31
     * @return the marked hit
     */
    public static long tilePending(long hit, int revealDelay) {
        return hit | FLAG_TILE_TARGETED | FLAG_TILE_PENDING | (long) (revealDelay & 0x1f) << SHIFT_REVEAL;
    }

    /**
     * Whether a tile targeted hit still waits for its tile.
     * @param hit the hit
     * @return true while the tile is not picked
     */
    public static boolean isTilePending(long hit) {
        return (hit & FLAG_TILE_PENDING) != 0;
    }

    /**
     * Ticks until the hit lands.
     * @param hit the hit
     * @return the delay
     */
    public static int delayOf(long hit) {
        return (int) (hit & 0x1f);
    }

    /**
     * The hit's style.
     * @param hit the hit
     * @return its {@link Style}
     */
    public static int styleOf(long hit) {
        return (int) (hit >>> SHIFT_STYLE & 3);
    }

    /**
     * Per roll expected damage (the amount, for a heal).
     * @param hit the hit
     * @return the expected damage
     */
    public static int expectedOf(long hit) {
        return (int) (hit >>> SHIFT_EXPECTED & 0x1ff);
    }

    /**
     * Per roll worst case damage.
     * @param hit the hit
     * @return the worst case
     */
    public static int worstOf(long hit) {
        return (int) (hit >>> SHIFT_WORST & 0x1ff);
    }

    /**
     * The source slot.
     * @param hit the hit
     * @return the slot, or {@link #SRC_PLAYER}
     */
    public static int srcOf(long hit) {
        return (int) (hit >>> SHIFT_SRC & 0x1f);
    }

    /**
     * The target: an NPC slot or a packed tile.
     * @param hit the hit
     * @return the target
     */
    public static int targetOf(long hit) {
        return (int) (hit >>> SHIFT_TARGET & 0xfff);
    }

    /**
     * The number of damage rolls.
     * @param hit the hit
     * @return 1 to 4
     */
    public static int rollsOf(long hit) {
        return (int) (hit >>> SHIFT_ROLLS & 3) + 1;
    }

    /**
     * Whether the overhead is checked again at landing.
     * @param hit the hit
     * @return true for a check at land hit
     */
    public static boolean isCheckAtLand(long hit) {
        return (hit & FLAG_CHECK_AT_LAND) != 0;
    }

    /**
     * Whether the hit lands on an NPC.
     * @param hit the hit
     * @return true for player hits and NPC heals
     */
    public static boolean isVsNpc(long hit) {
        return (hit & FLAG_VS_NPC) != 0;
    }

    /**
     * Whether the hit is a heal.
     * @param hit the hit
     * @return true for a heal
     */
    public static boolean isHeal(long hit) {
        return (hit & FLAG_HEAL) != 0;
    }

    /**
     * Whether the hit lands on a tile.
     * @param hit the hit
     * @return true for a tile targeted hit, picked or not
     */
    public static boolean isTileTargeted(long hit) {
        return (hit & FLAG_TILE_TARGETED) != 0;
    }

    /**
     * Whether the hit venoms the player.
     * @param hit the hit
     * @return true when it applies venom
     */
    public static boolean isApplyVenom(long hit) {
        return (hit & FLAG_APPLY_VENOM) != 0;
    }

    /**
     * Adds a hit to the state's in flight ring.
     * @param st the state
     * @param hit the hit
     */
    public static void push(SimState st, long hit) {
        if (st.pendingCount == SimState.PENDING_HITS) {
            throw new IllegalStateException("pending-hit ring full");
        }
        st.pendingHits[st.pendingCount++] = hit;
    }

    /**
     * Counts every in flight hit one tick closer to landing; runs once at tick start.
     * @param st the state
     */
    public static void ageAll(SimState st) {
        for (int i = 0; i < st.pendingCount; i++) {
            long h = st.pendingHits[i];
            int delay = (int) (h & 0x1f);
            if (delay > 0) {
                st.pendingHits[i] = (h & ~0x1fL) | (delay - 1);
            }
        }
    }

    /**
     * Picks the tile of every pending tile targeted hit whose remaining delay is down to its reveal delay: the tile
     * the player stands on at the start of this tick, the same tile NPCs judge this tick.
     * @param st the state
     */
    public static void pickTiles(SimState st) {
        for (int i = 0; i < st.pendingCount; i++) {
            long h = st.pendingHits[i];
            if ((h & FLAG_TILE_PENDING) != 0 && (int) (h & 0x1f) <= (int) (h >>> SHIFT_REVEAL & 0x1f)) {
                st.pendingHits[i] = (h & ~(FLAG_TILE_PENDING | TARGET_MASK | REVEAL_MASK))
                        | (long) (st.playerPos & 0xfff) << SHIFT_TARGET;
            }
        }
    }

    /**
     * Unprayed expected damage factor x1000 for a frame: the pipeline's, adjusted by the frame's damage modifiers.
     * @param f the frame
     * @return the factor
     */
    public static int unprayedFactor1000(Frame f) {
        DamageModifiers d = f.damage;
        return d.unprayedFactorOverride1000 >= 0 ? d.unprayedFactorOverride1000
                : f.data.flags.unprayedExpectedFactor1000 + d.unprayedFactorAdd1000;
    }

    /**
     * Applies one damage event to the player through both channels: expected damage moves simulated HP (and marks
     * the state dead at zero), the worst case roll accumulates into this tick's burst for the HP floor, and the
     * frame's prayer drain and stack couplings fire per instance.
     * @param st the state
     * @param expected expected damage
     * @param worst worst case damage
     * @param instances damage instances (rolls)
     */
    public static void damagePlayer(SimState st, int expected, int worst, int instances) {
        DamageModifiers d = st.frame.damage;
        st.tickBurstWorst += (short) worst;
        if (expected > 0) {
            st.tickExpectedDamage += (short) expected;
            st.accExpectedDamageTaken += expected;
            if (!st.died && st.hpExpected - expected <= 0) {
                st.died = true;
                st.deathTick = (short) st.tick;
            }
            st.hpExpected = (short) Math.max(0, st.hpExpected - expected);
            if (d.prayerDrainPctOfDamage > 0) {
                int drain = expected * d.prayerDrainPctOfDamage / 100;
                int drained = Math.min(drain, st.prayerExpected);
                st.prayerExpected -= (short) drained;
                st.accPrayerDrained += drained;
            }
        }
        if (d.stackPerHit && worst > 0) {
            st.hitStacksWorst += (byte) instances;
        }
    }
}
