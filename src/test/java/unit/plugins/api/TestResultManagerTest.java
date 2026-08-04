package unit.plugins.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import plugins.api.TestResultManager;
import plugins.api.suite.SuiteProgress;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers result tracking, including the distinction that makes a run readable: a skip is not a
 * failure, so a red row always means the API actually regressed.
 */
class TestResultManagerTest {

    private TestResultManager manager;

    @BeforeEach
    void setUp() {
        manager = new TestResultManager();
        manager.registerTest("AlphaTest", "Alpha");
        manager.registerTest("BetaTest", "Beta");
        manager.registerTest("GammaTest", "Gamma");
    }

    private List<String> idsInOrder() {
        return manager.resultsInOrder().stream()
                .map(TestResultManager.TestResult::getTestId)
                .collect(Collectors.toList());
    }

    @Test
    void resultsKeepRegistrationOrder() {
        // The old implementation iterated a ConcurrentHashMap, so overlay rows shuffled every frame.
        assertEquals(Arrays.asList("AlphaTest", "BetaTest", "GammaTest"), idsInOrder());
    }

    @Test
    void registeringTwiceDoesNotDuplicateOrReorder() {
        manager.registerTest("AlphaTest", "Alpha");

        assertEquals(Arrays.asList("AlphaTest", "BetaTest", "GammaTest"), idsInOrder());
    }

    @Test
    void testsStartNotStarted() {
        assertEquals(TestResultManager.TestStatus.NOT_STARTED,
                manager.resultsInOrder().get(0).getStatus());
    }

    @Test
    void aPassIsRecordedWithItsDuration() {
        manager.beginTest("AlphaTest", "Alpha", 1);
        manager.complete("AlphaTest", true, 312);

        TestResultManager.TestResult result = manager.resultsInOrder().get(0);

        assertEquals(TestResultManager.TestStatus.PASSED, result.getStatus());
        assertEquals(312, result.getExecutionTimeMs());
        assertNull(result.getMessage());
        assertFalse(result.isFailure());
    }

    @Test
    void aFailureCarriesAMessageAndCountsAsAFailure() {
        manager.beginTest("AlphaTest", "Alpha", 1);
        manager.complete("AlphaTest", false, 100);

        TestResultManager.TestResult result = manager.resultsInOrder().get(0);

        assertEquals(TestResultManager.TestStatus.FAILED, result.getStatus());
        assertTrue(result.isFailure());
        assertEquals(1, manager.count(TestResultManager.TestStatus.FAILED));
    }

    @Test
    void aThrownErrorIsRecordedWithItsType() {
        manager.beginTest("AlphaTest", "Alpha", 1);
        manager.fail("AlphaTest", new IllegalStateException("boom"), 50);

        TestResultManager.TestResult result = manager.resultsInOrder().get(0);

        assertEquals(TestResultManager.TestStatus.FAILED, result.getStatus());
        assertTrue(result.getMessage().contains("IllegalStateException"));
        assertTrue(result.getMessage().contains("boom"));
    }

    @Test
    void aSkipIsNotAFailure() {
        // The whole point of the status: a missing bank item must never look like a regression.
        manager.beginTest("AlphaTest", "Alpha", 1);
        manager.skip("AlphaTest", TestResultManager.SkipKind.UNMET_REQUIREMENT,
                "Rune platebody is not in the bank");

        TestResultManager.TestResult result = manager.resultsInOrder().get(0);

        assertEquals(TestResultManager.TestStatus.SKIPPED, result.getStatus());
        assertFalse(result.isFailure());
        assertEquals(0, manager.count(TestResultManager.TestStatus.FAILED));
        assertEquals(1, manager.count(TestResultManager.TestStatus.SKIPPED));
        assertEquals("Rune platebody is not in the bank", result.getMessage());
        assertEquals(TestResultManager.SkipKind.UNMET_REQUIREMENT, result.getSkipKind());
    }

    @Test
    void setupFailuresAreDistinguishedFromUnmetRequirements() {
        // Both are yellow, but one hints at a regression that surfaced during setup and deserves
        // separate attention in a summary.
        manager.skip("AlphaTest", TestResultManager.SkipKind.SETUP_FAILED, "the bank never opened");

        assertEquals(TestResultManager.SkipKind.SETUP_FAILED,
                manager.resultsInOrder().get(0).getSkipKind());
    }

