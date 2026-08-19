package plugins.api;

import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.kraken.api.Context;
import com.kraken.api.core.script.breakhandler.BreakConditions;
import com.kraken.api.core.script.breakhandler.BreakManager;
import com.kraken.api.core.script.breakhandler.BreakProfile;
import com.kraken.api.input.mouse.MouseRecorder;
import com.kraken.api.overlay.MouseOverlay;
import com.kraken.api.overlay.GlobalPathfinderOverlay;
import com.kraken.api.service.map.WorldMapService;
import com.kraken.api.service.pathfinding.LocalPathfinder;
import com.kraken.api.service.ui.login.LoginService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.JagexColors;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.ImageUtil;
import plugins.api.overlay.SceneOverlay;
import plugins.api.suite.RegisteredTest;
import plugins.api.suite.SuiteOptions;
import plugins.api.suite.TestRegistry;
import plugins.api.suite.TestRunner;
import plugins.api.ui.ApiTestPanel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

@Slf4j
@Singleton
@PluginDescriptor(
        name = "API Test",
        description = "A comprehensive example plugin used to test the API with enhanced overlays and configuration.",
        tags = {"example", "automation", "kraken", "testing"}
)
public class ApiTestPlugin extends Plugin {

    @Getter
    @Inject
    private ClientThread clientThread;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private Context context;

    @Inject
    private ApiTestConfig config;

    @Inject
    private MouseOverlay overlay;

    @Inject
    private TestResultManager testResultManager;

    @Inject
    private Client client;

    @Inject
    private SceneOverlay sceneOverlay;

    @Inject
    private MouseOverlay mouseOverlay;

    @Inject
    private GlobalPathfinderOverlay globalPathfinderOverlay;

    @Inject
    private LocalPathfinder pathfinder;

    @Inject
    private WorldMapService worldMapService;

    @Inject
    private ExampleScript exampleScript;

    @Inject
    private LoginService loginService;

    @Inject
    private MouseRecorder mouseRecorder;

    @Inject
    private BreakManager breakManager;

    @Inject
    private TargetTileProvider targetTileProvider;

    @Inject
    private TestRunner testRunner;

    @Inject
    private TestRegistry testRegistry;

    @Inject
    private ClientToolbar clientToolbar;

    private ApiTestPanel apiTestPanel;
    private NavigationButton navigationButton;

    @Getter
    private List<WorldPoint> currentPath = new ArrayList<>();

    @Getter
    private WorldArea targetArea;

    private WorldPoint trueTile;
    private static final String TARGET_TILE = ColorUtil.wrapWithColorTag("Target Tile", JagexColors.CHAT_PRIVATE_MESSAGE_TEXT_TRANSPARENT_BACKGROUND);

    @Provides
    ApiTestConfig provideConfig(final ConfigManager configManager) {
        return configManager.getConfig(ApiTestConfig.class);
    }

    @Subscribe
    private void onMenuOptionClicked(MenuOptionClicked event) {
        if (config.showDebugInfo()) {
            log.info("MouseEvent(Param0={}, Param1={}, MenuAction={}, ItemId={}, id={}, Option={}, Target={}, itemOp={})",
                    event.getParam0(), event.getParam1(), event.getMenuAction().name(), event.getItemId(),
                    event.getId(), event.getMenuOption(), event.getMenuTarget(), event.getItemOp());
        }
    }

    @Subscribe
    private void onConfigChanged(final ConfigChanged event) {
        if (!event.getGroup().equals("testapi")) {
            return;
        }

        String key = event.getKey();

        if(key.equals("login") && config.login()) {
            log.info("Logging into the client...");
            loginService.login();
        }

        if(key.equals("logout") && config.logout()) {
            log.info("Logging out of the client...");
            context.players().local().logout();
        }
        
        if(key.equalsIgnoreCase("pauseScript") && config.pauseScript()) {
            exampleScript.pause();
        } else {
            exampleScript.resume();
        }

        if(key.equals("mouseRecord") && config.mouseRecord()) {
            log.info("Starting recording...");
            mouseRecorder.start("test");
        } else if(key.equals("mouseRecord") && !config.mouseRecord()) {
            log.info("Stopping recording");
            mouseRecorder.stop();
        }

        if (key.equalsIgnoreCase("showMouse")) {
            if (config.showMouse()) {
                overlayManager.add(mouseOverlay);
            } else {
                overlayManager.remove(mouseOverlay);
            }
        }

        if (key.equalsIgnoreCase("renderWalkerRoute")) {
            if (config.renderWalkerRoute()) {
                overlayManager.add(globalPathfinderOverlay);
            } else {
                overlayManager.remove(globalPathfinderOverlay);
            }
        }

        // A per-test toggle starts just that test, preconditions and all. Reading the event's new
        // value means only ticking a box starts a run; un-ticking one no longer re-triggers it.
        if (!"true".equalsIgnoreCase(event.getNewValue())) {
            return;
        }

        testRegistry.byConfigKey(key)
                .ifPresent(test -> testRunner.runSingle(test, currentOptions()));
    }

