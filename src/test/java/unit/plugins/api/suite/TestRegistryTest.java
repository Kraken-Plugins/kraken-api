package unit.plugins.api.suite;

import org.junit.jupiter.api.Test;
import plugins.api.suite.TestGroup;
import plugins.api.suite.TestRegistry;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the registry's uninitialised state.
 *
 * <p>The catalogue is deliberately not built by constructor injection. Every test inherits a
 * {@code config} field of type {@code ApiTestConfig}, which RuneLite binds in a child injector
 * created for the plugin; a {@code @Singleton} whose only dependency is {@code Injector} gets bound
 * in the <em>root</em> injector and is handed the root one, which cannot resolve that config. The
 * result was a {@code ConfigurationException} per test and an empty catalogue.</p>
 *
 * <p>These tests pin down that an uninitialised registry is inert — every accessor answers safely
 * rather than throwing — so the failure mode stays "nothing runs and the log says why" rather than a
 * crash somewhere further away from the cause.</p>
 */
class TestRegistryTest {

    @Test
    void anUninitialisedRegistryReportsItself() {
        assertFalse(new TestRegistry().isInitialized());
    }

    @Test
    void anUninitialisedRegistryHasNoTests() {
        assertTrue(new TestRegistry().all().isEmpty());
    }

    @Test
    void lookupsOnAnUninitialisedRegistryAreEmptyRatherThanThrowing() {
        TestRegistry registry = new TestRegistry();

        assertFalse(registry.byId("BankTest").isPresent());
        assertFalse(registry.byConfigKey("enableBankQuery").isPresent());
        assertFalse(registry.selfCheck().isPresent());
    }

    @Test
    void groupLookupsOnAnUninitialisedRegistryAreEmpty() {
        TestRegistry registry = new TestRegistry();

        for (TestGroup group : TestGroup.values()) {
            assertTrue(registry.group(group).isEmpty(), group + " should be empty");
        }
    }

    @Test
    void theCatalogueIsADefensiveCopy() {
        // all() is rebuilt on re-initialise, so callers must not be able to mutate the live list.
        TestRegistry registry = new TestRegistry();

        registry.all().add(null);

        assertTrue(registry.all().isEmpty());
    }
}
