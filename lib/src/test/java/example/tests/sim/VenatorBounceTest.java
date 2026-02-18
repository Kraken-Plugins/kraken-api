package example.tests.sim;

import com.kraken.api.sim.colosim.Venator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VenatorBounceTest {
    @Test
    void source1x1ToAllSizesMatchesExpectedBounds() {
        assertRegion(1, 1, -2, 2, -2, 2);
        assertRegion(1, 2, -2, 2, -2, 2);
        assertRegion(1, 3, -2, 1, -1, 2);
        assertRegion(1, 4, -2, 0, 0, 2);
        assertRegion(1, 5, -2, 0, 0, 2);
    }

    @Test
    void source2x2ToAllSizesMatchesExpectedBounds() {
        assertRegion(2, 1, -2, 3, -3, 2);
        assertRegion(2, 2, -2, 3, -3, 2);
        assertRegion(2, 3, -3, 2, -2, 3);
        assertRegion(2, 4, -3, 1, -1, 3);
        assertRegion(2, 5, -3, 1, -1, 3);
    }

    @Test
    void source3x3ToAllSizesMatchesExpectedBounds() {
        assertRegion(3, 1, -1, 3, -3, 1);
        assertRegion(3, 2, -1, 3, -3, 1);
        assertRegion(3, 3, -1, 2, -2, 1);
        assertRegion(3, 4, -1, 1, -1, 1);
        assertRegion(3, 5, -1, 1, -1, 1);
    }

    @Test
    void source4x4ToAllSizesMatchesExpectedBounds() {
        assertRegion(4, 1, -1, 4, -4, 1);
        assertRegion(4, 2, -1, 4, -4, 1);
        assertRegion(4, 3, -1, 4, -4, 1);
        assertRegion(4, 4, -1, 3, -3, 1);
        assertRegion(4, 5, -1, 3, -3, 1);
    }

    private void assertRegion(int sourceSize, int targetSize, int minX, int maxX, int minY, int maxY) {
        for (int x = -5; x <= 5; x++) {
            for (int y = -5; y <= 5; y++) {
                boolean inside = x >= minX && x <= maxX && y >= minY && y <= maxY;
                boolean actual = Venator.canBounce(0, 0, sourceSize, x, y, targetSize);
                if (inside) {
                    assertTrue(actual, "Expected true at x=" + x + " y=" + y + " s=" + sourceSize + " t=" + targetSize);
                } else {
                    assertFalse(actual, "Expected false at x=" + x + " y=" + y + " s=" + sourceSize + " t=" + targetSize);
                }
            }
        }
    }
}
