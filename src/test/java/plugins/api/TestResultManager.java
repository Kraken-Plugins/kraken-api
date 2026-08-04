package plugins.api;

import com.google.inject.Singleton;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Getter
@Singleton
public class TestResultManager {

    @Getter
    @AllArgsConstructor
    public enum TestStatus {
        NOT_STARTED("Not Started"),
        RUNNING("Running..."),
        PASSED("Passed"),
        FAILED("Failed"),
        DISABLED("Disabled");
        private final String displayName;
    }

    public static class TestResult {
        @Getter
        private final String testName;
        @Getter
        private TestStatus status;
        @Getter
        private String lastRunTime;
        @Getter
        private String errorMessage;
        @Getter
        private long executionTimeMs;

        public TestResult(String testName) {
            this.testName = testName;
            this.status = TestStatus.NOT_STARTED;
            this.lastRunTime = "Never";
            this.errorMessage = null;
            this.executionTimeMs = 0;
        }

        public void setRunning() {
            this.status = TestStatus.RUNNING;
            this.errorMessage = null;
        }

        public void setCompleted(boolean passed, long executionTimeMs, String errorMessage) {
            this.status = passed ? TestStatus.PASSED : TestStatus.FAILED;
            this.lastRunTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            this.executionTimeMs = executionTimeMs;
            this.errorMessage = errorMessage;
        }

        public void setDisabled() {
            this.status = TestStatus.DISABLED;
        }

        /**
         * Returns this result to its pre-run state, discarding any error recorded by a cancellation.
         * The previous run time and duration are kept so the panel can still show when the test last
         * produced a real result.
         */
        public void setNotStarted() {
            this.status = TestStatus.NOT_STARTED;
            this.errorMessage = null;
        }
    }

    private final Map<String, TestResult> testResults = new ConcurrentHashMap<>();
    private final Map<String, CompletableFuture<Boolean>> runningTests = new ConcurrentHashMap<>();

    public void registerTest(String testName) {
        testResults.putIfAbsent(testName, new TestResult(testName));
    }

    public Map<String, TestResult> getAllTestResults() {
        return new ConcurrentHashMap<>(testResults);
    }

    public void startTest(String testName, CompletableFuture<Boolean> testFuture) {
        TestResult result = testResults.computeIfAbsent(testName, TestResult::new);
        result.setRunning();
        runningTests.put(testName, testFuture);

        long startTime = System.currentTimeMillis();

        // Handle test completion
        testFuture.whenComplete((passed, throwable) -> {
            long executionTime = System.currentTimeMillis() - startTime;
            runningTests.remove(testName);

            if (throwable != null) {
                result.setCompleted(false, executionTime, throwable.getMessage());
                log.error("Test {} failed with exception", testName, throwable);
            } else {
                result.setCompleted(passed, executionTime, passed ? null : "Test assertions failed");
                log.info("Test {} completed: {} ({}ms)", testName,
                        passed ? "PASSED" : "FAILED", executionTime);
            }

            testResults.put(testName, result);
        });
    }

    public void setTestDisabled(String testName) {
        testResults.computeIfAbsent(testName, TestResult::new).setDisabled();
    }

    public boolean isTestRunning(String testName) {
        return runningTests.containsKey(testName);
    }

    public boolean areAnyTestsRunning() {
        return !runningTests.isEmpty();
    }

    /**
     * Cancels a running test and returns it to {@link TestStatus#NOT_STARTED}.
     *
     * <p>The status reset is applied <em>after</em> the future is cancelled, and the future is removed
     * from {@code runningTests} first. Both matter: {@link CompletableFuture#cancel(boolean)} completes
     * the future exceptionally, which synchronously fires the {@code whenComplete} callback registered
     * in {@link #startTest(String, CompletableFuture)} and marks the test FAILED. Resetting afterwards
     * is what makes an un-ticked checkbox read "Not Started" instead of "Failed".</p>
     *
     * <p>Note that {@code cancel(true)} cannot interrupt a {@link CompletableFuture} that is already
     * executing — the supplier keeps running to completion on its pool thread, orphaned. Its late
     * completion is harmless because it no longer appears in {@code runningTests}, but a genuinely
     * interruptible test run needs a dedicated worker thread rather than the common pool.</p>
     *
     * @param testName the registered display name of the test to cancel
     */
    public void cancelTest(String testName) {
        CompletableFuture<Boolean> future = runningTests.remove(testName);
        if (future == null) {
            // Nothing in flight: the test either never started or completed on its own, in which case
            // its recorded PASSED/FAILED result must be left alone.
            return;
        }

        // cancel() returns false when the future had already completed, meaning whenComplete has
        // recorded a genuine result. Only a cancellation we actually won may reset the status.
        if (!future.cancel(true)) {
            return;
        }

        TestResult result = testResults.get(testName);
        if (result != null) {
            result.setNotStarted();
        }
    }

    public void cancelAllTests() {
        new ArrayList<>(runningTests.keySet()).forEach(this::cancelTest);
    }

    public void clearAllResults() {
        cancelAllTests();
        testResults.clear();
    }

    public int getPassedTestCount() {
        return (int) testResults.values().stream()
                .filter(result -> result.getStatus() == TestStatus.PASSED)
                .count();
    }

    public int getFailedTestCount() {
        return (int) testResults.values().stream()
                .filter(result -> result.getStatus() == TestStatus.FAILED)
                .count();
    }

    public int getRunningTestCount() {
        return (int) testResults.values().stream()
                .filter(result -> result.getStatus() == TestStatus.RUNNING)
                .count();
    }

    public int getTotalEnabledTestCount() {
        return (int) testResults.values().stream()
                .filter(result -> result.getStatus() != TestStatus.DISABLED)
                .count();
    }

    public String getOverallStatus() {
        if (areAnyTestsRunning()) {
            return "Tests Running...";
        }

        int passed = getPassedTestCount();
        int failed = getFailedTestCount();
        int total = getTotalEnabledTestCount();

        if (passed + failed == 0) {
            return "No Tests Run";
        }

        if (failed == 0 && passed > 0) {
            return "All Tests Passed";
        } else if (passed == 0 && failed > 0) {
            return "All Tests Failed";
        } else {
            return String.format("%d/%d Passed", passed, total);
        }
    }
}
