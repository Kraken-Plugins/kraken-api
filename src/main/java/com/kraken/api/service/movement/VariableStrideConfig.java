package com.kraken.api.service.movement;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;

import java.util.concurrent.ThreadLocalRandom;

/**
 * How far along a dense path each walk click jumps.
 *
 * <p>Defaults match VitaLite's 10–15 tile lookahead, sampled from a Gaussian around 12
 * rather than uniformly, so most clicks cluster near the middle of that range.</p>
 */
@Data
@Builder
public class VariableStrideConfig {
    @Builder.Default
    private int minStride = 10;

    @Builder.Default
    private int maxStride = 15;

    @Builder.Default
    private int meanStride = 12;

    @Builder.Default
    private int standardDeviation = 3;

    @Getter
    private boolean tileDeviation;

    public VariableStrideConfig withTileDeviation() {
        this.tileDeviation = true;
        return this;
    }

    /**
     * Computes a random stride value based on the configured mean, standard deviation, and min/max bounds.
     * Most results will cluster around the mean, with fewer results at the extremes.
     * @return The computed stride value.
     */
    public int computeStride() {
        double val = ThreadLocalRandom.current().nextGaussian() * this.standardDeviation + this.meanStride;
        int rounded = (int) Math.round(val);

        return Math.max(this.minStride, Math.min(this.maxStride, rounded));
    }
}
