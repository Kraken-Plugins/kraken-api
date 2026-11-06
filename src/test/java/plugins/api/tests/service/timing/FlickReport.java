package plugins.api.tests.service.timing;

import lombok.Value;

import java.util.Collections;
import java.util.List;

/**
 * The verdict on a phantom boss run: checks that must pass, and measurements reported for a human to read.
 */
@Value
public class FlickReport {

    /**
     * Checks that must all pass for the run to pass.
     */
    List<Check> checks;

    /**
     * Measurements that never fail the run: click offsets, lateness, server signals and prayer drain.
     */
    List<String> details;

    /**
     * @param checks  the checks
     * @param details the measurements
     */
    public FlickReport(List<Check> checks, List<String> details) {
        this.checks = Collections.unmodifiableList(checks);
        this.details = Collections.unmodifiableList(details);
    }

    /**
     * @return true when every check passed
     */
    public boolean passed() {
        return checks.stream().allMatch(Check::isPassed);
    }

    /**
     * @param name the check's name
     * @return the check, or null when there is none by that name
     */
    public Check check(String name) {
        return checks.stream().filter(check -> check.getName().equals(name)).findFirst().orElse(null);
    }

    /**
     * One check and its outcome.
     */
    @Value
    public static class Check {
        String name;
        boolean passed;

        /**
         * What was seen, such as the ticks a prayer was wrong on.
         */
        String detail;
    }
}
