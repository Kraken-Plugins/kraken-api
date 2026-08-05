package plugins.api.world;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;

import java.util.*;
import java.util.stream.Collectors;

/**
 * The places the in-client test suite runs, and what each one offers.
 */
@Getter
@AllArgsConstructor
public enum NamedLocation {
    VARROCK_EAST_BANK(
            "Varrock East Bank",
            new WorldPoint(3253, 3421, 0),
            new WorldArea(3250, 3416, 8, 8, 0),
            6,
            EnumSet.of(Facility.BANK_BOOTH, Facility.BANKER_NPC,
                    Facility.COMBAT_NPCS_F2P, Facility.OTHER_PLAYERS)),


    VARROCK_WEST_BANK(
            "Varrock West Bank",
            new WorldPoint(3182, 3438, 0),
            new WorldArea(3179, 3433, 12, 15, 0),
            6,
            EnumSet.of(Facility.BANK_BOOTH, Facility.BANKER_NPC, Facility.DEPOSIT_BOX, Facility.OTHER_PLAYERS)),

    VARROCK_EAST_GUARDS(
            "Varrock East Guards",
            new WorldPoint(3271, 3428, 0),
            new WorldArea(3266, 3425, 13, 8, 0),
            3,
            EnumSet.of(Facility.COMBAT_NPCS_F2P)
    ),

    VARROCK_SQUARE_FOUNTAIN(
            "Varrock Square fountain",
            new WorldPoint(3212, 3428, 0),
            new WorldArea(3208, 3425, 9, 8, 0),
            6,
            EnumSet.of(Facility.WATER_SOURCE, Facility.OTHER_PLAYERS)),

    GRAND_EXCHANGE(
            "Grand Exchange",
            new WorldPoint(3165, 3487, 0),
            new WorldArea(3159, 3482, 16, 12, 0),
            8,
            EnumSet.of(Facility.BANKER_NPC, Facility.GRAND_EXCHANGE_CLERK,
                    Facility.DEPOSIT_BOX, Facility.OTHER_PLAYERS)),

    ANYWHERE("Anywhere", null, null, 0, EnumSet.noneOf(Facility.class));

    private final String displayName;
    private final WorldPoint anchor;
    private final WorldArea bounds;
    private final int defaultRadius;
    private final Set<Facility> facilities;

    /**
     * The capabilities this location offers.
     *
     * <p>Hand written rather than generated so the returned set cannot be modified. Enum constants are
     * global singletons, so handing out the live {@code EnumSet} lets any caller permanently delete a
     * location's facilities for the rest of the process — which is exactly what happened when a unit
     * test called {@code clear()} on it and every later lookup stopped finding the bank.</p>
     *
     * @return an unmodifiable view of this location's facilities
     */
    public Set<Facility> getFacilities() {
        return Collections.unmodifiableSet(facilities);
    }

    /**
     * Reports whether this location offers every one of the given facilities.
     *
     * @param required the facilities a test depends on; an empty set is satisfied by any location
     * @return true when this location offers all of them
     */
    public boolean provides(Set<Facility> required) {
        return required == null || required.isEmpty() || facilities.containsAll(required);
    }

    /**
     * Reports whether a tile counts as being at this location.
     *
     * <p>A point inside the declared bounds qualifies, as does one within {@link #getDefaultRadius()}
     * of the anchor. The radius fallback matters because the bounds describe the useful interior of a
     * place, and standing a couple of tiles outside the doorway should still count as "here".</p>
     *
     * @param point the tile to test, may be null
     * @return true when the tile is at this location; always false for {@link #ANYWHERE}
     */
    public boolean contains(WorldPoint point) {
        if (point == null || anchor == null) {
            return false;
        }

        if (bounds != null && bounds.contains(point)) {
            return true;
        }

        return anchor.getPlane() == point.getPlane() && anchor.distanceTo(point) <= defaultRadius;
    }

    /**
     * Straight line distance between two location anchors, used only as an ordering heuristic.
     *
     * @param other the location to measure to
     * @return the distance in tiles, or {@link Integer#MAX_VALUE} when either location has no anchor
     */
    public int distanceTo(NamedLocation other) {
        if (anchor == null || other == null || other.anchor == null) {
            return Integer.MAX_VALUE;
        }
        return anchor.distanceTo(other.anchor);
    }

    /**
     * Finds every real location offering the required facilities, nearest first.
     *
     * @param required the facilities that must all be present
     * @param from the tile to measure distance from; when null the declaration order is preserved
     * @return matching locations ordered by distance from {@code from}, never including
     *         {@link #ANYWHERE}
     */
    public static List<NamedLocation> providing(Set<Facility> required, WorldPoint from) {
        return Arrays.stream(values())
                .filter(location -> location != ANYWHERE)
                .filter(location -> location.provides(required))
                .sorted(Comparator.comparingInt(location ->
                        from == null ? 0 : location.getAnchor().distanceTo(from)))
                .collect(Collectors.toList());
    }

    /**
     * Identifies which location a tile belongs to.
     *
     * @param point the tile to resolve, may be null
     * @return the matching location, or empty when the tile is not at any of them
     */
    public static Optional<NamedLocation> at(WorldPoint point) {
        return Arrays.stream(values())
                .filter(location -> location != ANYWHERE)
                .filter(location -> location.contains(point))
                .findFirst();
    }
}
