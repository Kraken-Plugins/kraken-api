package plugins.api.tests.service;

import com.google.inject.Inject;
import com.kraken.api.Context;
import com.kraken.api.service.pathfinding.GlobalPathfinder;
import com.kraken.api.service.walker.WalkResult;
import com.kraken.api.service.walker.Walker;
import com.kraken.api.service.walker.WalkerConfig;
import com.kraken.api.service.walker.transport.registry.TransportDefinition;
import com.kraken.api.service.walker.transport.registry.TransportRegistry;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.coords.WorldPoint;
import plugins.api.WalkerDestination;
import plugins.api.precondition.SuiteWalker;
import plugins.api.requirements.BankState;
import plugins.api.requirements.SideEffect;
import plugins.api.requirements.TestRequirements;
import plugins.api.tests.BaseApiTest;
import plugins.api.world.NamedLocation;
import plugins.api.world.RouteInspector;

/**
 * Plans and walks a route that cannot be finished without transports: from the Lumbridge Castle
 * courtyard up two flights of stairs to the bank on the top floor, then back down to the foot of
 * the stairs.
 *
 * <p>The route is free to play, needs no item, level or quest, and no teleport lands on either
 * end of it, so arriving on the other plane proves the stairs were operated. Before anything is
 * clicked the planned route is checked against the transport registry: it has to reach the bank
 * by climbing, a transport registered at runtime has to be planned through and has to be gone
 * again once its handle is closed, and a hidden dataset edge has to be routed around until it is
 * brought back.</p>
 *
 * <p>The way down ends at the foot of the stairs and not in the courtyard, because the home
 * teleport lands in the courtyard and the planner prefers it to two flights of stairs.</p>
 */
@Slf4j
public class TransportTest extends BaseApiTest {

    /** The top floor bank, two flights of stairs above the courtyard. */
    private static final WorldPoint CASTLE_BANK = WalkerDestination.LUMBRIDGE_CASTLE_BANK.getTile();

    /** Id of the made up transport that shows a registration reaches the planner. */
    private static final String PROBE_ID = "api-test:planning-probe";

    private static final int GROUND_FLOOR = 0;

    /** Budget for one of the two walks. Each is under forty tiles and two flights of stairs. */
    private static final long WALK_TIMEOUT_MS = 120_000;

    /** Budget for the whole test, above both walks so a slow walk ends with the walker's own result. */
    private static final long TEST_TIMEOUT_MS = 300_000;

    @Inject
    private Walker walker;

    @Inject
    private TransportRegistry transports;

    @Inject
    private RouteInspector routes;

    /**
     * Declares the courtyard as the starting point and a budget that covers both walks.
     *
     * @return requirements that leave the inventory and equipment alone
     */
    @Override
    public TestRequirements requirements() {
        return TestRequirements.builder()
                .location(NamedLocation.LUMBRIDGE_CASTLE)
                .bankState(BankState.CLOSED)
                .sideEffect(SideEffect.MOVES_PLAYER)
                .timeoutMs(TEST_TIMEOUT_MS)
                .build();
    }

    /**
     * Names the test in the panel and the log.
     *
     * @return the display name
     */
    @Override
    public String getTestName() {
        return "Transports";
    }

    @Override
    protected boolean runTest(Context ctx) {
        WorldPoint start = ctx.players().local().location();
        if (!assertNotNull(start, "the player's location could be read")
                || !assertThat(start.getPlane() == GROUND_FLOOR, "the test starts on the ground floor, not at " + start)) {
            return false;
        }

        WalkerConfig walkerConfig = SuiteWalker.travelConfig(false).toBuilder()
                .timeoutMillis(WALK_TIMEOUT_MS)
                .build();

        log.info("Transport test: planning from {} up to the castle bank at {}", start, CASTLE_BANK);
        GlobalPathfinder.PathResult up = routes.planAndLog(start, CASTLE_BANK, walkerConfig);
        if (!checkRoute(up, GROUND_FLOOR, CASTLE_BANK.getPlane(), "up to the castle bank")) {
            return false;
        }
        GlobalPathfinder.TransportUsage firstClimb = RouteInspector.planeChanges(up).get(0);

        boolean passed = checkRegisteredTransportIsPlanned(start, walkerConfig);
        passed &= checkHiddenEdgeIsRoutedAround(start, firstClimb, walkerConfig);

        if (!walk(ctx, CASTLE_BANK, walkerConfig, "up to the castle bank")) {
            return false;
        }

        WorldPoint top = ctx.players().local().location();
        WorldPoint foot = firstClimb.getOrigin();
        log.info("Transport test: planning from {} down to the foot of the stairs at {}", top, foot);
        GlobalPathfinder.PathResult down = routes.planAndLog(top, foot, walkerConfig);
        passed &= checkRoute(down, CASTLE_BANK.getPlane(), GROUND_FLOOR, "down to the foot of the stairs");
        passed &= walk(ctx, foot, walkerConfig, "down to the foot of the stairs");
        return passed;
    }

