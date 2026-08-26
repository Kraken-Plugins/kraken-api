package com.kraken.api.service.walker.transport;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.gameval.InterfaceID;

/**
 * Resolves fairy ring codes to the interface components that select them.
 *
 * <p>The client exposes one component per code in the travel log, named after the code itself, and
 * one per dial letter in the configuration panel. Looking them up by name keeps sixty four ids out of
 * this repository and lets the mapping follow the client version rather than a copied table.</p>
 */
@Slf4j
public final class FairyRingWidgets {

    /** Returned when a code has no matching component. */
    public static final int NOT_FOUND = -1;

    private FairyRingWidgets() {
    }

    /**
     * Finds the travel log entry for a code.
     *
     * @param code a three letter code such as {@code "ALQ"}
     * @return the packed component id, or {@link #NOT_FOUND}
     */
    public static int logEntry(String code) {
        return isValidCode(code) ? lookup(InterfaceID.FairyringsLog.class, code) : NOT_FOUND;
    }

    /**
     * Finds the configuration panel's button for one dial letter.
     *
     * @param letter a single dial letter
     * @return the packed component id, or {@link #NOT_FOUND}
     */
    public static int dialLetter(char letter) {
        return lookup(InterfaceID.Fairyrings.class, String.valueOf(Character.toUpperCase(letter)));
    }

    /**
     * Reports whether a code is well formed, one letter from each dial.
     *
     * @param code the code to check, may be null
     * @return true when the code names a real ring
     */
    public static boolean isValidCode(String code) {
        if (code == null || code.length() != 3) {
            return false;
        }

        char first = Character.toUpperCase(code.charAt(0));
        char second = Character.toUpperCase(code.charAt(1));
        char third = Character.toUpperCase(code.charAt(2));

        return first >= 'A' && first <= 'D'
                && second >= 'I' && second <= 'L'
                && third >= 'P' && third <= 'S';
    }

    private static int lookup(Class<?> holder, String name) {
        try {
            return holder.getField(name.toUpperCase()).getInt(null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            log.debug("No fairy ring component named {} on {}", name, holder.getSimpleName());
            return NOT_FOUND;
        }
    }
}
