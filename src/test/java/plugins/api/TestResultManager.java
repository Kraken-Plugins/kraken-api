package plugins.api;

import com.google.inject.Singleton;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import plugins.api.suite.SuiteProgress;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The single source of truth for what has run and how it went.
 *
 * <p>Results are held in registration order rather than a hash map. The old implementation iterated a
 * {@code ConcurrentHashMap}, so rows jumped around the overlay between frames and there was no stable
 * way to read "the third test".</p>
 *
 * <p>Every mutator is synchronised and every reader returns a copy. Results are written from the
 * runner's worker thread and read from the client thread during overlay rendering, and the previous
 * implementation published mutable objects across that boundary with no happens-before edge — so the
 * renderer could observe a status without the duration that goes with it.</p>
 */
@Slf4j
@Singleton
public class TestResultManager {

    /**
     * How a test ended.
     *
     * <p>{@link #SKIPPED} exists so that a red result always means a genuine regression. A missing
     * bank item is an environment problem, and reporting it as a failure makes it indistinguishable
     * from the API actually breaking.</p>
     */
    @Getter
    @AllArgsConstructor
    public enum TestStatus {
        NOT_STARTED("Not Started"),
        RUNNING("Running..."),
        PASSED("Passed"),
        FAILED("Failed"),
        SKIPPED("Skipped"),
        CANCELLED("Cancelled");

        private final String displayName;
    }

    /** Why a test was skipped, kept separate from a failure message. */
    @Getter
    @AllArgsConstructor
    public enum SkipKind {
        /** Something the environment does not provide, such as a missing item or too low a level. */
        UNMET_REQUIREMENT("Requirement not met"),
        /** The world did not respond during setup. Possibly a regression that surfaced early. */
        SETUP_FAILED("Setup failed"),
        /** Not logged in. */
        NOT_LOGGED_IN("Not logged in"),
        /** Excluded from this run because it is marked destructive. */
        DESTRUCTIVE_OPT_OUT("Excluded (destructive)");

        private final String displayName;
    }

    /** An immutable snapshot of one test's outcome. */
    @Getter
    @AllArgsConstructor
    public static class TestResult {
        private final String testId;
        private final String testName;
        private final TestStatus status;
        private final String lastRunTime;
        private final String message;
        private final SkipKind skipKind;
        private final long executionTimeMs;

        /**
         * A result for a test that has not run yet.
         *
         * @param testId stable id
         * @param testName display name
         * @return the initial result
         */
        static TestResult notStarted(String testId, String testName) {
            return new TestResult(testId, testName, TestStatus.NOT_STARTED, "Never", null, null, 0);
        }

        /**
         * Whether this result should be treated as a genuine problem with the API.
         *
         * @return true only for failures
         */
        public boolean isFailure() {
            return status == TestStatus.FAILED;
        }
    }

    private final Map<String, TestResult> results = new LinkedHashMap<>();

    private boolean running;
    private int currentIndex;
    private int total;
    private String currentTestId;
    private String currentTestName;
    private String currentPhase;
    private Instant startedAt;

    /**
     * Adds a test to the results table in registration order, without disturbing an existing entry.
     *
     * @param testId stable id
     * @param testName display name
     */
    public synchronized void registerTest(String testId, String testName) {
        results.putIfAbsent(testId, TestResult.notStarted(testId, testName));
    }

    /**
     * Begins a run.
     *
     * @param totalTests how many tests are in the run
     */
    public synchronized void beginRun(int totalTests) {
        this.running = true;
        this.total = totalTests;
        this.currentIndex = 0;
        this.startedAt = Instant.now();
        this.currentPhase = "Starting";
    }

    /**
     * Ends the current run.
     */
    public synchronized void endRun() {
        this.running = false;
        this.currentTestId = null;
        this.currentTestName = null;
        this.currentPhase = null;
    }

    /**
     * Marks a test as running and advances the run position.
     *
     * @param testId stable id
     * @param testName display name
     * @param index one based position within the run
     */
    public synchronized void beginTest(String testId, String testName, int index) {
        this.currentTestId = testId;
        this.currentTestName = testName;
        this.currentIndex = index;
        this.currentPhase = "Running " + testName;

        results.put(testId, new TestResult(testId, testName, TestStatus.RUNNING,
                existingRunTime(testId), null, null, 0));
    }

    /**
     * Updates the description of what is currently happening.
     *
     * @param phase a short description, e.g. "Travelling to Grand Exchange"
     */
    public synchronized void setPhase(String phase) {
        this.currentPhase = phase;
    }

    /**
     * Records a completed test.
     *
     * @param testId stable id
     * @param passed whether it passed
     * @param executionTimeMs how long it took
     */
    public synchronized void complete(String testId, boolean passed, long executionTimeMs) {
        TestResult existing = results.get(testId);
        String name = existing == null ? testId : existing.getTestName();

        results.put(testId, new TestResult(testId, name,
                passed ? TestStatus.PASSED : TestStatus.FAILED, timestamp(),
                passed ? null : "Test assertions failed", null, executionTimeMs));
    }

