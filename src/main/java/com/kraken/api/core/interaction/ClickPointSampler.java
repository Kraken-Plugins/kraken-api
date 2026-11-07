package com.kraken.api.core.interaction;

import net.runelite.api.Point;

import java.awt.Rectangle;
import java.util.Random;

/**
 * Chooses where inside a target's bounds a Kraken click lands, in place of the default point
 * ({@code UIService.getClickingPoint}, biased toward the center). Register one with ClickPoints#setSampler.
 *
 * <p>The sampler may be called from any thread that starts an interaction, so it must be thread safe and must not
 * read client state. It should only handle the targets it knows and return null for the rest.
 */
@FunctionalInterface
public interface ClickPointSampler {

    /**
     * Chooses a click point.
     *
     * @param target what is about to be clicked.
     * @param bounds the target's bounds on the canvas, never empty.
     * @param random a random source to draw from.
     * @return a canvas point inside {@code bounds}, or null to use the default point. A point outside the bounds is
     * ignored and the default point is used.
     */
    Point sample(ClickTarget target, Rectangle bounds, Random random);
}