    @Test
    void cancellingIsNeitherPassNorFail() {
        manager.beginTest("AlphaTest", "Alpha", 1);
        manager.markCancelled("AlphaTest");

        TestResultManager.TestResult result = manager.resultsInOrder().get(0);

        assertEquals(TestResultManager.TestStatus.CANCELLED, result.getStatus());
        assertFalse(result.isFailure());
        assertEquals(0, manager.count(TestResultManager.TestStatus.FAILED));
    }

    @Test
    void runningStatesAreClearedWhenARunEndsAbnormally() {
        manager.beginTest("AlphaTest", "Alpha", 1);
        assertEquals(1, manager.count(TestResultManager.TestStatus.RUNNING));

        manager.clearRunningStates();

        assertEquals(0, manager.count(TestResultManager.TestStatus.RUNNING));
        assertEquals(TestResultManager.TestStatus.NOT_STARTED,
                manager.resultsInOrder().get(0).getStatus());
    }

    @Test
    void progressTracksPositionAndCounts() {
        manager.beginRun(3);
        manager.beginTest("AlphaTest", "Alpha", 1);
        manager.complete("AlphaTest", true, 10);
        manager.beginTest("BetaTest", "Beta", 2);
        manager.skip("BetaTest", TestResultManager.SkipKind.UNMET_REQUIREMENT, "no guard nearby");

        SuiteProgress progress = manager.getProgress();

        assertTrue(progress.isRunning());
        assertEquals(2, progress.getCurrentIndex());
        assertEquals(3, progress.getTotal());
        assertEquals("2/3", progress.describePosition());
        assertEquals(1, progress.getPassed());
        assertEquals(1, progress.getSkipped());
    }

    @Test
    void currentPhaseIsReported() {
        manager.beginRun(1);
        manager.beginTest("AlphaTest", "Alpha", 1);
        manager.setPhase("Travelling to Grand Exchange");

        assertEquals("Travelling to Grand Exchange", manager.getProgress().getCurrentPhase());
    }

    @Test
    void endingARunStopsReportingAsRunning() {
        manager.beginRun(1);
        assertTrue(manager.isRunning());

        manager.endRun();

        assertFalse(manager.isRunning());
        assertNull(manager.getProgress().getCurrentTestId());
    }

    @Test
    void clearingResetsResultsButKeepsRegisteredTests() {
        manager.beginTest("AlphaTest", "Alpha", 1);
        manager.complete("AlphaTest", true, 10);

        manager.clearAllResults();

        assertEquals(Arrays.asList("AlphaTest", "BetaTest", "GammaTest"), idsInOrder());
        assertEquals(0, manager.count(TestResultManager.TestStatus.PASSED));
        assertEquals(TestResultManager.TestStatus.NOT_STARTED,
                manager.resultsInOrder().get(0).getStatus());
    }

    @Test
    void overallStatusSummarisesTheRun() {
        assertEquals("No Tests Run", manager.getOverallStatus());

        manager.complete("AlphaTest", true, 10);
        assertEquals("All Tests Passed", manager.getOverallStatus());

        manager.complete("BetaTest", false, 10);
        assertTrue(manager.getOverallStatus().contains("1F"));
    }

    @Test
    void aSkipKeepsOverallStatusOutOfTheAllPassedState() {
        // A run with skips has not really covered everything, so it must not read as a clean pass.
        manager.complete("AlphaTest", true, 10);
        manager.skip("BetaTest", TestResultManager.SkipKind.UNMET_REQUIREMENT, "missing item");

        assertFalse(manager.getOverallStatus().equals("All Tests Passed"));
        assertTrue(manager.getOverallStatus().contains("1S"));
    }

    @Test
    void snapshotsAreIndependentOfLaterMutation() {
        List<TestResultManager.TestResult> snapshot = manager.resultsInOrder();
        manager.complete("AlphaTest", true, 10);

        assertEquals(TestResultManager.TestStatus.NOT_STARTED, snapshot.get(0).getStatus());
    }
}
