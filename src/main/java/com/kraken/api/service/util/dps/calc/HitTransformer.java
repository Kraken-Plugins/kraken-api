package com.kraken.api.service.util.dps.calc;

/**
 * Transforms a single hitsplat into a distribution of possible resulting hitsplats.
 * Used to model post-roll effects such as damage caps, procs, and NPC damage reductions.
 */
@FunctionalInterface
public interface HitTransformer {
    HitDistribution transform(Hitsplat hitsplat);
}
