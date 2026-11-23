package unit.plugins.api.precondition;

import com.kraken.api.Context;
import com.kraken.api.core.script.ScriptStoppedException;
import com.kraken.api.service.pathfinding.GlobalPathfinderConfig;
import com.kraken.api.service.walker.WalkOutcome;
import com.kraken.api.service.walker.WalkResult;
import com.kraken.api.service.walker.Walker;
import com.kraken.api.service.walker.WalkerConfig;
import net.runelite.api.coords.WorldPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import plugins.api.ApiTestConfig;
import plugins.api.precondition.SuiteWalker;
import plugins.api.suite.CancellationToken;
import plugins.api.suite.TestCancelledException;
import plugins.api.world.NamedLocation;
import plugins.api.world.RouteInspector;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Covers the walk the runner makes between tests: the settings it walks with, when it walks at
 * all, and how a walk that is stopped or fails is handed back.
 *
 * <p>Item setup happens before the trip to where a test runs, so a trip that spent a rune, a
 * charge or a coin could use up what the test is about to assert on. The settings are what keeps
 * the planner away from anything that costs something.</p>
 */
class SuiteWalkerTest {

    private static final NamedLocation GRAND_EXCHANGE = NamedLocation.GRAND_EXCHANGE;
    private static final WorldPoint FAR_AWAY = NamedLocation.LUMBRIDGE_CASTLE.getAnchor();

    private final Context ctx = mock(Context.class, RETURNS_DEEP_STUBS);
    private final Walker walker = mock(Walker.class);
    private final RouteInspector routes = mock(RouteInspector.class);
    private final ApiTestConfig config = mock(ApiTestConfig.class);
    private final CancellationToken token = new CancellationToken();
    private final SuiteWalker suiteWalker = new SuiteWalker();

    @BeforeEach
    void setup() throws Exception {
        inject("ctx", ctx);
        inject("walker", walker);
        inject("routes", routes);
        inject("config", config);
        standingAt(FAR_AWAY);
        when(walker.walkTo(any(WorldPoint.class), any(WalkerConfig.class)))
                .thenReturn(WalkResult.success(GRAND_EXCHANGE.getAnchor(), 3));
    }

    private void standingAt(WorldPoint tile) {
        when(ctx.players().local().location()).thenReturn(tile);
    }

    private WalkerConfig settingsOfTheWalk() {
        ArgumentCaptor<WalkerConfig> settings = ArgumentCaptor.forClass(WalkerConfig.class);
        verify(walker).walkTo(eq(GRAND_EXCHANGE.getAnchor()), settings.capture());
        return settings.getValue();
    }

    private void inject(String name, Object value) throws Exception {
        Field field = SuiteWalker.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(suiteWalker, value);
    }

    @Test
    void aTripSpendsNothingByDefault() {
        GlobalPathfinderConfig planning = SuiteWalker.travelConfig(false).getPathfinderConfig();

        assertFalse(planning.isUseTeleportationItems());
        assertFalse(planning.isUseTeleportationSpells());
        assertEquals(0, planning.getCurrencyThreshold(), "no fare of any size is paid");
    }

    @Test
    void aTripStillTakesTheTransportsThatCostNothing() {
        GlobalPathfinderConfig planning = SuiteWalker.travelConfig(false).getPathfinderConfig();

        assertTrue(planning.isUseAgilityShortcuts());
        assertTrue(planning.isUseSpiritTrees());
        assertTrue(planning.isUseGnomeGliders());
        assertTrue(planning.isUseFairyRings());
    }

    @Test
    void consumablesCanBeAllowed() {
        GlobalPathfinderConfig defaults = WalkerConfig.builder().build().getPathfinderConfig();
        GlobalPathfinderConfig planning = SuiteWalker.travelConfig(true).getPathfinderConfig();

        assertTrue(planning.isUseTeleportationItems());
        assertTrue(planning.isUseTeleportationSpells());
        assertEquals(defaults.getCurrencyThreshold(), planning.getCurrencyThreshold());
    }

    @Test
    void aTripPlansCanoesAndMinigameTeleportsLikeAnyWalk() {
        // Neither uses anything up: a canoe is built with an axe the player keeps, and a minigame
        // teleport costs its cooldown. A trip takes them whether or not consumables are allowed.
        for (boolean allowConsumables : new boolean[]{false, true}) {
            GlobalPathfinderConfig planning = SuiteWalker.travelConfig(allowConsumables).getPathfinderConfig();

            assertTrue(planning.isUseCanoes());
            assertTrue(planning.isUseTeleportationMinigames());
        }
    }

