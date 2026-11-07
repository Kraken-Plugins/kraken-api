package com.kraken.api.core.interaction;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kraken.api.service.ui.UIService;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Point;

import java.awt.Rectangle;
import java.util.Random;
import java.util.function.Supplier;

/**
 * Chooses where on its target each Kraken click lands. {@link InteractionManager} asks it for every click on a
 * widget, a container item or a scene target. With no {@link ClickPointSampler} registered, or when the sampler
 * returns null, the point is the default one: {@link UIService#getClickingPoint} with randomization, biased toward
 * the center of the target.
 *
 * <p>One instance serves every plugin: Guice creates it in RuneLite's injector, so a sampler registered by one plugin
 * or shared library applies to clicks from all of them.
 */
@Slf4j
@Singleton
public class ClickPoints {

    private final Random random;
    private final Supplier<Point> previousClick;

    @Getter
    @Setter
    private volatile ClickPointSampler sampler;

    /**
     * @param clickHistory the last click from any source, passed to the sampler as {@link ClickTarget#getPreviousClick()}.
     */
    @Inject
    public ClickPoints(ClickHistory clickHistory) {
        this(new Random(), clickHistory::getLastClickPoint);
    }

    /**
     * Creates click points with their own random source and previous click, for tests.
     *
     * @param random        the random source handed to the sampler.
     * @param previousClick where the previous click landed, or null.
     */
    public ClickPoints(Random random, Supplier<Point> previousClick) {
        this.random = random;
        this.previousClick = previousClick;
    }

    /**
     * Chooses the canvas point a click on {@code target} lands on.
     *
     * @param target what is being clicked, or null when unknown.
     * @param bounds the target's clickbox on the canvas, or a fallback rectangle when it could not be read.
     * @return the sampler's point when it gives one inside {@code bounds}, otherwise the default point. A missing or
     * fallback clickbox always gives the default point, (1, 1).
     */
    public Point choose(ClickTarget target, Rectangle bounds) {
        ClickPointSampler current = sampler;
        if (current == null || target == null || UIService.isFallbackClickbox(bounds)
                || bounds.width <= 0 || bounds.height <= 0) {
            return UIService.getClickingPoint(bounds, true);
        }

        try {
            Point point = current.sample(target.withPreviousClick(previousClick.get()), new Rectangle(bounds), random);
            if (point != null && bounds.contains(point.getX(), point.getY())) {
                return point;
            }

            if (point != null) {
                log.debug("Click point sampler returned {} outside {} for {}", point, bounds, target);
            }
        } catch (RuntimeException e) {
            log.warn("Click point sampler failed for {}", target, e);
        }
        return UIService.getClickingPoint(bounds, true);
    }
}
