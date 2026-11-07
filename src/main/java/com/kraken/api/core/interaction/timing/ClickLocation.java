package com.kraken.api.core.interaction.timing;

import lombok.Value;
import net.runelite.api.Point;

import java.awt.Rectangle;

/**
 * Where a scheduled click will land and the bounds of the target it lands on, so the travel time from the previous
 * click can use the target's real size.
 */
@Value
public class ClickLocation {

    /**
     * The canvas point the click lands on.
     */
    Point point;

    /**
     * The target's bounds on the canvas, or null when unknown.
     */
    Rectangle bounds;

    /**
     * @param point the canvas point the click lands on.
     * @return a location with unknown target bounds, or null when {@code point} is null.
     */
    public static ClickLocation of(Point point) {
        return point == null ? null : new ClickLocation(point, null);
    }

    /**
     * @param point  the canvas point the click lands on.
     * @param bounds the target's bounds on the canvas, or null.
     * @return the location, or null when {@code point} is null.
     */
    public static ClickLocation of(Point point, Rectangle bounds) {
        return point == null ? null : new ClickLocation(point, bounds == null ? null : new Rectangle(bounds));
    }

    /**
     * The width a hand has to hit, for Fitts' law: the smaller side of the target's bounds.
     *
     * @return the smaller side of the bounds in pixels, or 0 when the bounds are unknown or empty.
     */
    public int getTargetWidthPx() {
        if (bounds == null || bounds.width <= 0 || bounds.height <= 0) {
            return 0;
        }
        return Math.min(bounds.width, bounds.height);
    }
}
