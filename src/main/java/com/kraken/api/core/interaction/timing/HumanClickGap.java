package com.kraken.api.core.interaction.timing;

import lombok.Getter;
import net.runelite.api.Point;

import java.util.Random;

/**
 * A {@link ClickGapModel} shaped like a person's: a lognormal gap between two clicks on one spot (median about
 * 110 ms), plus a Fitts' law travel time to a different spot.
 *
 * <p>Each account gets its own median, spread and travel speed, seeded from its account hash, so two accounts never
 * share one exact distribution while one account keeps the same hand across sessions.
 */
@Getter
public class HumanClickGap implements ClickGapModel {

    /**
     * The width in pixels of a typical click target, such as a prayer icon, used by the travel time.
     */
    public static final int TARGET_WIDTH_PX = 30;

    /**
     * Middle of the range every parameter is drawn from, for when no account is logged in.
     */
    public static final HumanClickGap DEFAULT = new HumanClickGap(110, 0.24, 55);

    /**
     * Median gap between two clicks on the same spot, in milliseconds.
     */
    private final double medianMs;

    /**
     * Spread of the gap's logarithm. 0.24 puts about 95% of gaps between 0.62 and 1.6 times the median.
     */
    private final double logSigma;

    /**
     * Fitts' law slope: milliseconds per bit of index of difficulty.
     */
    private final double msPerBit;

    /**
     * @param medianMs median gap between two clicks on the same spot, in milliseconds.
     * @param logSigma spread of the gap's logarithm.
     * @param msPerBit Fitts' law slope, in milliseconds per bit.
     */
    public HumanClickGap(double medianMs, double logSigma, double msPerBit) {
        this.medianMs = medianMs;
        this.logSigma = logSigma;
        this.msPerBit = msPerBit;
    }

    /**
     * Draws the hand of one account: a median from 95 to 125 ms, a log spread from 0.18 to 0.30 and a travel slope
     * from 40 to 70 ms per bit.
     *
     * @param accountHash the account hash from {@code Client.getAccountHash()}.
     * @return the account's gap model.
     */
    public static HumanClickGap forAccount(long accountHash) {
        Random seeded = new Random(mix(accountHash ^ 0x6b72616b656e4761L));
        double median = 95 + seeded.nextDouble() * 30;
        double sigma = 0.18 + seeded.nextDouble() * 0.12;
        double slope = 40 + seeded.nextDouble() * 30;
        return new HumanClickGap(median, sigma, slope);
    }

    @Override
    public int baseGapMs(Random random) {
        return (int) Math.max(1, Math.round(medianMs * Math.exp(logSigma * random.nextGaussian())));
    }

    @Override
    public int travelMs(Point from, Point to) {
        return travelMs(from, to, TARGET_WIDTH_PX);
    }

    /**
     * Fitts' law travel time: {@link #getMsPerBit()} times {@code log2(distance / width + 1)}.
     *
     * @param from          the previous click, or null when unknown.
     * @param to            the next click, or null when unknown.
     * @param targetWidthPx the width of the target in pixels, or 0 for {@link #TARGET_WIDTH_PX}.
     * @return the travel time in milliseconds, 0 when either point is unknown.
     */
    @Override
    public int travelMs(Point from, Point to, int targetWidthPx) {
        if (from == null || to == null) {
            return 0;
        }

        int width = targetWidthPx > 0 ? targetWidthPx : TARGET_WIDTH_PX;
        double distance = Math.hypot(to.getX() - from.getX(), to.getY() - from.getY());
        double bits = Math.log(distance / width + 1) / Math.log(2);
        return (int) Math.round(msPerBit * bits);
    }

    /**
     * Spreads the bits of a seed so similar account hashes give unrelated draws (SplitMix64's finalizer).
     *
     * @param seed the value to mix.
     * @return the mixed value.
     */
    private static long mix(long seed) {
        long z = seed + 0x9e3779b97f4a7c15L;
        z = (z ^ (z >>> 30)) * 0xbf58476d1ce4e5b9L;
        z = (z ^ (z >>> 27)) * 0x94d049bb133111ebL;
        return z ^ (z >>> 31);
    }
}
