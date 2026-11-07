package com.kraken.api.core.interaction;

import net.runelite.api.Point;
import org.junit.jupiter.api.Test;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClickPointsTest {

    private static final Rectangle ICON = new Rectangle(600, 300, 37, 37);
    private static final ClickTarget PRAYER = ClickTarget.widget(35454997, -1, -1, "Activate");

    private final List<ClickTarget> seen = new ArrayList<>();
    private final ClickPoints clickPoints = new ClickPoints(new Random(1), () -> new Point(5, 6));

    @Test
    void withoutASamplerEveryPointIsInsideTheTarget() {
        assertNull(clickPoints.getSampler());
        for (int i = 0; i < 500; i++) {
            assertDefaultPoint(clickPoints.choose(PRAYER, ICON));
        }
    }

    /**
     * The default point is drawn from the target's edges inclusive, so it can sit on the right or bottom edge.
     */
    private static void assertDefaultPoint(Point point) {
        assertTrue(point.getX() >= ICON.x && point.getX() <= ICON.x + ICON.width, point.toString());
        assertTrue(point.getY() >= ICON.y && point.getY() <= ICON.y + ICON.height, point.toString());
    }

    @Test
    void theSamplersPointIsUsedAndSeesThePreviousClick() {
        clickPoints.setSampler((target, bounds, random) -> {
            seen.add(target);
            return new Point(bounds.x + 3, bounds.y + 4);
        });

        assertEquals(new Point(603, 304), clickPoints.choose(PRAYER, ICON));
        assertEquals(1, seen.size());
        assertEquals(new Point(5, 6), seen.get(0).getPreviousClick());
        assertEquals(35454997, seen.get(0).getWidgetId());
        assertEquals("Activate", seen.get(0).getAction());
    }

    @Test
    void aPointOutsideTheTargetFallsBackToTheDefault() {
        clickPoints.setSampler((target, bounds, random) -> new Point(bounds.x - 50, bounds.y));

        assertDefaultPoint(clickPoints.choose(PRAYER, ICON));
    }

    @Test
    void aFailingSamplerFallsBackToTheDefault() {
        clickPoints.setSampler((target, bounds, random) -> {
            throw new IllegalStateException("broken");
        });

        assertDefaultPoint(clickPoints.choose(PRAYER, ICON));
    }

    @Test
    void aSamplerThatPassesUsesTheDefault() {
        clickPoints.setSampler((target, bounds, random) -> null);

        assertDefaultPoint(clickPoints.choose(PRAYER, ICON));
    }

    @Test
    void aMissingClickboxNeverReachesTheSampler() {
        clickPoints.setSampler((target, bounds, random) -> {
            seen.add(target);
            return new Point(10, 10);
        });

        assertEquals(new Point(1, 1), clickPoints.choose(PRAYER, null));
        assertEquals(new Point(1, 1), clickPoints.choose(PRAYER, new Rectangle(-1, -1, 800, 600)));
        assertEquals(new Point(1, 1), clickPoints.choose(PRAYER, new Rectangle(0, 0, 800, 600)));
        assertTrue(seen.isEmpty());
    }

    @Test
    void theSamplerGetsACopyOfTheBounds() {
        Rectangle bounds = new Rectangle(ICON);
        clickPoints.setSampler((target, box, random) -> {
            box.translate(100, 100);
            return null;
        });

        clickPoints.choose(PRAYER, bounds);

        assertEquals(ICON, bounds);
    }
}
