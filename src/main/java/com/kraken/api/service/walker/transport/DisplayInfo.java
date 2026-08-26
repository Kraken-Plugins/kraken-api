package com.kraken.api.service.walker.transport;

import lombok.Getter;

import java.util.Objects;

/**
 * The parsed form of a transport's display info.
 *
 * <p>For hub transports this is the whole of the execution data. The raw dataset rows leave it empty
 * because they list stops rather than journeys, but the loader expands those stops into every
 * permutation and fills it in per destination — so an expanded fairy ring edge carries the code of the
 * ring it leads to, and a spirit tree edge carries the menu entry that selects its stop.</p>
 *
 * <p>Three shapes appear:</p>
 * <ul>
 *   <li>a bare name, as in {@code "Aldarin"}</li>
 *   <li>a menu entry, as in {@code "6: Prifddinas"}, {@code "B: Farming Guild"} or
 *       {@code "4. Mushroom Meadow"}</li>
 *   <li>a fairy ring code, as in {@code "A L Q"}</li>
 * </ul>
 */
@Getter
public final class DisplayInfo {

    /** Returned as the menu position when the display info carries none. */
    public static final int NO_POSITION = -1;

    /** What the destination is called, as the interface shows it. */
    private final String label;

    /** The menu position, when the display info leads with one. */
    private final int position;

    /** The three letter fairy ring code with its spaces removed, or null when this is not one. */
    private final String fairyRingCode;

    private DisplayInfo(String label, int position, String fairyRingCode) {
        this.label = label;
        this.position = position;
        this.fairyRingCode = fairyRingCode;
    }

    /**
     * Parses display info into the parts an interface needs to select a destination.
     *
     * @param displayInfo the raw display info, may be null or blank
     * @return the parsed form, or null when there is nothing usable
     */
    public static DisplayInfo parse(String displayInfo) {
        if (displayInfo == null) {
            return null;
        }

        String trimmed = displayInfo.trim();
        if (trimmed.isEmpty()) {
            return null;
        }

        String code = fairyRingCode(trimmed);
        if (code != null) {
            return new DisplayInfo(trimmed, NO_POSITION, code);
        }

        int separator = separatorIndex(trimmed);
        if (separator > 0) {
            String prefix = trimmed.substring(0, separator).trim();
            String rest = trimmed.substring(separator + 1).trim();

            if (!rest.isEmpty() && isMenuPosition(prefix)) {
                return new DisplayInfo(rest, positionOf(prefix), null);
            }
        }

        return new DisplayInfo(trimmed, NO_POSITION, null);
    }

    /**
     * Reports whether this display info selects a fairy ring.
     *
     * @return true when it carries a fairy ring code
     */
    public boolean isFairyRing() {
        return fairyRingCode != null;
    }

    /**
     * Reports whether the display info leads with a menu position.
     *
     * @return true when a position was parsed
     */
    public boolean hasPosition() {
        return position != NO_POSITION;
    }

    /**
     * Recognises a fairy ring code.
     *
     * <p>Codes are always one letter from each dial, written spaced. Anything else — including the
     * one dataset entry that chains several codes together for a multi hop journey — is not treated
     * as a code, because a single selection cannot express it.</p>
     */
    private static String fairyRingCode(String value) {
        String compact = value.replace(" ", "");
        if (compact.length() != 3) {
            return null;
        }

        char first = Character.toUpperCase(compact.charAt(0));
        char second = Character.toUpperCase(compact.charAt(1));
        char third = Character.toUpperCase(compact.charAt(2));

        boolean valid = first >= 'A' && first <= 'D'
                && second >= 'I' && second <= 'L'
                && third >= 'P' && third <= 'S';

        return valid ? "" + first + second + third : null;
    }

    /** Menu entries separate their position from the name with either a colon or a full stop. */
    private static int separatorIndex(String value) {
        int colon = value.indexOf(':');
        int dot = value.indexOf('.');

        if (colon < 0) {
            return dot;
        }
        if (dot < 0) {
            return colon;
        }
        return Math.min(colon, dot);
    }

    /**
     * A position is a single digit or letter. Anything longer is part of the name — display info for
     * a teleport item reads "Ardougne cloak: Kandarin Monastery", and "Ardougne cloak" is not a
     * position.
     */
    private static boolean isMenuPosition(String prefix) {
        if (prefix.length() != 1) {
            return false;
        }

        char c = prefix.charAt(0);
        return Character.isDigit(c) || Character.isLetter(c);
    }

    private static int positionOf(String prefix) {
        char c = prefix.charAt(0);
        if (Character.isDigit(c)) {
            return c - '0';
        }

        // Lettered entries continue the numbering past nine, so A is the tenth entry.
        return Character.toUpperCase(c) - 'A' + 10;
    }

    @Override
    public String toString() {
        return "DisplayInfo(label=" + label + ", position=" + position + ", code=" + fairyRingCode + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DisplayInfo that = (DisplayInfo) o;
        return position == that.position
                && Objects.equals(label, that.label)
                && Objects.equals(fairyRingCode, that.fairyRingCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(label, position, fairyRingCode);
    }
}