    @Test
    void aTripStaysOutOfTheWilderness() {
        assertTrue(SuiteWalker.travelConfig(false).getPathfinderConfig().isAvoidWilderness());
        assertTrue(SuiteWalker.travelConfig(true).getPathfinderConfig().isAvoidWilderness());
    }

    @Test
    void aTripHasLongerThanTheDefaultWalkToArrive() {
        WalkerConfig defaults = WalkerConfig.builder().build();

        assertTrue(SuiteWalker.travelConfig(false).getTimeoutMillis() > defaults.getTimeoutMillis(),
                "Lumbridge to the Grand Exchange is over 300 tiles on foot");
    }

    @Test
    void aFailedWalkIsDescribedByItsOutcomeAndReason() {
        WalkResult stalled = WalkResult.failure(WalkOutcome.STALLED, "stopped making progress", null, 2);

        assertEquals("STALLED (stopped making progress)", SuiteWalker.describeFailure(stalled));
    }

    @Test
    void anywhereNeedsNoTrip() {
        assertTrue(suiteWalker.walkTo(NamedLocation.ANYWHERE, token).isSuccess());
        assertTrue(suiteWalker.walkTo(null, token).isSuccess());

        verifyNoInteractions(walker, routes);
    }

    @Test
    void aPlayerWhoIsAlreadyThereIsNotWalked() {
        standingAt(GRAND_EXCHANGE.getAnchor());

        WalkResult result = suiteWalker.walkTo(GRAND_EXCHANGE, token);

        assertTrue(result.isSuccess());
        assertEquals(GRAND_EXCHANGE.getAnchor(), result.getFinalLocation());
        verifyNoInteractions(walker, routes);
    }

    @Test
    void aTripIsLoggedAndWalkedToTheAnchorWithoutSpendingAnything() {
        WalkResult result = suiteWalker.walkTo(GRAND_EXCHANGE, token);

        assertTrue(result.isSuccess());
        WalkerConfig settings = settingsOfTheWalk();
        assertFalse(settings.getPathfinderConfig().isUseTeleportationItems());
        assertEquals(0, settings.getPathfinderConfig().getCurrencyThreshold());
        verify(routes).planAndLog(FAR_AWAY, GRAND_EXCHANGE.getAnchor(), settings);
    }

    @Test
    void aTripMaySpendOnceThePluginSettingAllowsIt() {
        when(config.travelUsesConsumables()).thenReturn(true);

        suiteWalker.walkTo(GRAND_EXCHANGE, token);

        assertTrue(settingsOfTheWalk().getPathfinderConfig().isUseTeleportationItems());
    }

    @Test
    void aWalkThatDoesNotArriveIsHandedBackWithItsReason() {
        WalkResult stalled = WalkResult.failure(WalkOutcome.STALLED, "a closed gate", FAR_AWAY, 4);
        when(walker.walkTo(any(WorldPoint.class), any(WalkerConfig.class))).thenReturn(stalled);

        assertSame(stalled, suiteWalker.walkTo(GRAND_EXCHANGE, token));
    }

    @Test
    void aRunThatIsAlreadyStoppedStartsNoTrip() {
        token.cancel();

        assertThrows(TestCancelledException.class, () -> suiteWalker.walkTo(GRAND_EXCHANGE, token));
        verify(walker, never()).walkTo(any(WorldPoint.class), any(WalkerConfig.class));
    }

    @Test
    void aStopThatEndsTheWalkersWaitEndsTheTripAsCancelled() {
        when(walker.walkTo(any(WorldPoint.class), any(WalkerConfig.class))).thenAnswer(call -> {
            token.cancel();
            throw new ScriptStoppedException("Script stopped during sleep");
        });

        assertThrows(TestCancelledException.class, () -> suiteWalker.walkTo(GRAND_EXCHANGE, token));
    }

    @Test
    void aWalkThatComesBackAfterAStopIsStillCancelled() {
        // An interrupted search hands back an empty route, so the walker can return an ordinary
        // failure after a stop instead of throwing.
        when(walker.walkTo(any(WorldPoint.class), any(WalkerConfig.class))).thenAnswer(call -> {
            token.cancel();
            return WalkResult.failure(WalkOutcome.NO_ROUTE, "no route", FAR_AWAY, 0);
        });

        assertThrows(TestCancelledException.class, () -> suiteWalker.walkTo(GRAND_EXCHANGE, token));
    }

    @Test
    void anErrorThatIsNotAStopIsPassedOn() {
        when(walker.walkTo(any(WorldPoint.class), any(WalkerConfig.class)))
                .thenThrow(new IllegalStateException("the walker broke"));

        assertThrows(IllegalStateException.class, () -> suiteWalker.walkTo(GRAND_EXCHANGE, token));
    }
}