    /**
     * Records a test that threw.
     *
     * @param testId stable id
     * @param error what was thrown
     * @param executionTimeMs how long it ran before throwing
     */
    public synchronized void fail(String testId, Throwable error, long executionTimeMs) {
        TestResult existing = results.get(testId);
        String name = existing == null ? testId : existing.getTestName();
        String message = error == null ? "Unknown error"
                : error.getClass().getSimpleName() + ": " + error.getMessage();

        results.put(testId, new TestResult(testId, name, TestStatus.FAILED, timestamp(),
                message, null, executionTimeMs));
    }

    /**
     * Records a test that could not run because its preconditions were not met.
     *
     * @param testId stable id
     * @param kind why it was skipped
     * @param reason a human readable explanation
     */
    public synchronized void skip(String testId, SkipKind kind, String reason) {
        TestResult existing = results.get(testId);
        String name = existing == null ? testId : existing.getTestName();

        results.put(testId, new TestResult(testId, name, TestStatus.SKIPPED, timestamp(),
                reason, kind, 0));
    }

    /**
     * Records a test that was interrupted by cancellation.
     *
     * @param testId stable id
     */
    public synchronized void markCancelled(String testId) {
        TestResult existing = results.get(testId);
        String name = existing == null ? testId : existing.getTestName();

        results.put(testId, new TestResult(testId, name, TestStatus.CANCELLED, timestamp(),
                "Cancelled", null, 0));
    }

    /**
     * Returns any test still marked running to the not started state.
     *
     * <p>Used when a run ends abnormally, so nothing is left showing a spinner forever.</p>
     */
    public synchronized void clearRunningStates() {
        for (Map.Entry<String, TestResult> entry : results.entrySet()) {
            TestResult result = entry.getValue();
            if (result.getStatus() == TestStatus.RUNNING) {
                entry.setValue(TestResult.notStarted(result.getTestId(), result.getTestName()));
            }
        }
    }

    /**
     * Clears every recorded result, keeping the registered tests.
     */
    public synchronized void clearAllResults() {
        List<TestResult> registered = new ArrayList<>(results.values());
        results.clear();
        for (TestResult result : registered) {
            results.put(result.getTestId(), TestResult.notStarted(result.getTestId(), result.getTestName()));
        }
        endRun();
    }

    /**
     * Every result, in registration order.
     *
     * @return an immutable snapshot safe to iterate from any thread
     */
    public synchronized List<TestResult> resultsInOrder() {
        return new ArrayList<>(results.values());
    }

    /**
     * A snapshot of the run's progress.
     *
     * @return the current progress
     */
    public synchronized SuiteProgress getProgress() {
        return SuiteProgress.builder()
                .running(running)
                .currentIndex(currentIndex)
                .total(total)
                .currentTestId(currentTestId)
                .currentTestName(currentTestName)
                .currentPhase(currentPhase)
                .elapsed(startedAt == null ? Duration.ZERO : Duration.between(startedAt, Instant.now()))
                .passed(count(TestStatus.PASSED))
                .failed(count(TestStatus.FAILED))
                .skipped(count(TestStatus.SKIPPED))
                .cancelled(count(TestStatus.CANCELLED))
                .build();
    }

    /**
     * Whether a run is currently in progress.
     *
     * @return true while a run is active
     */
    public synchronized boolean isRunning() {
        return running;
    }

    /**
     * Counts results in a given state.
     *
     * @param status the state to count
     * @return how many results are in that state
     */
    public synchronized int count(TestStatus status) {
        return (int) results.values().stream().filter(r -> r.getStatus() == status).count();
    }

    /**
     * A one line summary of the run, suitable for an overlay.
     *
     * @return e.g. {@code "12/31 · 9P 2F 1S"} or {@code "All Tests Passed"}
     */
    public synchronized String getOverallStatus() {
        if (running) {
            return "Running " + currentIndex + "/" + total;
        }

        int passed = count(TestStatus.PASSED);
        int failed = count(TestStatus.FAILED);
        int skipped = count(TestStatus.SKIPPED);

        if (passed + failed + skipped == 0) {
            return "No Tests Run";
        }

        if (failed == 0 && skipped == 0) {
            return "All Tests Passed";
        }

        return String.format("%dP %dF %dS", passed, failed, skipped);
    }

    /**
     * Preserves the previous run timestamp when a test is restarted.
     *
     * @param testId stable id
     * @return the recorded timestamp, or "Never"
     */
    private String existingRunTime(String testId) {
        TestResult existing = results.get(testId);
        return existing == null ? "Never" : existing.getLastRunTime();
    }

    /**
     * The current wall clock time, formatted for display.
     *
     * @return a HH:mm:ss timestamp
     */
    private String timestamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }
}
