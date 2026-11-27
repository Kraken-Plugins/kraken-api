package com.kraken.api.simulation.colosseum;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * The four Solarflare flares as a pure function of one packed clock: ring index | ticks until the next step
 * {@code << 5}. Kept in {@link Lanes#SOLAR}.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Solar {
    private static final int IDX_MASK = 0x1f;

    /** The flares' state on wave tick 0: on their start corners, leaving them after the corner gap (the first step's
     *  timing is inferred, modifiers.json). */
    public static int start(Mods mods) {
        return pack(0, Math.max(1, mods.solarCornerGap));
    }

    public static int pack(int ringIdx, int ticksToStep) {
        return (ringIdx & IDX_MASK) | ticksToStep << 5;
    }

    public static int index(int packed) {
        return packed & IDX_MASK;
    }

    /** The tile of flare {@code flare} (0 to 3, one per pillar) for a packed flare state. */
    public static int tile(ColosseumData col, int packed, int flare) {
        return col.solarRings[flare][packed & IDX_MASK];
    }

    /** True when any flare sits on the packed tile. */
    public static boolean on(ColosseumData col, int packed, int tile) {
        int idx = packed & IDX_MASK;
        for (short[] ring : col.solarRings) {
            if (ring[idx] == tile) {
                return true;
            }
        }
        return false;
    }

    /**
     * One tick of the flares: a step every side gap along a side, and the corner gap on leaving a corner (capture:
     * I 2 and 7, II 2 and 2, III 1 and 2, timed from the last step).
     */
    public static int step(int packed, Mods mods) {
        int idx = packed & IDX_MASK;
        int wait = packed >>> 5;
        if (--wait <= 0) {
            idx = (idx + 1) % ColosseumData.SOLAR_RING;
            wait = idx % ColosseumData.SOLAR_SIDE == 0 ? mods.solarCornerGap : mods.solarSideGap;
            wait = Math.max(1, wait);
        }
        return pack(idx, wait);
    }

    /**
     * Re anchors the flares' clock to an observed flare tile: keeps a clock already at the observed ring index (its
     * step countdown intact), and otherwise restarts it at that index. A tile on no flare's ring leaves the clock
     * alone.
     */
    public static int reanchor(ColosseumData col, Mods mods, int clock, int observedPos) {
        int idx = col.solarOrbitIndex(observedPos);
        if (idx < 0 || (clock >= 0 && index(clock) == idx)) {
            return clock;
        }
        int wait = idx % ColosseumData.SOLAR_SIDE == 0 ? mods.solarCornerGap : mods.solarSideGap;
        return pack(idx, Math.max(1, wait));
    }
}
