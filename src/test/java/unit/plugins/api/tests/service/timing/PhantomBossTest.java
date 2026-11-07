package unit.plugins.api.tests.service.timing;

import com.kraken.api.core.interaction.timing.ClickTiming;
import org.junit.jupiter.api.Test;
import plugins.api.tests.service.timing.PhantomBoss;
import plugins.api.tests.service.timing.PhantomBoss.Kind;
import plugins.api.tests.service.timing.PhantomBoss.PlannedFlick;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PhantomBossTest {
    private static final int CUTOFF_MS = 300;
    private static final int[] NO_PROBES = {};

    private static List<String> changes(PhantomBoss boss) {
        List<String> changes = new ArrayList<>();
        for (PlannedFlick flick : boss.getFlicks()) {
            changes.add(flick.getTick() + (flick.isOn() ? " on" : " off"));
        }
        return changes;
    }

    @Test
    void hitsFollowTheIntervals() {
        PhantomBoss boss = PhantomBoss.create(100, CUTOFF_MS, new int[]{4, 1, 2}, NO_PROBES);

        assertEquals(List.of(100, 104, 105, 107), new ArrayList<>(boss.getHitTicks()));
        assertEquals(107, boss.getLastMainHitTick());
    }

    @Test
    void everyChangeOfStateIsOneClickInTheTickBefore() {
        PhantomBoss boss = PhantomBoss.create(100, CUTOFF_MS, new int[]{4, 1, 2}, NO_PROBES);

        assertEquals(List.of("99 on", "100 off", "103 on", "105 off", "106 on", "107 off"), changes(boss));
    }

    @Test
    void backToBackHitsKeepThePrayerUpWithoutClicks() {
        PhantomBoss boss = PhantomBoss.create(100, CUTOFF_MS, new int[]{1, 1, 1}, NO_PROBES);

        assertEquals(List.of("99 on", "103 off"), changes(boss));
    }

    @Test
    void theFirstOnClickIsImmediateAndOneAsksPastTheCutoff() {
        PhantomBoss boss = PhantomBoss.create(100, CUTOFF_MS);

        List<PlannedFlick> ons = new ArrayList<>();
        boss.getFlicks().stream().filter(PlannedFlick::isOn).forEach(ons::add);
        assertEquals(Kind.IMMEDIATE, ons.get(0).getKind());
        assertTrue(ons.get(0).timing().isImmediate());

        List<PlannedFlick> late = new ArrayList<>();
        boss.getFlicks().stream().filter(flick -> flick.getKind() == Kind.LATE_REQUEST).forEach(late::add);
        assertEquals(1, late.size());
        assertEquals(CUTOFF_MS + PhantomBoss.LATE_REQUEST_PAST_CUTOFF_MS, late.get(0).getOffsetMs());
        assertEquals(ClickTiming.SCHEDULER_CUTOFF, late.get(0).timing().getDeadlineMs());
    }

    @Test
    void sweptClicksAskForOffsetsAcrossTheWindowBeforeTheCutoff() {
        PhantomBoss boss = PhantomBoss.create(100, CUTOFF_MS);

        int lowest = Integer.MAX_VALUE;
        int highest = Integer.MIN_VALUE;
        for (PlannedFlick flick : boss.getFlicks()) {
            if (flick.getKind() != Kind.SWEEP) {
                continue;
            }
            assertEquals(ClickTiming.SCHEDULER_CUTOFF, flick.timing().getDeadlineMs());
            lowest = Math.min(lowest, flick.getOffsetMs());
            highest = Math.max(highest, flick.getOffsetMs());
        }
        assertEquals(PhantomBoss.FIRST_SWEEP_OFFSET_MS, lowest);
        assertTrue(highest <= CUTOFF_MS - 10 && highest >= CUTOFF_MS - 30, "highest " + highest);
    }

    @Test
    void calibrationClicksAskForTheirOffsetsWithALateDeadline() {
        PhantomBoss boss = PhantomBoss.create(100, CUTOFF_MS);

        List<Integer> offsets = new ArrayList<>();
        for (PlannedFlick flick : boss.getFlicks()) {
            if (flick.getKind() == Kind.PROBE) {
                assertTrue(flick.isOn());
                assertTrue(boss.getProbeHitTicks().contains(flick.getTick() + 1));
                assertEquals(PhantomBoss.PROBE_DEADLINE_MS, flick.timing().getDeadlineMs());
                offsets.add(flick.getOffsetMs());
            }
        }
        List<Integer> expected = new ArrayList<>();
        for (int offset : PhantomBoss.PROBE_OFFSETS_MS) {
            expected.add(offset);
        }
        assertEquals(expected, offsets);
    }

    @Test
    void theFeintFallsWhereThePrayerIsDownAndNoHitFollows() {
        PhantomBoss boss = PhantomBoss.create(100, CUTOFF_MS);
        int feint = boss.getFeint().getTick();

        assertFalse(boss.isHit(feint));
        assertFalse(boss.isHit(feint + 1));
        assertTrue(boss.flicksAt(feint).isEmpty());
        assertTrue(boss.getFeint().getCancelAfterMs() < boss.getFeint().getOnOffsetMs());
        assertTrue(boss.isChecked(feint + 1));
    }

    @Test
    void checksCoverTheMainPatternButNotTheCalibrationHits() {
        PhantomBoss boss = PhantomBoss.create(100, CUTOFF_MS);

        assertTrue(boss.isChecked(99));
        assertTrue(boss.isChecked(boss.getLastMainHitTick() + 1));
        for (int probe : boss.getProbeHitTicks()) {
            assertFalse(boss.isChecked(probe));
        }
        assertTrue(boss.getEndTick() > boss.getHitTicks().last());
    }

    @Test
    void intervalsBelowOneAreRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> PhantomBoss.create(100, CUTOFF_MS, new int[]{2, 0}, NO_PROBES));
    }
}
