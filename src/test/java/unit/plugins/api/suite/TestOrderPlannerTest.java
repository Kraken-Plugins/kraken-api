package unit.plugins.api.suite;

import net.runelite.api.coords.WorldPoint;
import org.junit.jupiter.api.Test;
import plugins.api.requirements.SideEffect;
import plugins.api.requirements.TestRequirements;
import plugins.api.suite.PlannedStep;
import plugins.api.suite.RegisteredTest;
import plugins.api.suite.TestGroup;
import plugins.api.suite.TestOrderPlanner;
import plugins.api.tests.BaseApiTest;
import plugins.api.world.Facility;
import plugins.api.world.NamedLocation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers run ordering. Getting this wrong is expensive rather than incorrect — a bad plan walks the
 * player back and forth, or lets a teleporting test strand the twenty tests that follow it.
 */
class TestOrderPlannerTest {

    private final TestOrderPlanner planner = new TestOrderPlanner();

    /** A test double that reports whatever requirements it is given. */
    private static class StubTest extends BaseApiTest {
        private final String name;
        private final TestRequirements requirements;

        StubTest(String name, TestRequirements requirements) {
            this.name = name;
            this.requirements = requirements;
        }

        @Override
        public TestRequirements requirements() {
            return requirements;
        }

        @Override
        protected boolean runTest(com.kraken.api.Context ctx) {
            return true;
        }

        @Override
        public String getTestName() {
            return name;
        }
    }

    private RegisteredTest registered(String name, TestGroup group, TestRequirements requirements) {
        return new RegisteredTest(group, new StubTest(name, requirements), "stub-" + name);
    }

    private List<String> namesOf(List<PlannedStep> plan) {
        return plan.stream().map(step -> step.getTest().getDisplayName()).collect(Collectors.toList());
    }

    @Test
    void theSelfCheckAlwaysRunsFirst() {
        List<RegisteredTest> tests = Arrays.asList(
                registered("Bank", TestGroup.QUERY, TestRequirements.builder()
                        .facility(Facility.BANK_BOOTH).build()),
                registered("Self Check", TestGroup.SELF_CHECK, TestRequirements.NONE));

        List<PlannedStep> plan = planner.plan(tests, null, false);

        assertEquals("Self Check", plan.get(0).getTest().getDisplayName());
    }

    @Test
    void anywhereTestsRunBeforeAnyTravel() {
        // Cheap signal first: if an update broke everything, the read-only tests say so in seconds
        // rather than after a walk across the map.
        List<RegisteredTest> tests = Arrays.asList(
                registered("AtBank", TestGroup.QUERY, TestRequirements.builder()
                        .facility(Facility.BANK_BOOTH).build()),
                registered("Anywhere", TestGroup.QUERY, TestRequirements.NONE));

        List<PlannedStep> plan = planner.plan(tests, null, false);

        assertEquals(Arrays.asList("Anywhere", "AtBank"), namesOf(plan));
    }

    @Test
    void destructiveTestsAreExcludedByDefault() {
        List<RegisteredTest> tests = Arrays.asList(
                registered("Normal", TestGroup.QUERY, TestRequirements.NONE),
                registered("Hops", TestGroup.QUERY, TestRequirements.builder()
                        .destructive(true).sideEffect(SideEffect.HOPS_WORLDS).build()));

        List<PlannedStep> plan = planner.plan(tests, null, false);

        assertEquals(Collections.singletonList("Normal"), namesOf(plan));
    }

    @Test
    void destructiveTestsCanBeOptedIn() {
        List<RegisteredTest> tests = Arrays.asList(
                registered("Normal", TestGroup.QUERY, TestRequirements.NONE),
                registered("Hops", TestGroup.QUERY, TestRequirements.builder()
                        .destructive(true).sideEffect(SideEffect.HOPS_WORLDS).build()));

        assertEquals(2, planner.plan(tests, null, true).size());
    }

    @Test
    void teleportingTestsAreOrderedLastWithinTheirLocation() {
        // Otherwise the teleport strands everything that was meant to run at the same place.
        List<RegisteredTest> tests = Arrays.asList(
                registered("Teleports", TestGroup.SERVICE, TestRequirements.builder()
                        .facility(Facility.BANK_BOOTH).sideEffect(SideEffect.TELEPORTS).build()),
                registered("ReadOnly", TestGroup.QUERY, TestRequirements.builder()
                        .facility(Facility.BANK_BOOTH).build()),
                registered("Moves", TestGroup.SERVICE, TestRequirements.builder()
                        .facility(Facility.BANK_BOOTH).sideEffect(SideEffect.MOVES_PLAYER).build()));

        List<PlannedStep> plan = planner.plan(tests, null, false);

        assertEquals(Arrays.asList("ReadOnly", "Moves", "Teleports"), namesOf(plan));
    }