    /**
     * Builds run options from the current plugin settings.
     *
     * @return the options for a run started right now
     */
    private SuiteOptions currentOptions() {
        return SuiteOptions.builder()
                .establishPreconditions(config.establishPreconditions())
                .includeDestructive(config.includeDestructive())
                .perTestTimeoutMs(config.timeout() * 1000L)
                .build();
    }

    @Override
    protected void startUp() {
        // Built here rather than by constructor injection: the tests need ApiTestConfig, which RuneLite
        // binds in the child injector it creates for this plugin. getInjector() is that injector;
        // the one Guice would inject into a @Singleton is the root, which cannot see the config.
        testRegistry.initialize(getInjector());
        for (RegisteredTest test : testRegistry.all()) {
            testResultManager.registerTest(test.getId(), test.getDisplayName());
        }

        addSidePanel();

        exampleScript.start();

        overlayManager.add(overlay);
        overlayManager.add(sceneOverlay);
        if (config.showMouse()) {
            overlayManager.add(mouseOverlay);
        }
        if (config.renderWalkerRoute()) {
            overlayManager.add(globalPathfinderOverlay);
        }

        breakManager.initialize();

        BreakProfile profile = BreakProfile.builder()
                .name("Jewelry Profile")
                .minRuntime(java.time.Duration.ofMinutes(2))
                .maxRuntime(java.time.Duration.ofMinutes(4))
                .minBreakDuration(java.time.Duration.ofMinutes(2))
                .maxBreakDuration(java.time.Duration.ofMinutes(5))
                .logoutDuringBreak(true)
                .randomizeTimings(true)
                .addBreakCondition(BreakConditions.onLevelReached(context.getClient(), Skill.CRAFTING, 54))
                .build();

        // TODO Find out how you want to test this
        // breakManager.attachScript(exampleScript, profile);
        testResultManager.clearAllResults();
    }

    /**
     * Adds the run panel to the sidebar.
     *
     * <p>Reuses the existing {@code kraken.png} resource rather than adding a binary asset. The panel
     * and button are recreated per enable and removed in {@link #shutDown()}: {@code NavigationButton}
     * compares panels by identity, so a stale button would never be considered equal to a new one and
     * the sidebar would accumulate a duplicate every time the plugin was re-enabled.</p>
     */
    private void addSidePanel() {
        if (navigationButton != null) {
            return;
        }

        apiTestPanel = new ApiTestPanel(testRegistry, testRunner, testResultManager, this::currentOptions);

        navigationButton = NavigationButton.builder()
                .tooltip("API Tests")
                .icon(ImageUtil.resizeImage(
                        ImageUtil.loadImageResource(ApiTestPlugin.class, "/kraken.png"), 16, 16))
                .priority(1)
                .panel(apiTestPanel)
                .build();

        clientToolbar.addNavigation(navigationButton);
    }

    @Override
    protected void shutDown() {
        testRunner.cancel();

        if (navigationButton != null) {
            clientToolbar.removeNavigation(navigationButton);
            navigationButton = null;
            apiTestPanel = null;
        }

        exampleScript.stop();

        overlayManager.remove(overlay);
        overlayManager.remove(sceneOverlay);
        overlayManager.remove(mouseOverlay);
        overlayManager.remove(globalPathfinderOverlay);

        // TODO Find out how you want to test this
//        if (!breakManager.isOnBreak()) {
//            breakManager.detachScript();
//        }
    }

