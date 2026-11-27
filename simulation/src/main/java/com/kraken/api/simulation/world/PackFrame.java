package com.kraken.api.simulation.world;

/**
 * A content pack's part of a {@link Frame}: per capture context only that pack reads (its modifiers, its scripted
 * spawn slots), reached from {@link Frame#pack}. Immutable once the frame is built, like the rest of the frame.
 */
public interface PackFrame {
    /**
     * A copy that does not share mutable arrays with this one.
     * @return the copy
     */
    PackFrame copy();

    /**
     * Overwrites this pack frame with another of the same class.
     * @param other the frame part to copy
     */
    void copyFrom(PackFrame other);
}
