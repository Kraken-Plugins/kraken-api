package plugins.api.world;

import lombok.Getter;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The places the in-client test suite runs, and what each one offers.
 *
 * <p>The list is deliberately short. Tests that previously demanded their own location — the Lumbridge
 * canoe station, the Barbarian Village fire — were changed to exercise the same API from the hub,
 * because the location was incidental to what they verified. What remains is a hub, a water source a
 * few tiles from it, and one genuine outpost.</p>
 */
@Getter
public enum NamedLocation {

    /**
     * The suite hub. Bank booths and bankers for the container tests, Guards and Men in the adjacent
     * square for the combat and NPC tests, and reliable foot traffic for the player query test. The
     * Varrock teleport also lands nearby, so tests that teleport away return here on their own.
     *
     * <p>These bounds are the only location coordinates attested elsewhere in the repository; they
     * match the area the area service test used before it was made player relative.</p>
     */
    VARROCK_EAST_BANK(
            "Varrock East Bank",
            new WorldPoint(3253, 3421, 0),
            new WorldArea(3250, 3416, 8, 8, 0),
            6,
            EnumSet.of(Facility.BANK_BOOTH, Facility.BANKER_NPC,
                    Facility.COMBAT_NPCS_F2P, Facility.OTHER_PLAYERS)),

    /**
     * The Varrock Square fountain, a short walk north of the hub, used as the water source for the
     * item on object interaction test.
     *
     * <p><b>Unverified:</b> this anchor has not been confirmed in game. Correct it against the actual
     * fountain (object id 5125) before relying on automated travel here.</p>
     */
    VARROCK_SQUARE_FOUNTAIN(
            "Varrock Square fountain",
            new WorldPoint(3253, 3433, 0),
            new WorldArea(3248, 3428, 11, 11, 0),
            6,
            EnumSet.of(Facility.WATER_SOURCE, Facility.COMBAT_NPCS_F2P, Facility.OTHER_PLAYERS)),

    /**
     * The only outpost the suite needs. Bankers, exchange clerks and a deposit box sit together, so
     * the grand exchange test and both deposit box tests share a single trip.
     *
     * <p><b>Unverified:</b> this anchor has not been confirmed in game.</p>
     */
    GRAND_EXCHANGE(
            "Grand Exchange",
            new WorldPoint(3165, 3487, 0),
            new WorldArea(3159, 3482, 16, 12, 0),
            8,
            EnumSet.of(Facility.BANKER_NPC, Facility.GRAND_EXCHANGE_CLERK,
                    Facility.DEPOSIT_BOX, Facility.OTHER_PLAYERS)),

    /** Sentinel for tests that work anywhere. Never triggers travel and carries no ordering weight. */
    ANYWHERE("Anywhere", null, null, 0, EnumSet.noneOf(Facility.class));

    private final String displayName;
    private final WorldPoint anchor;
    private final WorldArea bounds;
    private final int defaultRadius;
    private final Set<Facility> facilities;

    NamedLocation(String displayName, WorldPoint anchor, WorldArea bounds,
                  int defaultRadius, Set<Facility> facilities) {
        this.displayName = displayName;
        this.anchor = anchor;
        this.bounds = bounds;
        this.defaultRadius = defaultRadius;
        this.facilities = Collections.unmodifiableSet(facilities);
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
