package com.kraken.api.core.interaction.timing;

import net.runelite.api.Point;

import java.util.Random;

/**
 * How long a hand needs between two clicks. {@link ClickScheduler} holds a scheduled click back until this much time
 * has passed since the previous click, from any plugin, unless the click's deadline is about to pass.
 */
public interface ClickGapModel {

    /**
     * Draws the time between two clicks on the same spot.
     *
     * @param random the random source to draw from.
     * @return the gap in milliseconds, above 0.
     */
    int baseGapMs(Random random);

    /**
     * The extra time needed to move between two points on the canvas.
     *
     * @param from the previous click, or null when unknown.
     * @param to   the next click, or null when unknown.
     * @return the travel time in milliseconds, 0 when either point is unknown.
     */
    int travelMs(Point from, Point to);
}