    /**
     * Cancels any in-flight tests when the player ends up back at the login screen, since a test cannot
     * make meaningful assertions about a world it is no longer in.
     *
     * <p>This deliberately does not call {@link #startUp()} or {@link #shutDown()}. RuneLite already
     * invokes those when the plugin is enabled and disabled, and driving them from here re-registered
     * every overlay on each login, wiped all recorded results via {@code clearAllResults()}, and — because
     * {@code HOPPING} was treated as a shutdown — cancelled the whole run whenever a test hopped worlds.
     * {@code HOPPING} is intentionally ignored: it is transient and the client returns to a logged in
     * state on its own.</p>
     *
     * @param event the game state transition published by the client
     */
    @Subscribe
    private void onGameStateChanged(final GameStateChanged event) {
        if (event.getGameState() == GameState.LOGIN_SCREEN && testRunner.isRunning()) {
            log.warn("Returned to the login screen with a run in progress, cancelling it.");
            testRunner.cancel();
        }
    }

    @Subscribe
    public void onGameTick(GameTick event) {
        WorldPoint target = getTargetTile();
        if (target == null) {
            return;
        }

        this.targetArea = new WorldArea(target, 5, 5);
        if (config.renderCurrentPath()) {
            this.currentPath = pathfinder.findPath(client.getLocalPlayer().getWorldLocation(), target);
        }
    }

    @Subscribe
    public void onMenuOpened(MenuOpened event) {
        trueTile = getSelectedWorldPoint();
    }

    @Subscribe
    private void onMenuEntryAdded(MenuEntryAdded event) {
        // Handles a user setting custom destination for movement test within the game world by Shift + clicking a tile
        if (client.isKeyPressed(KeyCode.KC_SHIFT) && event.getOption().equals("Walk here") && event.getTarget().isEmpty()) {
            addMenuEntry(event, "Set", TARGET_TILE, 1);
        }

        // If a right click occurs on the world map get the WorldPoint of where the click occurred
        // and allow players to set a destination that way.
        final Widget map = client.getWidget(InterfaceID.Worldmap.MAP_CONTAINER);
        if(map == null) return;

        Point lastMenuOpenedPoint = client.getMouseCanvasPosition();
        final WorldPoint wp = worldMapService.mapClickToWorldPoint(lastMenuOpenedPoint.getX(), lastMenuOpenedPoint.getY());

        if (wp != null) {
            client.getMenu().createMenuEntry(0)
                    .setOption("Set")
                    .setTarget(ColorUtil.wrapWithColorTag(wp.toString(), JagexColors.CHAT_PRIVATE_MESSAGE_TEXT_TRANSPARENT_BACKGROUND))
                    .setParam0(event.getActionParam0())
                    .setParam1(event.getActionParam1())
                    .setIdentifier(event.getIdentifier())
                    .setType(MenuAction.RUNELITE)
                    .onClick(e -> {
                        if(e.getOption().equalsIgnoreCase("Set")) {
                            targetTileProvider.setManualTile(wp);
                        }
                    });
        }
    }

    private void addMenuEntry(MenuEntryAdded event, String option, String target, int position) {
        List<MenuEntry> entries = new LinkedList<>(Arrays.asList(client.getMenu().getMenuEntries()));
        if (entries.stream().anyMatch(e -> e.getOption().equals(option) && e.getTarget().equals(target))) {
            return;
        }

        client.getMenu().createMenuEntry(position)
                .setOption(option)
                .setTarget(target)
                .setParam0(event.getActionParam0())
                .setParam1(event.getActionParam1())
                .setIdentifier(event.getIdentifier())
                .setType(MenuAction.RUNELITE)
                .onClick(this::onMenuOptionClicked);
    }

    private void onMenuOptionClicked(MenuEntry entry) {
        if (entry.getOption().equals("Set") && entry.getTarget().equals(TARGET_TILE)) {
            targetTileProvider.setManualTile(trueTile);
        }
    }

    /**
     * The tile the movement, camera and pathfinder tests should act on.
     *
     * <p>Delegates to {@link TargetTileProvider} so a running suite can supply a tile programmatically
     * while manual shift click selection keeps working when no suite is active.</p>
     *
     * @return the destination tile, or null when none has been selected
     */
    public WorldPoint getTargetTile() {
        return targetTileProvider.get();
    }

    private WorldPoint getSelectedWorldPoint() {
        if (client.getWidget(ComponentID.WORLD_MAP_MAPVIEW) == null) {
            if (client.getTopLevelWorldView().getSelectedSceneTile() != null) {
                return client.getTopLevelWorldView().isInstance() ?
                        WorldPoint.fromLocalInstance(client, client.getTopLevelWorldView().getSelectedSceneTile().getLocalLocation()) :
                        client.getTopLevelWorldView().getSelectedSceneTile().getWorldLocation();
            }
        }
        return null;
    }

}
