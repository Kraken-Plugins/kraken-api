package plugins.api.suite;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kraken.api.Context;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.coords.WorldPoint;
import plugins.api.TestResultManager;
import plugins.api.precondition.PreconditionEngine;
import plugins.api.precondition.PreconditionResult;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Runs tests one at a time on a dedicated worker thread, establishing each one's preconditions first.
 *
 * <p>A single owned thread rather than {@code CompletableFuture} on the common pool, for one decisive
 * reason: {@code CompletableFuture.cancel(true)} cannot interrupt a task that is already executing, so
 * the old Stop button left the test running and merely orphaned its result. {@code SleepService}
 * already checks {@code Thread.isInterrupted()} in every wait loop, so interrupting a thread we own
 * aborts the test within a poll interval. One worker also guarantees tests never overlap, which
 * matters when they all share one inventory.</p>
 */
@Slf4j
@Singleton
public class TestRunner {

    @Inject
    private Context ctx;

    @Inject
    private TestRegistry registry;

    @Inject
    private TestOrderPlanner planner;

    @Inject
    private PreconditionEngine engine;

    @Inject
    private BaselineRestorer baseline;

    @Inject
    private TestResultManager results;

    private final ScheduledExecutorService timeoutScheduler =
            Executors.newSingleThreadScheduledExecutor(runnable -> {
                Thread thread = new Thread(runnable, "kraken-api-test-timeout");
                thread.setDaemon(true);
                return thread;
            });

    private final AtomicBoolean running = new AtomicBoolean(false);
    private volatile CancellationToken token;

    /**
     * Runs every eligible registered test.
     *
     * @param options run configuration
     * @return true if the run was started, false if one is already in progress
     */
    public boolean runAll(SuiteOptions options) {
        return runSelection(registry.all(), options);
    }

    /**
     * Runs every test in one category.
     *
     * @param group the category to run
     * @param options run configuration
     * @return true if the run was started
     */
    public boolean runGroup(TestGroup group, SuiteOptions options) {
        return runSelection(registry.group(group), options);
    }

    /**
     * Runs a single test, still establishing its preconditions.
     *
     * @param test the test to run
     * @param options run configuration
     * @return true if the run was started
     */
    public boolean runSingle(RegisteredTest test, SuiteOptions options) {
        // Destructive tests are excluded from bulk runs but must still be runnable on request,
        // otherwise there would be no way to exercise them at all.
        return runSelection(Collections.singletonList(test),
                options.toBuilder().includeDestructive(true).abortOnSelfCheckFailure(false).build());
    }

    /**
     * Runs an explicit set of tests.
     *
     * @param tests the tests to run
     * @param options run configuration
     * @return true if the run was started, false if one is already in progress
     */
    public boolean runSelection(List<RegisteredTest> tests, SuiteOptions options) {
        if (tests == null || tests.isEmpty()) {
            log.warn("Nothing to run");
            return false;
        }

        if (!running.compareAndSet(false, true)) {
            log.warn("A run is already in progress");
            return false;
        }

        CancellationToken runToken = new CancellationToken();
        this.token = runToken;

        Thread thread = new Thread(() -> {
            runToken.bind(Thread.currentThread());
            try {
                execute(tests, options, runToken);
            } catch (TestCancelledException e) {
                log.info("Run cancelled");
            } catch (Exception e) {
                log.error("Run aborted by an unexpected error", e);
            } finally {
                // Clear any interrupt the cancellation left set so the flag cannot leak.
                Thread.interrupted();
                results.clearRunningStates();
                results.endRun();
                running.set(false);
            }
        }, "kraken-api-test-runner");

        // The thread itself is not retained: cancellation goes through the token, which holds the
        // reference it needs to interrupt. Keeping a second handle here would just be a field that is
        // written and never read.
        thread.setDaemon(true);
        thread.start();
        return true;
    }

    /**
     * Requests cancellation of the current run.
     */
    public void cancel() {
        CancellationToken active = token;
        if (active != null) {
            log.info("Cancelling the current run");
            active.cancel();
        }
    }

    /**
     * Whether a run is in progress.
     *
     * @return true while the worker is active
     */
    public boolean isRunning() {
        return running.get();
    }

    /**
     * Previews the itinerary without running anything.
     *
     * @param tests the tests to order
     * @param includeDestructive whether to keep tests marked destructive
     * @return the ordered plan
     */
    public List<PlannedStep> preview(List<RegisteredTest> tests, boolean includeDestructive) {
        return planner.plan(tests, playerLocation(), includeDestructive);
    }

    // ---------- worker ----------

