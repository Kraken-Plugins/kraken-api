package plugins.api.suite;

import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.coords.WorldPoint;
import plugins.api.requirements.SideEffect;
import plugins.api.requirements.TestRequirements;
import plugins.api.world.NamedLocation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Turns a set of tests into a travel-minimising, deterministic itinerary.
 *
 * <p>Two rules do most of the work. Tests that work anywhere run first, because they cost no travel
 * and — more usefully — if a client update broke everything you find out in fifteen seconds from the
 * npc and widget queries rather than after a two minute walk. Within each location, tests are ordered
 * by how disruptive they are, so the one that teleports away is the last thing that happens there.</p>
 *
 * <p>Ordering is fully deterministic: ties break on registration order, so the same input always
 * produces the same plan and a failure is reproducible.</p>
 */
@Slf4j
@Singleton
public class TestOrderPlanner {

    /** Read only: no declared side effects at all. */
    private static final int RANK_READ_ONLY = 0;

    /** Mutates carried or worn items, recoverable at the bank the test is already standing at. */
    private static final int RANK_MUTATES_ITEMS = 1;

    /** Leaves the player somewhere else in the same area. */
    private static final int RANK_MOVES = 2;

    /** Leaves the area entirely. */
    private static final int RANK_TELEPORTS = 3;

    /**
     * Produces the itinerary.
     *
     * @param tests the tests to order
     * @param start where the player is now, used as the travelling origin; may be null
     * @param includeDestructive whether to keep tests marked destructive
     * @return the ordered plan
     */
    public List<PlannedStep> plan(List<RegisteredTest> tests, WorldPoint start, boolean includeDestructive) {
        List<RegisteredTest> eligible = new ArrayList<>();
        RegisteredTest selfCheck = null;

        for (RegisteredTest test : tests) {
            if (test.getGroup() == TestGroup.SELF_CHECK) {
                selfCheck = test;
                continue;
            }

            // Destructive tests hop worlds or spend real coins. Excluded unless asked for, which is
            // what keeps the world query test's hop from invalidating everything after it.
            if (test.isDestructive() && !includeDestructive) {
                continue;
            }

            eligible.add(test);
        }

        Map<NamedLocation, List<RegisteredTest>> buckets = bucketByLocation(eligible);

        List<PlannedStep> plan = new ArrayList<>();

        if (selfCheck != null) {
            plan.add(new PlannedStep(plan.size(), selfCheck, NamedLocation.ANYWHERE, RANK_READ_ONLY,
                    "Runs first: gates the rest of the run on the harness being able to drive the client"));
        }

        // Anywhere first: no travel, and the fastest possible signal that something is broadly broken.
        List<RegisteredTest> anywhere = buckets.remove(NamedLocation.ANYWHERE);
        if (anywhere != null) {
            appendBucket(plan, anywhere, NamedLocation.ANYWHERE, "Runs anywhere, so costs no travel");
        }

        for (NamedLocation location : orderLocations(buckets.keySet(), start)) {
            appendBucket(plan, buckets.get(location), location,
                    "Grouped at " + location.getDisplayName() + " to avoid re-walking");
        }

        return plan;
    }

    /**
     * Groups tests by where each one needs to run.
     *
     * @param tests the eligible tests
     * @return a map from location to the tests that belong there, preserving registration order
     */
    private Map<NamedLocation, List<RegisteredTest>> bucketByLocation(List<RegisteredTest> tests) {
        Map<NamedLocation, List<RegisteredTest>> buckets = new LinkedHashMap<>();

        for (RegisteredTest test : tests) {
            buckets.computeIfAbsent(resolveLocation(test), key -> new ArrayList<>()).add(test);
        }

        return buckets;
    }

