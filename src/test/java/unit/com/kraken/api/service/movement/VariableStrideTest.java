package unit.com.kraken.api.service.movement;

import com.kraken.api.service.movement.MovementService;
import com.kraken.api.service.movement.VariableStrideConfig;
import net.runelite.api.coords.WorldPoint;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the path shapes {@code applyVariableStride} handles without touching client state.
 *
 * <p>{@link MovementService} uses field injection, so an un-injected instance is safe here: none of
 * the paths under test read an injected field. Anything that walks the client is excluded.</p>
 */
class VariableStrideTest {

    private final MovementService movementService = new MovementService();
    private final VariableStrideConfig config = VariableStrideConfig.builder().build();

    @Test
    void emptyPathReturnsEmptyRatherThanThrowing() {
        // Regression: the size <= 5 branch indexed get(size - 1), so an empty path called get(-1).
        // A global path can legitimately come back empty when the destination is already reached.
        assertTrue(movementService.applyVariableStride(Collections.emptyList(), config).isEmpty());
    }

    @Test
    void nullPathReturnsEmptyRatherThanThrowing() {
        assertTrue(movementService.applyVariableStride(null, config).isEmpty());
    }

    @Test
    void singleTilePathReturnsThatTile() {
        WorldPoint only = new WorldPoint(3200, 3200, 0);

        List<WorldPoint> strided = movementService.applyVariableStride(Collections.singletonList(only), config);

        assertEquals(Collections.singletonList(only), strided);
    }

    @Test
    void shortPathCollapsesToItsFinalTile() {
        // Five or fewer tiles is close enough to click directly, so only the destination survives.
        List<WorldPoint> densePath = straightPath(3200, 3200, 5);
        WorldPoint destination = densePath.get(densePath.size() - 1);

        List<WorldPoint> strided = movementService.applyVariableStride(densePath, config);

        assertEquals(Collections.singletonList(destination), strided);
    }

    @Test
    void longPathIsStridedAndStillEndsAtTheDestination() {
        List<WorldPoint> densePath = straightPath(3200, 3200, 40);
        WorldPoint destination = densePath.get(densePath.size() - 1);

        List<WorldPoint> strided = movementService.applyVariableStride(densePath, config);

        assertFalse(strided.isEmpty(), "a 40 tile path should produce waypoints");
        assertSame(destination, strided.get(strided.size() - 1),
                "striding must never drop the destination tile");
        assertTrue(strided.size() < densePath.size(),
                "striding should yield fewer waypoints than the dense path");
        assertTrue(densePath.containsAll(strided),
                "every waypoint must be a tile from the original path");
    }

    @Test
    void defaultOverloadMatchesTheExplicitConfigOverloadForDegenerateInput() {
        assertTrue(movementService.applyVariableStride(Collections.emptyList()).isEmpty());
    }

    /**
     * Builds a contiguous diagonal run of tiles, mirroring the dense output of the pathfinders.
     *
     * @param startX the world x coordinate of the first tile
     * @param startY the world y coordinate of the first tile
     * @param length the number of tiles to produce
     * @return a dense path of {@code length} tiles
     */
    private List<WorldPoint> straightPath(int startX, int startY, int length) {
        List<WorldPoint> path = new ArrayList<>(length);
        for (int i = 0; i < length; i++) {
            path.add(new WorldPoint(startX + i, startY + i, 0));
        }
        return path;
    }
}