    /**
     * The body of a run, executed on the worker thread.
     *
     * @param tests the tests to run
     * @param options run configuration
     * @param runToken cancellation token bound to this thread
     */
    private void execute(List<RegisteredTest> tests, SuiteOptions options, CancellationToken runToken) {
        List<PlannedStep> plan = planner.plan(tests, playerLocation(), options.isIncludeDestructive());

        if (plan.isEmpty()) {
            log.warn("Every selected test was filtered out of the plan");
            return;
        }

        results.beginRun(plan.size());
        log.info("Starting run of {} tests", plan.size());

        for (PlannedStep step : plan) {
            runToken.throwIfCancelled("between tests");

            boolean passed = runOne(step, options, runToken);

            // The self check validates the primitives every other test's setup is built from. If it
            // fails, the rest of the run would produce a wall of identical setup failures that say
            // nothing, so stop and say so once instead.
            if (!passed && step.getTest().getGroup() == TestGroup.SELF_CHECK
                    && options.isAbortOnSelfCheckFailure()) {
                int remaining = plan.size() - step.getIndex() - 1;
                log.error("Self check failed: the harness cannot drive the client. "
                        + "{} remaining tests were not run", remaining);
                skipRemainder(plan, step.getIndex() + 1,
                        "Self check failed, so the harness cannot drive the client");
                return;
            }
        }

        baseline.restoreAtEndOfRun(runToken);
        log.info("Run finished: {}", results.getOverallStatus());
    }

    /**
     * Establishes preconditions for one test and runs it.
     *
     * @param step the planned step
     * @param options run configuration
     * @param runToken cancellation token
     * @return true when the test passed
     */
    private boolean runOne(PlannedStep step, SuiteOptions options, CancellationToken runToken) {
        RegisteredTest test = step.getTest();
        results.beginTest(test.getId(), test.getDisplayName(), step.getIndex() + 1);

        long startedAt = System.currentTimeMillis();

        try {
            if (options.isEstablishPreconditions()) {
                PreconditionResult precondition =
                        engine.satisfy(test.requirements(), runToken, results::setPhase);

                if (!precondition.satisfied()) {
                    recordUnsatisfied(test, precondition);
                    return false;
                }
            }

            results.setPhase("Running " + test.getDisplayName());
            boolean passed = runWithTimeout(test, options, runToken);
            results.complete(test.getId(), passed, System.currentTimeMillis() - startedAt);
            return passed;

        } catch (TestCancelledException e) {
            results.markCancelled(test.getId());
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            results.markCancelled(test.getId());
            throw new TestCancelledException("Interrupted during " + test.getId());
        } catch (Exception e) {
            log.error("Test {} threw", test.getId(), e);
            results.fail(test.getId(), e, System.currentTimeMillis() - startedAt);
            return false;
        } finally {
            try {
                baseline.restoreBetween(test, runToken);
            } catch (TestCancelledException e) {
                throw e;
            } catch (Exception e) {
                log.warn("Cleanup after {} failed", test.getId(), e);
            }
        }
    }

    /**
     * Runs a test body with a watchdog that cancels it if it overruns.
     *
     * <p>The worker cannot time itself out while blocked, so a scheduled task cancels the token — and
     * therefore interrupts the worker — from outside.</p>
     *
     * @param test the test to run
     * @param options run configuration
     * @param runToken cancellation token
     * @return true when the test passed
     * @throws Exception whatever the test body threw
     */
    private boolean runWithTimeout(RegisteredTest test, SuiteOptions options,
                                   CancellationToken runToken) throws Exception {
        long declared = test.requirements().getTimeoutMs();
        long timeout = declared > 0 ? declared : options.getPerTestTimeoutMs();

        ScheduledFuture<?> watchdog = timeoutScheduler.schedule(() -> {
            log.error("Test {} exceeded its {}ms budget, cancelling it", test.getId(), timeout);
            runToken.cancel();
        }, timeout, TimeUnit.MILLISECONDS);

        try {
            return test.getInstance().runSynchronously();
        } finally {
            watchdog.cancel(false);
        }
    }

    /**
     * Translates an unmet precondition into the right kind of skip.
     *
     * @param test the test that could not run
     * @param precondition why it could not run
     */
    private void recordUnsatisfied(RegisteredTest test, PreconditionResult precondition) {
        String reason = precondition.getReason();

        switch (precondition.getOutcome()) {
            case SETUP_FAILED:
                log.warn("Setup failed for {}: {}", test.getId(), reason);
                results.skip(test.getId(), TestResultManager.SkipKind.SETUP_FAILED, reason);
                break;
            case CANCELLED:
                results.markCancelled(test.getId());
                throw new TestCancelledException("Cancelled setting up " + test.getId());
            default:
                log.info("Skipping {}: {}", test.getId(), reason);
                results.skip(test.getId(), TestResultManager.SkipKind.UNMET_REQUIREMENT, reason);
                break;
        }
    }

    /**
     * Marks every remaining planned test as skipped.
     *
     * @param plan the itinerary
     * @param from the index to start skipping at
     * @param reason why they were not run
     */
    private void skipRemainder(List<PlannedStep> plan, int from, String reason) {
        for (int i = from; i < plan.size(); i++) {
            RegisteredTest test = plan.get(i).getTest();
            results.skip(test.getId(), TestResultManager.SkipKind.SETUP_FAILED, reason);
        }
    }

    /**
     * Reads the player's tile, tolerating not being logged in.
     *
     * @return the player's location, or null when unavailable
     */
    private WorldPoint playerLocation() {
        try {
            return ctx.players().local().location();
        } catch (Exception e) {
            return null;
        }
    }
}
