package com.kraken.api.simulation.hits;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Attack style codes and the overhead protection that blocks each. Overheads use the {@code SimState.overhead}
 * encoding: 0 none, 1 melee, 2 missiles, 3 magic.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Style {
    /** Ranged attack style. */
    public static final int RANGED = 0;
    /** Magic attack style. */
    public static final int MAGIC = 1;
    /** Melee attack style. */
    public static final int MELEE = 2;
    /** Damage no protection prayer reduces (ground hazards, tile targeted tosses). */
    public static final int TYPELESS = 3;

    /** No protection prayer up. */
    public static final int OVERHEAD_NONE = 0;
    /** Protect from Melee. */
    public static final int OVERHEAD_MELEE = 1;
    /** Protect from Missiles. */
    public static final int OVERHEAD_MISSILES = 2;
    /** Protect from Magic. */
    public static final int OVERHEAD_MAGIC = 3;

    /**
     * The overhead that blocks a style.
     * @param style {@link #RANGED}, {@link #MAGIC} or {@link #MELEE}
     * @return the overhead code; melee for any other style
     */
    public static int overheadFor(int style) {
        switch (style) {
            case RANGED:
                return OVERHEAD_MISSILES;
            case MAGIC:
                return OVERHEAD_MAGIC;
            default:
                return OVERHEAD_MELEE;
        }
    }

    /**
     * Parses a style name from the data files.
     * @param name "ranged", "magic" or "melee"
     * @return the style code, or -1 for any other name (per orb styles such as a volley)
     */
    public static int parse(String name) {
        if (name == null) {
            return -1;
        }
        switch (name) {
            case "ranged":
                return RANGED;
            case "magic":
                return MAGIC;
            case "melee":
                return MELEE;
            default:
                return -1;
        }
    }
}
