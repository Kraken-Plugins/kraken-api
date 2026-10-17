package com.kraken.api.service.util.dps.calc;

/**
 * Transforms a single hitsplat into a distribution of possible resulting hitsplats.
 * Used to model post-roll effects such as damage caps, procs, and NPC damage reductions.
 */
@FunctionalInterface
public interface HitTransformer {
    /**
     * Transforms one hitsplat into the distribution of outcomes it can produce.
     * @param hitsplat The hitsplat to transform
     * @return HitDistribution the possible outcomes
     */
    HitDistribution transform(Hitsplat hitsplat);
}
