package unit.plugins.api.tests.service.timing;

import org.junit.jupiter.api.Test;
import plugins.api.tests.service.timing.FlickRecorder;
import plugins.api.tests.service.timing.FlickRecorder.SignalType;
import plugins.api.tests.service.timing.FlickReport;
import plugins.api.tests.service.timing.PhantomBoss;
import plugins.api.tests.service.timing.PhantomBoss.Kind;
import plugins.api.tests.service.timing.PhantomBoss.PlannedFlick;
import plugins.api.tests.service.timing.PrayerTimingReport;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;
import java.util.function.ToIntFunction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlickRecorderTest {
    private static final int CUTOFF_MS = 300;
    private static final int CYCLE_MS = 20;
    private static final int FIRST_HIT = 100;
    private static final long TICK_ZERO_MILLIS = 1_000_000;

    /**
     * Plays a phantom boss run into a recorder the way the client would: a tick every 600 ms and 30 cycles, each
     * click at the offset the given function picks, and the prayer's state at a tick set by the clicks before it.
     * The varp changes locally a cycle after each click, and optionally again with the next tick update when the
     * click left before {@code serverCutoffMs}, or with the one after it otherwise.
     */
    private static final class Run {
        final PhantomBoss boss = PhantomBoss.create(FIRST_HIT, CUTOFF_MS);
        final FlickRecorder recorder = new FlickRecorder();
        ToIntFunction<PlannedFlick> sentOffset = Run::onTime;
        PlannedFlick dropped;
        PlannedFlick doubled;
        PlannedFlick refused;
        boolean feintSends;
        int serverCutoffMs = -1;
        boolean clicksSeen = true;
        int onClicks;
        int offClicks;

        static int onTime(PlannedFlick flick) {
            switch (flick.getKind()) {
                case IMMEDIATE:
                    return 1;
                case LATE_REQUEST:
                    return CUTOFF_MS - 10;
                default:
                    return flick.getOffsetMs() + 5;
            }
        }

        static long start(int tick) {
            return TICK_ZERO_MILLIS + 600L * (tick - FIRST_HIT);
        }

        static int cycle(int tick) {
            return 30 * (tick - FIRST_HIT) + 3_000;
        }

        FlickReport play() {
            boolean up = false;
            for (int tick = FIRST_HIT - 3; tick <= boss.getEndTick() + 2; tick++) {
                recorder.tick(tick, start(tick), cycle(tick), up);
                for (PlannedFlick flick : boss.flicksAt(tick)) {
                    recorder.scheduled(flick, flick != refused);
                    if (flick == dropped || flick == refused) {
                        continue;
                    }

                    int offset = sentOffset.applyAsInt(flick);
                    click(tick, offset, flick.isOn());
                    if (flick == doubled) {
                        click(tick, offset + 120, flick.isOn());
                    }
                    if (offset < 600) {
                        up = flick.isOn();
                    }
                }

                if (tick == boss.getFeint().getTick()) {
                    recorder.feintScheduled(true, true);
                    recorder.feintCancelled(start(tick) + 75, false, false);
                    if (feintSends) {
                        recorder.clicked(start(tick) + 260, cycle(tick) + 13, true);
                    }
                }
            }
            return recorder.report(boss, CUTOFF_MS, CYCLE_MS);
        }

        private void click(int tick, int offset, boolean on) {
            int clickCycle = cycle(tick) + offset / CYCLE_MS;
            if (clicksSeen) {
                recorder.clicked(start(tick) + offset, clickCycle, on);
            }
            if (on) {
                onClicks++;
            } else {
                offClicks++;
            }
            recorder.signal(SignalType.VARP, start(tick) + offset + CYCLE_MS, clickCycle + 1, on);
            if (serverCutoffMs >= 0) {
                int applied = offset <= serverCutoffMs ? tick + 1 : tick + 2;
                recorder.signal(SignalType.VARP, start(applied) - 5, cycle(applied), on);
            }
        }

        PlannedFlick first(Kind kind, boolean on) {
            return boss.getFlicks().stream()
                    .filter(flick -> flick.getKind() == kind && flick.isOn() == on && boss.isChecked(flick.getTick()))
                    .findFirst().orElseThrow();
        }
    }

    private static boolean passed(FlickReport report, String check) {
        return report.check(check).isPassed();
    }

    @Test
    void aCleanRunPassesEveryCheck() {
        FlickReport report = new Run().play();

        assertTrue(report.passed(), report.getChecks().toString());
        assertTrue(report.getDetails().stream().anyMatch(line -> line.startsWith("No signal arrived")),
                report.getDetails().toString());
    }

    @Test
    void aMissedOnClickLeavesTheHitUnprayed() {
        Run run = new Run();
        run.dropped = run.first(Kind.SWEEP, true);

        FlickReport report = run.play();

        assertFalse(passed(report, FlickRecorder.UP_ON_HITS));
        assertTrue(report.check(FlickRecorder.UP_ON_HITS).getDetail().contains(
                String.valueOf(run.dropped.getTick() + 1)));
        assertFalse(passed(report, FlickRecorder.ONE_CLICK_PER_CHANGE));
    }

    @Test
    void aMissedOffClickLeavesThePrayerUp() {
        Run run = new Run();
        run.dropped = run.first(Kind.SWEEP, false);

        FlickReport report = run.play();

        assertFalse(passed(report, FlickRecorder.DOWN_BETWEEN_HITS));
    }

    @Test
    void aDoubleClickFails() {
        Run run = new Run();
        run.doubled = run.first(Kind.SWEEP, true);

        FlickReport report = run.play();

        assertFalse(passed(report, FlickRecorder.ONE_CLICK_PER_CHANGE));
        assertTrue(report.check(FlickRecorder.ONE_CLICK_PER_CHANGE).getDetail().contains("2 times"));
    }

    @Test
    void aRefusedClickFails() {
        Run run = new Run();
        run.refused = run.first(Kind.SWEEP, true);

        FlickReport report = run.play();

        assertTrue(report.check(FlickRecorder.ONE_CLICK_PER_CHANGE).getDetail().contains("refused"));
    }

    @Test
    void aClickThatSlipsIntoTheNextTickFails() {
        Run run = new Run();
        PlannedFlick slipped = run.first(Kind.SWEEP, true);
        run.sentOffset = flick -> flick == slipped ? 610 : Run.onTime(flick);

        FlickReport report = run.play();

        assertFalse(passed(report, FlickRecorder.ONE_CLICK_PER_CHANGE));
        assertTrue(report.check(FlickRecorder.ONE_CLICK_PER_CHANGE).getDetail().contains("unplanned"));
    }

    @Test
    void aClickBeforeItsOffsetFails() {
        Run run = new Run();
        run.sentOffset = flick -> flick.getKind() == Kind.SWEEP ? flick.getOffsetMs() - 20 : Run.onTime(flick);

        FlickReport report = run.play();

        assertFalse(passed(report, FlickRecorder.WAITS_FOR_OFFSET));
    }

    @Test
    void aClickPastItsDeadlineFails() {
        Run run = new Run();
        PlannedFlick late = run.first(Kind.LATE_REQUEST, true);
        run.sentOffset = flick -> flick == late ? CUTOFF_MS + CYCLE_MS + 30 : Run.onTime(flick);

        FlickReport report = run.play();

        assertFalse(passed(report, FlickRecorder.LEAVES_BY_DEADLINE));
    }

    @Test
    void aCalibrationClickMayLeavePastTheCutoff() {
        Run run = new Run();

        FlickReport report = run.play();

        assertTrue(passed(report, FlickRecorder.LEAVES_BY_DEADLINE));
    }

    @Test
    void aSlowImmediateClickFails() {
        Run run = new Run();
        run.sentOffset = flick -> flick.getKind() == Kind.IMMEDIATE ? 80 : Run.onTime(flick);

        FlickReport report = run.play();

        assertFalse(passed(report, FlickRecorder.IMMEDIATE_AT_ONCE));
    }

    @Test
    void aFeintThatSendsItsClickFails() {
        Run run = new Run();
        run.feintSends = true;

        FlickReport report = run.play();

        assertFalse(passed(report, FlickRecorder.FEINT_CANCELLED));
    }

    @Test
    void aRunWhoseClicksAreNotSeenFailsEveryClickCheck() {
        Run run = new Run();
        run.clicksSeen = false;

        FlickReport report = run.play();

        assertTrue(passed(report, FlickRecorder.UP_ON_HITS));
        assertFalse(passed(report, FlickRecorder.ONE_CLICK_PER_CHANGE));
        assertFalse(passed(report, FlickRecorder.WAITS_FOR_OFFSET));
        assertFalse(passed(report, FlickRecorder.LEAVES_BY_DEADLINE));
        assertFalse(passed(report, FlickRecorder.IMMEDIATE_AT_ONCE));
        assertFalse(passed(report, FlickRecorder.FEINT_CANCELLED));
        assertTrue(report.getDetails().contains("Sent offsets: none, no click was matched to a planned change"),
                report.getDetails().toString());
        assertTrue(report.getDetails().stream().anyMatch(line -> line.startsWith("Recorded 0 clicks; signals: varp")),
                report.getDetails().toString());
    }

    @Test
    void recordedSignalsAreCountedEvenWithoutMatchedClicks() {
        Run run = new Run();
        run.clicksSeen = false;

        FlickReport report = run.play();

        String line = report.getDetails().stream().filter(detail -> detail.startsWith("Recorded "))
                .findFirst().orElseThrow();
        assertTrue(line.contains("varp " + run.onClicks + " up, " + run.offClicks + " down;"), line);
    }

    @Test
    void serverEchoesShowWhichOffsetsTheServerAppliedInTime() {
        Run run = new Run();
        run.serverCutoffMs = 400;

        FlickReport report = run.play();

        assertTrue(report.passed(), report.getChecks().toString());
        String measured = report.getDetails().stream().filter(line -> line.startsWith("Server applied"))
                .findFirst().orElseThrow();
        assertTrue(measured.contains("a tick late"), measured);
        assertTrue(measured.contains("latest offset in time 365 ms"), measured);
        assertTrue(measured.contains("earliest offset that slipped 405 ms"), measured);
    }

    @Test
    void theTimelineHoldsEveryTickClickAndEvent() {
        Run run = new Run();

        PrayerTimingReport timeline = run.play().getTimeline();

        assertEquals(run.boss.getFlicks().size(), timeline.getClicks().size());
        assertTrue(timeline.getClicks().stream().noneMatch(click -> click.getKind().equals("UNPLANNED")));
        PrayerTimingReport.Click sweep = timeline.getClicks().stream()
                .filter(click -> click.getKind().equals(Kind.SWEEP.name())).findFirst().orElseThrow();
        assertEquals(sweep.getRequestedMs() + 5, sweep.getSentMs());
        assertTrue(timeline.getTicks().stream()
                .noneMatch(tick -> tick.getVerdict().equals("missed") || tick.getVerdict().equals("stray")));
        assertEquals(run.boss.getHitTicks().size(),
                timeline.getTicks().stream().filter(PrayerTimingReport.Tick::isHit).count());
        assertTrue(timeline.getMarkers().stream().anyMatch(marker -> marker.getType().equals("feint")));
        PrayerTimingReport.Marker cancel = timeline.getMarkers().stream()
                .filter(marker -> marker.getType().equals("cancel")).findFirst().orElseThrow();
        assertEquals(run.boss.getFeint().getTick(), cancel.getTick());
        assertEquals(75, cancel.getMs());
        assertEquals(timeline.getChecks().size(), run.play().getChecks().size());
    }

    @Test
    void theTimelineMarksAMissedHitOnItsTick() {
        Run run = new Run();
        run.dropped = run.first(Kind.SWEEP, true);

        PrayerTimingReport timeline = run.play().getTimeline();

        PrayerTimingReport.Tick missed = timeline.getTicks().stream()
                .filter(tick -> tick.getTick() == run.dropped.getTick() + 1).findFirst().orElseThrow();
        assertEquals("missed", missed.getVerdict());
    }

    /**
     * Writes a chart of a simulated run to {@code build/reports/prayer-timing}, to look at without a client. The
     * clicks leave a little after their offsets, and the server echoes each click with the next tick update when it
     * left by 400 ms.
     */
    @Test
    void writesASampleChart() throws IOException {
        Run run = new Run();
        run.serverCutoffMs = 400;
        Random jitter = new Random(7);
        run.sentOffset = flick -> flick.getKind() == Kind.SWEEP || flick.getKind() == Kind.PROBE
                ? flick.getOffsetMs() + 2 + jitter.nextInt(14) : Run.onTime(flick);

        FlickReport report = run.play();
        Path html = report.getTimeline().toBuilder()
                .title("Timed Prayer Flicks: sample")
                .subtitle("A simulated run of the phantom boss, not game data. Clicks leave 2 to 15 ms after their "
                        + "offsets, and the server echoes those sent by 400 ms on the next tick.")
                .prayer("Thick Skin")
                .build()
                .write(Path.of("build", "reports", "prayer-timing"), "timed-prayer-flicks-sample");

        assertTrue(report.passed(), report.getChecks().toString());
        assertTrue(Files.size(html) > 0);
    }
}
