package plugins.api.tests.service;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kraken.api.Context;
import com.kraken.api.service.tile.AreaService;
import com.kraken.api.service.tile.GameArea;
import plugins.api.tests.BaseApiTest;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.coords.WorldPoint;

/**
 * Exercises {@link AreaService}'s area construction: reachability flood fill, polygon rasterisation
 * and radius expansion.
 *
 * <p>Every area is built relative to wherever the player is standing. This is deliberate: the checks
 * here are geometric, so pinning them to hardcoded Varrock East coordinates only forced the suite to
 * walk somewhere specific to assert something that holds anywhere. The one genuinely location
 * sensitive check, reachability, needs open walkable ground rather than a particular spot.</p>
 *
 * <p><b>Requires:</b> standing on open, walkable ground. Any bank floor works.</p>
 */
@Slf4j
@Singleton
public class AreaServiceTest extends BaseApiTest {

    /** Half-width of the square polygon built around the player. */
    private static final int POLYGON_RADIUS = 4;

    /** Radius used for the reachability flood fill. */
    private static final int REACHABLE_RANGE = 6;

    /** Radius used for the simple square area. */
    private static final int AREA_RADIUS = 3;

    @Inject
    private AreaService areaService;

    // Public fields so SceneOverlay can access them for rendering
    public GameArea reachableArea;
    public GameArea polygonArea;
    public GameArea radiusArea;

    @Override
    protected boolean runTest(Context ctx) throws Exception {
        WorldPoint origin = ctx.players().local().location();
        if (!assertNotNull(origin, "Area service test: could not read the player's location")) {
            return false;
        }

        boolean testsPassed = true;
        testsPassed &= testReachabilityLogic(origin);
        testsPassed &= testPolygonGeneration(origin);
        testsPassed &= testRadiusAreaGeneration(origin);
        return testsPassed;
    }

    /**
     * Verifies the reachability flood fill produces a connected area containing its own origin.
     *
     * @param origin the tile the player is standing on
     * @return true when the reachable area is non-empty and contains the origin
     */
    private boolean testReachabilityLogic(WorldPoint origin) {
        this.reachableArea = areaService.createReachableArea(origin, REACHABLE_RANGE, false);

        if (!assertNotNull(reachableArea, "Area service test: createReachableArea returned nothing")) {
            return false;
        }

        boolean passed = assertTrue(!reachableArea.getTiles().isEmpty(),
                "Area service test: the reachable area around the player is empty. Either the player is "
                        + "boxed in or scene collision data is not being read");

        // The origin is trivially reachable from itself, so its absence means the flood fill dropped
        // its own seed tile.
        passed &= assertTrue(reachableArea.contains(origin),
                "Area service test: the reachable area does not contain its own origin " + origin);

        return passed;
    }

    /**
     * Verifies polygon rasterisation fills the interior of a square and excludes points outside it.
     *
     * <p>A square centred on the player is used rather than an irregular building outline: the
     * property under test is that vertices rasterise into a filled region, and a square makes both
     * the interior and exterior assertions exact rather than eyeballed from a map.</p>
     *
     * @param origin the tile the player is standing on
     * @return true when the polygon contains its centre and excludes a point well outside it
     */
    private boolean testPolygonGeneration(WorldPoint origin) {
        WorldPoint[] vertices = new WorldPoint[] {
                origin.dx(-POLYGON_RADIUS).dy(-POLYGON_RADIUS),
                origin.dx(POLYGON_RADIUS).dy(-POLYGON_RADIUS),
                origin.dx(POLYGON_RADIUS).dy(POLYGON_RADIUS),
                origin.dx(-POLYGON_RADIUS).dy(POLYGON_RADIUS)
        };

        this.polygonArea = areaService.createPolygonArea(vertices);

        if (!assertNotNull(polygonArea, "Area service test: createPolygonArea returned nothing")) {
            return false;
        }

        boolean passed = assertTrue(!polygonArea.getTiles().isEmpty(),
                "Area service test: the polygon rasterised to zero tiles");

        passed &= assertTrue(polygonArea.contains(origin),
                "Area service test: the polygon does not contain its own centre " + origin);

        // One tile inside the boundary, to catch a polygon that only produced its outline.
        WorldPoint nearCorner = origin.dx(POLYGON_RADIUS - 1).dy(POLYGON_RADIUS - 1);
        passed &= assertTrue(polygonArea.contains(nearCorner),
                "Area service test: the polygon is missing the interior tile " + nearCorner
                        + ", suggesting it produced an outline rather than a filled area");

        WorldPoint wellOutside = origin.dx(POLYGON_RADIUS + 25).dy(POLYGON_RADIUS + 25);
        passed &= assertTrue(!polygonArea.contains(wellOutside),
                "Area service test: the polygon claims to contain " + wellOutside
                        + ", which is far outside its vertices");

        return passed;
    }

    /**
     * Verifies a radius area covers its centre and stays within the square implied by the radius.
     *
     * @param origin the tile the player is standing on
     * @return true when the radius area is the expected size and contains its centre
     */
    private boolean testRadiusAreaGeneration(WorldPoint origin) {
        this.radiusArea = areaService.createAreaFromRadius(origin, AREA_RADIUS);

        if (!assertNotNull(radiusArea, "Area service test: createAreaFromRadius returned nothing")) {
            return false;
        }

        boolean passed = assertTrue(!radiusArea.getTiles().isEmpty(),
                "Area service test: the radius area is empty");

        passed &= assertTrue(radiusArea.contains(origin),
                "Area service test: the radius area does not contain its own centre " + origin);

        // A radius of n spans an (2n + 1) square, so anything larger means the radius is being
        // applied incorrectly.
        int maximumTiles = (2 * AREA_RADIUS + 1) * (2 * AREA_RADIUS + 1);
        passed &= assertTrue(radiusArea.getTiles().size() <= maximumTiles,
                "Area service test: radius " + AREA_RADIUS + " produced " + radiusArea.getTiles().size()
                        + " tiles, more than the " + maximumTiles + " a square of that radius can hold");

        return passed;
    }

    @Override
    protected String getTestName() {
        return "Area Service Visual";
    }
}
