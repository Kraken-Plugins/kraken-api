package com.kraken.api.service.walker.transport;

import lombok.Getter;

import java.util.Objects;

/**
 * The parsed form of a transport's {@code objectInfo} string.
 *
 * <p>The transport dataset stores what to click as a single unstructured string under a column headed
 * {@code menuOption menuTarget objectID}, for example {@code "Open Door 9398"}. Only the trailing
 * integer is unambiguous: both the menu option and the target may contain spaces and there is no
 * delimiter between them, so {@code "Al Kharid Amulet of Glory 13523"} splits as option
 * {@code "Al Kharid"} and target {@code "Amulet of Glory"}, not as option {@code "Al"}.</p>
 *
 * <p>Parsing therefore happens in two stages. {@link #parse(String)} extracts the id and makes a
 * best-effort guess at the boundary by taking the first word as the option. Once the entity behind the
 * id has been resolved and its real name is known, {@link #withEntityName(String)} re-splits the
 * remainder on that name and yields an exact option, because the remainder always ends with the
 * entity's name.</p>
 */
@Getter
public final class ObjectInfo {

    /** Returned as the id when the string carries no trailing integer. */
    public static final int NO_ID = -1;

    private final String menuOption;
    private final String menuTarget;
    private final int id;

    private ObjectInfo(String menuOption, String menuTarget, int id) {
        this.menuOption = menuOption;
        this.menuTarget = menuTarget;
        this.id = id;
    }

    /**
     * Parses a raw {@code objectInfo} string into its menu option, target and id.
     *
     * <p>The option/target boundary is a guess at this stage; refine it with
     * {@link #withEntityName(String)} once the entity's real name is known.</p>
     *
     * @param objectInfo the raw string, may be null or blank
     * @return the parsed form, or null when {@code objectInfo} holds nothing usable
     */
    public static ObjectInfo parse(String objectInfo) {
        if (objectInfo == null) {
            return null;
        }

        String trimmed = objectInfo.trim();
        if (trimmed.isEmpty()) {
            return null;
        }

        int id = NO_ID;
        String remainder = trimmed;

        int lastSpace = trimmed.lastIndexOf(' ');
        if (lastSpace >= 0) {
            String tail = trimmed.substring(lastSpace + 1);
            if (isUnsignedInteger(tail)) {
                id = Integer.parseInt(tail);
                remainder = trimmed.substring(0, lastSpace).trim();
            }
        }

        if (remainder.isEmpty()) {
            return null;
        }

        int firstSpace = remainder.indexOf(' ');
        if (firstSpace < 0) {
            return new ObjectInfo(remainder, "", id);
        }

        return new ObjectInfo(
                remainder.substring(0, firstSpace),
                remainder.substring(firstSpace + 1).trim(),
                id
        );
    }

    /**
     * Re-splits the option and target using the entity's real name.
     *
     * <p>The unparsed remainder is {@code menuOption + " " + menuTarget}, and the target is the
     * entity's name as the client reports it. Matching on that name is what makes multi-word options
     * such as {@code "Al Kharid"} or {@code "Musa Point"} recoverable.</p>
     *
     * @param entityName the name from the object, NPC or item composition, may be null
     * @return a refined copy, or this instance when the name does not fit
     */
    public ObjectInfo withEntityName(String entityName) {
        if (entityName == null) {
            return this;
        }

        String name = entityName.trim();
        if (name.isEmpty()) {
            return this;
        }

        String remainder = menuTarget.isEmpty() ? menuOption : menuOption + " " + menuTarget;
        if (remainder.equalsIgnoreCase(name)) {
            return new ObjectInfo("", name, id);
        }

        // The remainder reads "<option> <name>", so the name is a suffix and whatever precedes it is
        // the menu option however many words it runs to.
        if (remainder.length() > name.length() && endsWithIgnoreCase(remainder, name)) {
            int boundary = remainder.length() - name.length();
            if (remainder.charAt(boundary - 1) == ' ') {
                return new ObjectInfo(
                        remainder.substring(0, boundary - 1).trim(),
                        remainder.substring(boundary),
                        id
                );
            }
        }

        return this;
    }

    /**
     * Reports whether the string carried a usable entity id.
     *
     * @return true when an id was parsed
     */
    public boolean hasId() {
        return id != NO_ID;
    }

    /**
     * Returns the unparsed remainder, which is the option and target with their original spacing.
     *
     * @return the option and target rejoined
     */
    public String getRemainder() {
        return menuTarget.isEmpty() ? menuOption : menuOption + " " + menuTarget;
    }

    private static boolean isUnsignedInteger(String value) {
        if (value.isEmpty() || value.length() > 9) {
            return false;
        }

        for (int i = 0; i < value.length(); i++) {
            if (value.charAt(i) < '0' || value.charAt(i) > '9') {
                return false;
            }
        }

        return true;
    }

    private static boolean endsWithIgnoreCase(String value, String suffix) {
        return value.regionMatches(true, value.length() - suffix.length(), suffix, 0, suffix.length());
    }

    @Override
    public String toString() {
        return "ObjectInfo(option=" + menuOption + ", target=" + menuTarget + ", id=" + id + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ObjectInfo that = (ObjectInfo) o;
        return id == that.id
                && Objects.equals(menuOption, that.menuOption)
                && Objects.equals(menuTarget, that.menuTarget);
    }

    @Override
    public int hashCode() {
        return Objects.hash(menuOption, menuTarget, id);
    }
}
