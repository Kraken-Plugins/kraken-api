package unit.com.kraken.api.util;

import com.kraken.api.util.WorldAreaUtils;
import net.runelite.api.coords.WorldPoint;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorldAreaUtilsTest {

    private static final WorldPoint CORNER_A = new WorldPoint(3200, 3200, 0);
    private static final WorldPoint CORNER_B = new WorldPoint(3210, 3220, 0);

    @Test
    void containsPointsInsideAndOnTheInclusiveBoundary() {
        assertTrue(WorldAreaUtils.contains(new WorldPoint(3205, 3210, 0), CORNER_A, CORNER_B));
        assertTrue(WorldAreaUtils.contains(CORNER_A, CORNER_A, CORNER_B));
        assertTrue(WorldAreaUtils.contains(CORNER_B, CORNER_A, CORNER_B));
        assertTrue(WorldAreaUtils.contains(new WorldPoint(3200, 3220, 0), CORNER_A, CORNER_B));
    }

    @Test
    void rejectsPointsOutsideEitherAxis() {
        assertFalse(WorldAreaUtils.contains(new WorldPoint(3199, 3210, 0), CORNER_A, CORNER_B));
        assertFalse(WorldAreaUtils.contains(new WorldPoint(3211, 3210, 0), CORNER_A, CORNER_B));
        assertFalse(WorldAreaUtils.contains(new WorldPoint(3205, 3199, 0), CORNER_A, CORNER_B));
        assertFalse(WorldAreaUtils.contains(new WorldPoint(3205, 3221, 0), CORNER_A, CORNER_B));
    }

    @Test
    void cornerOrderDoesNotMatter() {
        WorldPoint point = new WorldPoint(3205, 3210, 0);
        assertTrue(WorldAreaUtils.contains(point, CORNER_A, CORNER_B));
        assertTrue(WorldAreaUtils.contains(point, CORNER_B, CORNER_A));
    }

    @Test
    void ignoresPlane() {
        // The area corners are on plane 0; a point on plane 2 inside the x/y box still matches,
        // matching the query-layer withinArea filters this backs.
        assertTrue(WorldAreaUtils.contains(new WorldPoint(3205, 3210, 2), CORNER_A, CORNER_B));
    }
}