    @Test
    void itemMutatingTestsSortBetweenReadOnlyAndMoving() {
        List<RegisteredTest> tests = Arrays.asList(
                registered("Moves", TestGroup.SERVICE, TestRequirements.builder()
                        .facility(Facility.BANK_BOOTH).sideEffect(SideEffect.MOVES_PLAYER).build()),
                registered("Deposits", TestGroup.QUERY, TestRequirements.builder()
                        .facility(Facility.BANK_BOOTH).sideEffect(SideEffect.EMPTIES_INVENTORY).build()),
                registered("ReadOnly", TestGroup.QUERY, TestRequirements.builder()
                        .facility(Facility.BANK_BOOTH).build()));

        assertEquals(Arrays.asList("ReadOnly", "Deposits", "Moves"),
                namesOf(planner.plan(tests, null, false)));
    }

    @Test
    void orderHintBreaksTiesWithinTheSameDisruptionRank() {
        List<RegisteredTest> tests = Arrays.asList(
                registered("Later", TestGroup.QUERY, TestRequirements.builder()
                        .facility(Facility.BANK_BOOTH).orderHint(50).build()),
                registered("Earlier", TestGroup.QUERY, TestRequirements.builder()
                        .facility(Facility.BANK_BOOTH).orderHint(1).build()));

        assertEquals(Arrays.asList("Earlier", "Later"), namesOf(planner.plan(tests, null, false)));
    }

    @Test
    void testsAtTheSameLocationAreGroupedTogether() {
        // Interleaving locations would mean walking back and forth between them.
        List<RegisteredTest> tests = Arrays.asList(
                registered("Bank1", TestGroup.QUERY, TestRequirements.builder()
                        .location(NamedLocation.VARROCK_EAST_BANK).build()),
                registered("Ge1", TestGroup.SERVICE, TestRequirements.builder()
                        .location(NamedLocation.GRAND_EXCHANGE).build()),
                registered("Bank2", TestGroup.QUERY, TestRequirements.builder()
                        .location(NamedLocation.VARROCK_EAST_BANK).build()));

        List<String> names = namesOf(planner.plan(tests, NamedLocation.VARROCK_EAST_BANK.getAnchor(), false));

        assertEquals(Arrays.asList("Bank1", "Bank2", "Ge1"), names);
    }

    @Test
    void theNearestLocationIsVisitedFirst() {
        List<RegisteredTest> tests = Arrays.asList(
                registered("Ge", TestGroup.SERVICE, TestRequirements.builder()
                        .location(NamedLocation.GRAND_EXCHANGE).build()),
                registered("Fountain", TestGroup.INTERACTION, TestRequirements.builder()
                        .location(NamedLocation.VARROCK_SQUARE_FOUNTAIN).build()));

        List<String> names = namesOf(planner.plan(tests, NamedLocation.VARROCK_EAST_BANK.getAnchor(), false));

        assertEquals(Arrays.asList("Fountain", "Ge"), names);
    }

    @Test
    void planningIsDeterministic() {
        // A run has to be reproducible or a failure cannot be chased down.
        List<RegisteredTest> tests = new ArrayList<>(Arrays.asList(
                registered("A", TestGroup.QUERY, TestRequirements.builder()
                        .location(NamedLocation.GRAND_EXCHANGE).build()),
                registered("B", TestGroup.QUERY, TestRequirements.NONE),
                registered("C", TestGroup.QUERY, TestRequirements.builder()
                        .location(NamedLocation.VARROCK_EAST_BANK).build())));

        List<String> first = namesOf(planner.plan(tests, null, false));
        List<String> second = namesOf(planner.plan(tests, null, false));

        assertEquals(first, second);
    }

    @Test
    void stepsAreIndexedContiguouslyFromZero() {
        List<RegisteredTest> tests = Arrays.asList(
                registered("A", TestGroup.QUERY, TestRequirements.NONE),
                registered("B", TestGroup.QUERY, TestRequirements.NONE),
                registered("C", TestGroup.QUERY, TestRequirements.NONE));

        List<PlannedStep> plan = planner.plan(tests, null, false);

        for (int i = 0; i < plan.size(); i++) {
            assertEquals(i, plan.get(i).getIndex());
        }
    }

    @Test
    void everyStepExplainsItself() {
        List<PlannedStep> plan = planner.plan(
                Collections.singletonList(registered("A", TestGroup.QUERY, TestRequirements.NONE)),
                null, false);

        assertFalse(plan.get(0).getRationale().isEmpty());
    }

    @Test
    void anEmptySelectionPlansNothing() {
        assertTrue(planner.plan(Collections.emptyList(), null, false).isEmpty());
    }
}
