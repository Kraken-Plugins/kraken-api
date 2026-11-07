package com.kraken.api.core.interaction.timing;

import net.runelite.api.Point;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HumanClickGapTest {

    @Test
    void anAccountKeepsItsHandAcrossSessions() {
        HumanClickGap first = HumanClickGap.forAccount(123_456_789L);
        HumanClickGap second = HumanClickGap.forAccount(123_456_789L);

        assertEquals(first.getMedianMs(), second.getMedianMs());
        assertEquals(first.getLogSigma(), second.getLogSigma());
        assertEquals(first.getMsPerBit(), second.getMsPerBit());
    }

    @Test
    void neighbouringAccountsGetDifferentHands() {
        HumanClickGap first = HumanClickGap.forAccount(1000L);
        HumanClickGap second = HumanClickGap.forAccount(1001L);

        assertNotEquals(first.getMedianMs(), second.getMedianMs());
    }

    @Test
    void everyAccountDrawsInsideTheDocumentedRanges() {
        for (long hash = 0; hash < 500; hash++) {
            HumanClickGap gap = HumanClickGap.forAccount(hash * 7_919);
            assertTrue(gap.getMedianMs() >= 95 && gap.getMedianMs() <= 125);
            assertTrue(gap.getLogSigma() >= 0.18 && gap.getLogSigma() <= 0.30);
            assertTrue(gap.getMsPerBit() >= 40 && gap.getMsPerBit() <= 70);
        }
    }

    @Test
    void gapsCentreOnTheMedian() {
        HumanClickGap gap = new HumanClickGap(110, 0.24, 55);
        Random random = new Random(7);
        int[] draws = new int[5_001];
        for (int i = 0; i < draws.length; i++) {
            draws[i] = gap.baseGapMs(random);
            assertTrue(draws[i] > 0);
        }

        Arrays.sort(draws);
        int median = draws[draws.length / 2];
        assertTrue(median >= 105 && median <= 115, "median " + median);
    }

    @Test
    void travelGrowsWithDistanceAndIsZeroWhenUnknown() {
        HumanClickGap gap = new HumanClickGap(110, 0.24, 55);
        Point origin = new Point(100, 100);

        assertEquals(0, gap.travelMs(null, origin));
        assertEquals(0, gap.travelMs(origin, null));
        assertEquals(0, gap.travelMs(origin, origin));

        int near = gap.travelMs(origin, new Point(137, 100));
        int far = gap.travelMs(origin, new Point(100, 250));
        assertTrue(near > 0);
        assertTrue(far > near);
        // log2(150 / 30 + 1) bits at 55 ms per bit.
        assertEquals(Math.round(55 * Math.log(6) / Math.log(2)), far);
    }

    @Test
    void aWiderTargetTakesLessTravel() {
        HumanClickGap gap = new HumanClickGap(110, 0.24, 55);
        Point origin = new Point(100, 100);
        Point target = new Point(100, 250);

        assertEquals(gap.travelMs(origin, target), gap.travelMs(origin, target, 0), "0 means the default width");
        assertEquals(gap.travelMs(origin, target), gap.travelMs(origin, target, HumanClickGap.TARGET_WIDTH_PX));
        assertTrue(gap.travelMs(origin, target, 75) < gap.travelMs(origin, target, 15));
        // log2(150 / 75 + 1) bits at 55 ms per bit.
        assertEquals(Math.round(55 * Math.log(3) / Math.log(2)), gap.travelMs(origin, target, 75));
    }
}
