package com.kraken.api.simulation.colosseum;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Bit-packing helpers for arena-local tile coordinates.
 *
 * <p>The colosseum simulation works in arena-local coordinates where {@code (0, 0)} is the
 * south-west corner of the captured grid and both axes fit in 6 bits (max grid 64x64). A
 * position is packed as {@code x | (y << 6)} into the low 12 bits of a short, which keeps
 * simulation state copies down to primitive array copies with no object churn.</p>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ColoCoords {
    /** Sentinel packed position meaning "none". */
    public static final short NONE = -1;

    /** Maximum grid dimension supported by the 6-bit packing. */
    public static final int MAX_DIMENSION = 64;

    private static final int AXIS_MASK = 0x3F;

    /**
     * Packs local coordinates into a short.
     *
     * @param x local x, 0-63.
     * @param y local y, 0-63.
     * @return packed position.
     */
    public static short pack(int x, int y) {
        return (short) ((x & AXIS_MASK) | ((y & AXIS_MASK) << 6));
    }

    /**
     * @param packed packed position.
     * @return local x.
     */
    public static int x(short packed) {
        return packed & AXIS_MASK;
    }

    /**
     * @param packed packed position.
     * @return local y.
     */
    public static int y(short packed) {
        return (packed >> 6) & AXIS_MASK;
    }

    /**
     * @param packed packed position.
     * @return true when the position is a real tile rather than {@link #NONE}.
     */
    public static boolean isPresent(short packed) {
        return packed >= 0;
    }

    /**
     * Chebyshev distance between two packed positions.
     *
     * @param a first packed position.
     * @param b second packed position.
     * @return max axis distance.
     */
    public static int chebyshev(short a, short b) {
        int dx = Math.abs(x(a) - x(b));
        int dy = Math.abs(y(a) - y(b));
        return Math.max(dx, dy);
    }

    /**
     * Offsets a packed position. Callers must keep the result in bounds.
     *
     * @param packed packed position.
     * @param dx x delta.
     * @param dy y delta.
     * @return packed offset position.
     */
    public static short offset(short packed, int dx, int dy) {
        return pack(x(packed) + dx, y(packed) + dy);
    }
}