    /**
     * Builds the made up transport the registration check plans through. It joins two tiles in one
     * step, which no real route can beat, and its handler refuses to operate it, so a walk that
     * was ever planned through it would end with a reason instead of clicking something.
     *
     * @param origin the tile the transport is entered from
     * @param destination the tile it leads to
     * @return the definition to register
     */
    public static TransportDefinition planningProbe(WorldPoint origin, WorldPoint destination) {
        return TransportDefinition.builder()
                .id(PROBE_ID)
                .origin(origin)
                .destination(destination)
                .handler(context -> context.fail("the transport test's planning probe cannot be operated"))
                .build();
    }

    /**
     * Checks a planned route before it is walked: it reaches its destination, and it changes
     * plane by transports entered in the scene.
     *
     * @param route the planned route
     * @param fromPlane the plane the route starts on
     * @param toPlane the plane it has to end on
     * @param label names the route in assertion messages
     * @return true when the route is what the walk needs
     */
    private boolean checkRoute(GlobalPathfinder.PathResult route, int fromPlane, int toPlane, String label) {
        boolean passed = assertThat(route.isComplete(),
                "the route " + label + " reaches its destination: " + RouteInspector.describe(route));
        passed &= assertThat(RouteInspector.climbs(route, fromPlane, toPlane),
                "the route " + label + " goes from plane " + fromPlane + " to plane " + toPlane
                        + " by transports entered in the scene");
        return passed;
    }

    /**
     * Registers the planning probe and checks the next search goes through it, then closes its
     * handle and checks the search after that does not. Nothing is walked while it is registered.
     *
     * @param start where the route begins
     * @param walkerConfig the settings the route is planned with
     * @return true when the registration and its removal both reached the planner
     */
    private boolean checkRegisteredTransportIsPlanned(WorldPoint start, WalkerConfig walkerConfig) {
        GlobalPathfinder.PathResult registered;
        try (TransportRegistry.Registration registration = transports.register(planningProbe(start, CASTLE_BANK))) {
            registered = routes.plan(start, CASTLE_BANK, walkerConfig);
        }
        GlobalPathfinder.PathResult unregistered = routes.plan(start, CASTLE_BANK, walkerConfig);

        boolean passed = assertThat(RouteInspector.usesDefinition(registered, PROBE_ID),
                "a transport registered at runtime is planned through: " + RouteInspector.describe(registered));
        passed &= assertThat(!RouteInspector.usesDefinition(unregistered, PROBE_ID),
                "closing the registration takes the transport out of the next search");
        return passed;
    }

    /**
     * Hides the dataset edge of the first flight of stairs and checks the next search climbs
     * without it, then closes the handle and checks the edge is planned again. The staircase can
     * be entered from more than one tile, so the route stays complete while one edge is hidden.
     *
     * @param start where the route begins
     * @param climb the first flight of stairs the route up takes
     * @param walkerConfig the settings the route is planned with
     * @return true when hiding the edge and bringing it back both reached the planner
     */
    private boolean checkHiddenEdgeIsRoutedAround(WorldPoint start, GlobalPathfinder.TransportUsage climb,
                                                  WalkerConfig walkerConfig) {
        WorldPoint origin = climb.getOrigin();
        WorldPoint destination = climb.getDestination();

        GlobalPathfinder.PathResult hidden;
        try (TransportRegistry.Registration handle = transports.disableEdge(origin, destination)) {
            hidden = routes.plan(start, CASTLE_BANK, walkerConfig);
        }
        GlobalPathfinder.PathResult restored = routes.plan(start, CASTLE_BANK, walkerConfig);

        boolean passed = assertThat(!RouteInspector.usesEdge(hidden, origin, destination),
                "a hidden dataset edge is left out of the next search");
        passed &= assertThat(hidden.isComplete() && RouteInspector.climbs(hidden, GROUND_FLOOR, CASTLE_BANK.getPlane()),
                "the stairs are still climbed from another tile while one edge is hidden: "
                        + RouteInspector.describe(hidden));
        passed &= assertThat(RouteInspector.usesEdge(restored, origin, destination),
                "closing the handle brings the hidden edge back");
        return passed;
    }

    /**
     * Walks to a tile and checks the walker reported success and the player really stands there.
     *
     * @param ctx the API context
     * @param destination the tile to reach
     * @param walkerConfig the settings to walk with
     * @param label names the walk in assertion messages
     * @return true when the walk arrived on the destination's plane within the walker's tolerance
     */
    private boolean walk(Context ctx, WorldPoint destination, WalkerConfig walkerConfig, String label) {
        WalkResult result = walker.walkTo(destination, walkerConfig);
        WorldPoint end = ctx.players().local().location();
        log.info("Transport test: the walk {} finished with {}, standing at {}", label, result, end);

        boolean passed = assertThat(result.isSuccess(),
                "the walk " + label + " arrived: " + SuiteWalker.describeFailure(result));
        passed &= assertThat(end != null && end.getPlane() == destination.getPlane()
                        && end.distanceTo(destination) <= walkerConfig.getTolerance(),
                "the walk " + label + " ended within " + walkerConfig.getTolerance() + " tiles of "
                        + destination + ", not at " + end);
        return passed;
    }
}
