package plugins.api.suite;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import plugins.api.tests.BaseApiTest;
import plugins.api.tests.SelfCheckTest;
import plugins.api.tests.input.MouseTest;
import plugins.api.tests.interaction.WidgetActionTest;
import plugins.api.tests.interaction.WidgetSubActionTest;
import plugins.api.tests.interaction.WidgetTargetGameObjectTest;
import plugins.api.tests.interaction.WidgetTargetNpcTest;
import plugins.api.tests.interaction.WidgetTargetWidgetTest;
import plugins.api.tests.query.BankInventoryTest;
import plugins.api.tests.query.BankTest;
import plugins.api.tests.query.DepositBoxTest;
import plugins.api.tests.query.EquipmentTest;
import plugins.api.tests.query.GameObjectTest;
import plugins.api.tests.query.GroundObjectTest;
import plugins.api.tests.query.InventoryTest;
import plugins.api.tests.query.NpcTest;
import plugins.api.tests.query.PlayerTest;
import plugins.api.tests.query.WidgetTest;
import plugins.api.tests.query.WorldQueryTest;
import plugins.api.tests.service.AreaServiceTest;
import plugins.api.tests.service.BankServiceTest;
import plugins.api.tests.service.CameraServiceTest;
import plugins.api.tests.service.DepositBoxServiceTest;
import plugins.api.tests.service.DialogueServiceTest;
import plugins.api.tests.service.DpsServiceTest;
import plugins.api.tests.service.GlobalPathfinderTest;
import plugins.api.tests.service.GrandExchangeServiceTest;
import plugins.api.tests.service.MovementServiceTest;
import plugins.api.tests.service.PathfinderServiceTest;
import plugins.api.tests.service.PrayerServiceTest;
import plugins.api.tests.service.ProcessingServiceTest;
import plugins.api.tests.service.SpellServiceTest;
import plugins.api.tests.service.TaskChainTest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * The catalogue of every in-client test, in a stable registration order.
 *
 * <p>Replaces a thirty-one parameter {@code @Inject} method whose call sites each carried a
 * hand written display name and a config key. Resolving instances through the injector keeps
 * registration to one line per test and removes the possibility of those strings drifting.</p>
 *
 * <p><b>Must be initialised with the plugin's own injector</b>, via
 * {@link #initialize(com.google.inject.Injector)} from {@code startUp()}. RuneLite binds
 * {@code ApiTestConfig} in a child injector created for the plugin, and every test inherits a
 * {@code config} field from {@code BaseApiTest}, so resolving a test from the root injector fails
 * with "already configured on one or more child injectors or private modules". Taking the injector
 * as a constructor dependency would silently get the root one, because this class has no other
 * child-scoped dependency to pull its own binding down into the child.</p>
 */
@Slf4j
@Singleton
public class TestRegistry {

    private final List<RegisteredTest> tests = new ArrayList<>();
    private final Map<String, RegisteredTest> byId = new LinkedHashMap<>();
    private final Map<String, RegisteredTest> byConfigKey = new LinkedHashMap<>();

    /**
     * Builds the catalogue.
     *
     * <p>Idempotent in effect: calling it again rebuilds from scratch, which is what happens when the
     * plugin is disabled and re-enabled and RuneLite hands out a fresh child injector.</p>
     *
     * @param injector the plugin's injector, from {@code Plugin.getInjector()}. Must be the child
     *                 injector that has {@code ApiTestConfig} bound, not the root one
     */
    public synchronized void initialize(Injector injector) {
        tests.clear();
        byId.clear();
        byConfigKey.clear();

        register(injector, SelfCheckTest.class, "enableSelfCheck", TestGroup.SELF_CHECK);

        register(injector, BankTest.class, "enableBankQuery", TestGroup.QUERY);
        register(injector, BankInventoryTest.class, "enableBankInventoryQuery", TestGroup.QUERY);
        register(injector, InventoryTest.class, "enableInventoryQuery", TestGroup.QUERY);
        register(injector, EquipmentTest.class, "enableEquipmentQuery", TestGroup.QUERY);
        register(injector, GameObjectTest.class, "enableGameObjectQuery", TestGroup.QUERY);
        register(injector, GroundObjectTest.class, "enableGroundObjectQuery", TestGroup.QUERY);
        register(injector, NpcTest.class, "enableNpcQuery", TestGroup.QUERY);
        register(injector, PlayerTest.class, "enablePlayerQuery", TestGroup.QUERY);
        register(injector, WidgetTest.class, "enableWidgetQuery", TestGroup.QUERY);
        register(injector, WorldQueryTest.class, "enableWorldQuery", TestGroup.QUERY);
        register(injector, DepositBoxTest.class, "enableDepositBoxQuery", TestGroup.QUERY);

        register(injector, PrayerServiceTest.class, "enablePrayer", TestGroup.SERVICE);
        register(injector, BankServiceTest.class, "enableBankServiceTests", TestGroup.SERVICE);
        register(injector, DepositBoxServiceTest.class, "enableDepositBoxService", TestGroup.SERVICE);
        register(injector, SpellServiceTest.class, "enableSpell", TestGroup.SERVICE);
        register(injector, MovementServiceTest.class, "enableMovement", TestGroup.SERVICE);
        register(injector, CameraServiceTest.class, "enableCamera", TestGroup.SERVICE);
        register(injector, PathfinderServiceTest.class, "enablePathfinder", TestGroup.SERVICE);
        register(injector, GlobalPathfinderTest.class, "enableGlobalPathfinder", TestGroup.SERVICE);
        register(injector, TaskChainTest.class, "enableTaskChain", TestGroup.SERVICE);
        register(injector, DialogueServiceTest.class, "enableDialogueService", TestGroup.SERVICE);
        register(injector, ProcessingServiceTest.class, "enableProcessingService", TestGroup.SERVICE);
        register(injector, AreaServiceTest.class, "enableAreaService", TestGroup.SERVICE);
        register(injector, GrandExchangeServiceTest.class, "enableGrandExchangeService", TestGroup.SERVICE);
        register(injector, DpsServiceTest.class, "enableDpsService", TestGroup.SERVICE);

        register(injector, WidgetActionTest.class, "widgetAction", TestGroup.INTERACTION);
        register(injector, WidgetSubActionTest.class, "widgetSubAction", TestGroup.INTERACTION);
        register(injector, WidgetTargetNpcTest.class, "widgetTargetOnNpc", TestGroup.INTERACTION);
        register(injector, WidgetTargetGameObjectTest.class, "widgetTargetOnGameObject", TestGroup.INTERACTION);
        register(injector, WidgetTargetWidgetTest.class, "widgetTargetOnWidget", TestGroup.INTERACTION);

        register(injector, MouseTest.class, "enableMouseTest", TestGroup.INPUT);

        if (tests.isEmpty()) {
            log.error("No in-client tests could be registered. The harness cannot run anything. "
                    + "This usually means TestRegistry was handed the root injector rather than the "
                    + "plugin's own one, so ApiTestConfig could not be resolved");
        } else {
            log.info("Registered {} in-client tests", tests.size());
        }
    }

    /**
     * Whether the catalogue has been built.
     *
     * @return true once at least one test is registered
     */
    public synchronized boolean isInitialized() {
        return !tests.isEmpty();
    }

    /**
     * Resolves and catalogues a single test.
     *
     * @param injector used to obtain the instance
     * @param type the test class
     * @param configKey the config toggle that runs it individually
     * @param group the category it belongs to
     */
    private void register(Injector injector, Class<? extends BaseApiTest> type, String configKey,
                          TestGroup group) {
        try {
            RegisteredTest registered = new RegisteredTest(group, injector.getInstance(type), configKey);
            tests.add(registered);
            byId.put(registered.getId(), registered);
            byConfigKey.put(configKey, registered);
        } catch (Exception e) {
            // One unresolvable test must not take the whole harness down with it.
            log.error("Could not register test {}", type.getSimpleName(), e);
        }
    }

    /**
     * Every registered test, in registration order.
     *
     * @return the catalogue
     */
    public synchronized List<RegisteredTest> all() {
        return new ArrayList<>(tests);
    }

    /**
     * Every test in a category, in registration order.
     *
     * @param group the category to filter by
     * @return the matching tests
     */
    public synchronized List<RegisteredTest> group(TestGroup group) {
        return tests.stream()
                .filter(test -> test.getGroup() == group)
                .collect(Collectors.toList());
    }

    /**
     * Looks a test up by its id, which is its class simple name.
     *
     * @param id the test id
     * @return the test, or empty when unknown
     */
    public synchronized Optional<RegisteredTest> byId(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    /**
     * Looks a test up by the config toggle that runs it.
     *
     * @param configKey the {@code ApiTestConfig} key that changed
     * @return the test, or empty when the key does not name one
     */
    public synchronized Optional<RegisteredTest> byConfigKey(String configKey) {
        return Optional.ofNullable(byConfigKey.get(configKey));
    }

    /**
     * The self check, which the runner runs first and treats as a gate on the rest.
     *
     * @return the self check test, or empty when it failed to register
     */
    public synchronized Optional<RegisteredTest> selfCheck() {
        return tests.stream()
                .filter(test -> test.getGroup() == TestGroup.SELF_CHECK)
                .findFirst();
    }
}
