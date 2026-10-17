package com.kraken.api.input.mouse.strategy;

import net.runelite.api.Point;

/**
 * A way of moving the cursor from one canvas point to another.
 */
public interface MoveableMouse {
    /**
     * Moves the cursor from a start point to a target point.
     * @param start The point the cursor starts from.
     * @param target The point the cursor should end on.
     */
    void move(Point start, Point target);
}
