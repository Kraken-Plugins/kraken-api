package plugins.api.suite;

import lombok.Getter;
import plugins.api.requirements.TestRequirements;
import plugins.api.tests.BaseApiTest;

/**
 * A test known to the runner, together with its identity and cached requirements.
 *
 * <p>The id is the class simple name and the display name comes from the test itself. Previously
 * these were hand written strings in the plugin's registration calls, and they had drifted: the
 * registry called one test {@code "BankQuery"} while the test called itself {@code "Bank"}, so a
 * result row and a log line for the same test did not agree.</p>
 */
@Getter
public class RegisteredTest {

    private final String id;
    private final String displayName;
    private final TestGroup group;
    private final BaseApiTest instance;

    /**
     * The {@code ApiTestConfig} key that runs this test on its own.
     *
     * <p>Kept alongside registration rather than in a separate lookup table so the mapping cannot
     * drift from the catalogue. It exists only until the run panel replaces per-test checkboxes.</p>
     */
    private final String configKey;

    private volatile TestRequirements cachedRequirements;

    /**
     * Wraps a test instance.
     *
     * @param group the category the test belongs to
     * @param instance the injected test
     * @param configKey the config toggle that runs this test individually
     */
    public RegisteredTest(TestGroup group, BaseApiTest instance, String configKey) {
        this.id = instance.getClass().getSimpleName();
        this.displayName = instance.getTestName();
        this.group = group;
        this.instance = instance;
        this.configKey = configKey;
    }

    /**
     * The test's declared requirements, resolved once and cached.
     *
     * <p>Caching is safe because {@code requirements()} is contractually pure, and it matters because
     * the planner asks every test for its requirements before a run starts.</p>
     *
     * @return the declared requirements, never null
     */
    public TestRequirements requirements() {
        TestRequirements resolved = cachedRequirements;
        if (resolved == null) {
            resolved = instance.requirements();
            if (resolved == null) {
                resolved = TestRequirements.NONE;
            }
            cachedRequirements = resolved;
        }
        return resolved;
    }

    /**
     * Whether this test is excluded from bulk runs unless explicitly opted into.
     *
     * @return true when the test is marked destructive
     */
    public boolean isDestructive() {
        return requirements().isDestructive();
    }

    @Override
    public String toString() {
        return id;
    }
}