    /**
     * Decides which location bucket a test belongs in.
     *
     * @param test the test to place
     * @return its explicit location, the nearest location offering its facilities, or
     *         {@link NamedLocation#ANYWHERE}
     */
    private NamedLocation resolveLocation(RegisteredTest test) {
        TestRequirements requirements = test.requirements();

        if (requirements.getLocation() != null) {
            return requirements.getLocation();
        }

        if (requirements.getFacilities().isEmpty()) {
            return NamedLocation.ANYWHERE;
        }

        List<NamedLocation> candidates =
                NamedLocation.providing(requirements.getFacilities(), null);

        return candidates.isEmpty() ? NamedLocation.ANYWHERE : candidates.get(0);
    }

    /**
     * Orders location buckets by a greedy nearest-neighbour walk from the starting point.
     *
     * @param locations the buckets to visit
     * @param start where the player is now; may be null, in which case declaration order is used
     * @return the visiting order
     */
    private List<NamedLocation> orderLocations(Iterable<NamedLocation> locations, WorldPoint start) {
        List<NamedLocation> remaining = new ArrayList<>();
        locations.forEach(remaining::add);

        List<NamedLocation> ordered = new ArrayList<>();
        NamedLocation current = start == null ? null : NamedLocation.at(start).orElse(null);

        while (!remaining.isEmpty()) {
            final NamedLocation from = current;
            final WorldPoint origin = from == null ? start : from.getAnchor();

            remaining.sort(Comparator
                    // Ties break on enum ordinal so the plan is reproducible run to run.
                    .<NamedLocation>comparingInt(candidate -> distance(origin, candidate))
                    .thenComparingInt(Enum::ordinal));

            NamedLocation next = remaining.remove(0);
            ordered.add(next);
            current = next;
        }

        return ordered;
    }

    /**
     * Distance from a point to a location's anchor.
     *
     * @param from the origin; may be null
     * @param to the candidate location
     * @return the distance in tiles, or zero when there is no origin to measure from
     */
    private int distance(WorldPoint from, NamedLocation to) {
        if (from == null || to.getAnchor() == null) {
            return 0;
        }
        return to.getAnchor().distanceTo(from);
    }

    /**
     * Appends one location's tests to the plan, least disruptive first.
     *
     * @param plan the accumulating itinerary
     * @param bucket the tests at this location
     * @param location where they run
     * @param rationale why they are grouped here
     */
    private void appendBucket(List<PlannedStep> plan, List<RegisteredTest> bucket,
                              NamedLocation location, String rationale) {
        if (bucket == null || bucket.isEmpty()) {
            return;
        }

        List<RegisteredTest> ordered = new ArrayList<>(bucket);
        // Stable sort: equal ranks keep registration order, so the plan stays reproducible.
        ordered.sort(Comparator
                .comparingInt(this::disruptionRank)
                .thenComparingInt(test -> test.requirements().getOrderHint()));

        for (RegisteredTest test : ordered) {
            int rank = disruptionRank(test);
            plan.add(new PlannedStep(plan.size(), test, location, rank,
                    rationale + describeRank(rank)));
        }
    }

    /**
     * Scores how disruptive a test is, which decides how late within its group it runs.
     *
     * @param test the test to score
     * @return the disruption rank, higher meaning more disruptive
     */
    int disruptionRank(RegisteredTest test) {
        TestRequirements requirements = test.requirements();

        if (requirements.hasSideEffect(SideEffect.TELEPORTS)
                || requirements.hasSideEffect(SideEffect.HOPS_WORLDS)) {
            return RANK_TELEPORTS;
        }

        if (requirements.hasSideEffect(SideEffect.MOVES_PLAYER)) {
            return RANK_MOVES;
        }

        if (requirements.getSideEffects().isEmpty()) {
            return RANK_READ_ONLY;
        }

        return RANK_MUTATES_ITEMS;
    }

    /**
     * Renders a disruption rank as a clause appended to a step's rationale.
     *
     * @param rank the disruption rank
     * @return a human readable suffix
     */
    private String describeRank(int rank) {
        switch (rank) {
            case RANK_TELEPORTS:
                return "; ordered last here because it leaves the area";
            case RANK_MOVES:
                return "; ordered late here because it moves the player";
            case RANK_MUTATES_ITEMS:
                return "; changes items, recoverable without travelling";
            default:
                return "; read only";
        }
    }
}
