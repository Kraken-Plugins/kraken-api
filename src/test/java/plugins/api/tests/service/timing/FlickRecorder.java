package plugins.api.tests.service.timing;

import lombok.Value;
import plugins.api.tests.service.timing.PhantomBoss.Kind;
import plugins.api.tests.service.timing.PhantomBoss.PlannedFlick;

import java.util.ArrayList;
import java.util.IntSummaryStatistics;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * Collects what happens during a phantom boss run and judges it against the plan.
 *
 * <p>It is fed from the client thread: the prayer's state at the start of every game tick, every click on the
 * prayer, and every signal that could show when the server applied a click (a change of the prayer's varp and the
 * prayer's sound effects). The test reads it once the run is over, so every method is synchronized.
 *
 * <p>The client sets prayer varbits itself by the next client cycle after a click, so the prayer's state at a game
 * tick only shows the click went out in the right tick. Whether the server applied it in time can only come from a
 * signal that arrives with the server's tick update: such a signal arrives in the cycle of a game tick rather than
 * within a cycle or two of the click. The report classifies every signal and, when any arrive with a tick update,
 * measures which offsets the server still applied on the next tick.
 *
 * <p>Pure: it never touches the client, so it is unit tested.
 */
public class FlickRecorder {

    /**
     * Name of the check that the prayer is up on every hit tick.
     */
    public static final String UP_ON_HITS = "Prayer up on every hit tick";

    /**
     * Name of the check that the prayer is down on every tick without a hit.
     */
    public static final String DOWN_BETWEEN_HITS = "Prayer down on every tick without a hit";

    /**
     * Name of the check that every planned change produced exactly one click in its own tick.
     */
    public static final String ONE_CLICK_PER_CHANGE = "One click per planned change, in its own tick";

    /**
     * Name of the check that no click goes out before its offset.
     */
    public static final String WAITS_FOR_OFFSET = "Clicks wait for their offset";

    /**
     * Name of the check that no click goes out after its deadline.
     */
    public static final String LEAVES_BY_DEADLINE = "Clicks leave by their deadline";

    /**
     * Name of the check that an immediate click goes out within the game tick that scheduled it.
     */
    public static final String IMMEDIATE_AT_ONCE = "Immediate click leaves at once";

    /**
     * Name of the check that a cancelled click is never sent.
     */
    public static final String FEINT_CANCELLED = "A cancelled click is never sent";

    /**
     * How much earlier than its offset a click may be measured, for the rounding of two clock reads.
     */
    static final int EARLY_TOLERANCE_MS = 2;

    /**
     * Client cycles after a click within which a signal counts as the client's own change.
     */
    static final int LOCAL_CYCLES = 2;

    private static final int HISTOGRAM_BUCKET_MS = 50;

    private final NavigableMap<Integer, TickSample> ticks = new TreeMap<>();
    private final List<Click> clicks = new ArrayList<>();
    private final List<Signal> signals = new ArrayList<>();
    private final Map<PlannedFlick, Boolean> scheduled = new LinkedHashMap<>();
    private Boolean feintInFlight;
    private Boolean feintWillBeActive;
    private Boolean feintInFlightAfterCancel;
    private Boolean feintWillBeActiveAfterCancel;
    private int startPrayerPoints = -1;
    private int endPrayerPoints = -1;

    /**
     * Records the start of a game tick.
     *
     * @param tick        the game tick, as {@code client.getTickCount()} reads in the game tick handler
     * @param startMillis when the tick started, from the click scheduler's clock
     * @param cycle       {@code client.getGameCycle()} at the game tick
     * @param up          whether the prayer reads as active at the start of the tick
     */
    public synchronized void tick(int tick, long startMillis, int cycle, boolean up) {
        ticks.put(tick, new TickSample(startMillis, cycle, up));
    }

    /**
     * Records a planned click handed to the prayer service.
     *
     * @param flick    the planned click
     * @param accepted what {@code PrayerService.schedule} returned
     */
    public synchronized void scheduled(PlannedFlick flick, boolean accepted) {
        scheduled.put(flick, accepted);
    }

    /**
     * Records a click on the prayer, as the menu reports it.
     *
     * @param millis when the click went out
     * @param cycle  the client cycle it went out in
     * @param on     true when the click turns the prayer on
     */
    public synchronized void clicked(long millis, int cycle, boolean on) {
        clicks.add(new Click(millis, cycle, on));
    }

    /**
     * Records a signal that may show when a click was applied.
     *
     * @param type   what the signal is
     * @param millis when it arrived
     * @param cycle  the client cycle it arrived in
     * @param up     the prayer state it reports: for a sound, true for the prayer's activation sound
     */
    public synchronized void signal(SignalType type, long millis, int cycle, boolean up) {
        signals.add(new Signal(type, millis, cycle, up));
    }

    /**
     * Records the prayer service's view right after the feint's on click was scheduled.
     *
     * @param inFlight     what {@code isInFlight} returned
     * @param willBeActive what {@code willBeActive} returned
     */
    public synchronized void feintScheduled(boolean inFlight, boolean willBeActive) {
        feintInFlight = inFlight;
        feintWillBeActive = willBeActive;
    }

    /**
     * Records the prayer service's view right after the feint's on click was called off.
     *
     * @param inFlight     what {@code isInFlight} returned
     * @param willBeActive what {@code willBeActive} returned
     */
    public synchronized void feintCancelled(boolean inFlight, boolean willBeActive) {
        feintInFlightAfterCancel = inFlight;
        feintWillBeActiveAfterCancel = willBeActive;
    }

    /**
     * Records the prayer points before and after the run.
     *
     * @param start points at the start
     * @param end   points at the end
     */
    public synchronized void prayerPoints(int start, int end) {
        startPrayerPoints = start;
        endPrayerPoints = end;
    }

    /**
     * Judges the run.
     *
     * @param boss     the plan the run followed
     * @param cutoffMs the scheduler's cutoff
     * @param cycleMs  the measured length of a client cycle, the slack allowed past a deadline
     * @return the checks and measurements
     */
    public synchronized FlickReport report(PhantomBoss boss, int cutoffMs, int cycleMs) {
        List<FlickReport.Check> checks = new ArrayList<>();
        List<String> details = new ArrayList<>();

        checkPrayerStates(boss, checks);

        Map<PlannedFlick, Click> matched = new LinkedHashMap<>();
        checkClicks(boss, matched, checks);
        checkOffsets(matched, cutoffMs, cycleMs, checks, details);
        checkFeint(boss, checks);

        describeServerSignals(matched, details);
        describeRecordedSignals(details);
        if (startPrayerPoints >= 0) {
            details.add("Prayer points: " + startPrayerPoints + " before, " + endPrayerPoints + " after, over "
                    + boss.getHitTicks().size() + " hit ticks");
        }
        return new FlickReport(checks, details);
    }

    /**
     * The prayer must be up on every hit tick and down on every other checked tick.
     */
    private void checkPrayerStates(PhantomBoss boss, List<FlickReport.Check> checks) {
        List<Integer> missed = new ArrayList<>();
        List<Integer> stray = new ArrayList<>();
        List<Integer> unseen = new ArrayList<>();
        for (int tick = boss.getHitTicks().first() - 1; tick <= boss.getEndTick(); tick++) {
            if (!boss.isChecked(tick)) {
                continue;
            }

            TickSample sample = ticks.get(tick);
            if (sample == null) {
                unseen.add(tick);
            } else if (boss.isHit(tick) && !sample.isUp()) {
                missed.add(tick);
            } else if (!boss.isHit(tick) && sample.isUp()) {
                stray.add(tick);
            }
        }

        String unseenNote = unseen.isEmpty() ? "" : "; no game tick seen for " + unseen;
        checks.add(new FlickReport.Check(UP_ON_HITS, missed.isEmpty() && unseen.isEmpty(),
                (missed.isEmpty() ? "up on every hit" : "down on hit ticks " + missed) + unseenNote));
        checks.add(new FlickReport.Check(DOWN_BETWEEN_HITS, stray.isEmpty(),
                stray.isEmpty() ? "down on every other tick" : "up on ticks without a hit " + stray));
    }

    /**
     * Every planned click must match exactly one click of the same direction in its tick, and no other click may go
     * out. The feint's tick is left to {@link #checkFeint}.
     */
    private void checkClicks(PhantomBoss boss, Map<PlannedFlick, Click> matched, List<FlickReport.Check> checks) {
        List<String> problems = new ArrayList<>();
        List<Click> unclaimed = new ArrayList<>(clicks);
        for (PlannedFlick flick : boss.getFlicks()) {
            Boolean accepted = scheduled.get(flick);
            if (accepted == null) {
                problems.add("tick " + flick.getTick() + " " + direction(flick.isOn()) + " was never scheduled");
                continue;
            }
            if (!accepted) {
                problems.add("tick " + flick.getTick() + " " + direction(flick.isOn())
                        + " was refused (no prayer points left?)");
                continue;
            }

            List<Click> inTick = new ArrayList<>();
            for (Click click : unclaimed) {
                if (click.isOn() == flick.isOn() && tickOf(click.getMillis()) == flick.getTick()) {
                    inTick.add(click);
                }
            }
            if (inTick.isEmpty()) {
                problems.add("tick " + flick.getTick() + " " + direction(flick.isOn()) + " never went out in its tick");
                continue;
            }
            if (inTick.size() > 1) {
                problems.add("tick " + flick.getTick() + " " + direction(flick.isOn()) + " went out "
                        + inTick.size() + " times");
            }
            matched.put(flick, inTick.get(0));
            unclaimed.removeAll(inTick);
        }

        for (Click click : unclaimed) {
            int tick = tickOf(click.getMillis());
            if (tick != boss.getFeint().getTick()) {
                problems.add("unplanned " + direction(click.isOn()) + " click in tick " + tick);
            }
        }

        checks.add(new FlickReport.Check(ONE_CLICK_PER_CHANGE, problems.isEmpty(),
                problems.isEmpty() ? boss.getFlicks().size() + " planned clicks, each sent once in its tick"
                        : String.join("; ", problems)));
    }

    /**
     * Each click must leave no earlier than its offset and no later than its deadline plus a client cycle; an
     * immediate click within a cycle of the tick starting. Reports how late clicks left and where in the tick.
     */
    private void checkOffsets(Map<PlannedFlick, Click> matched, int cutoffMs, int cycleMs,
                              List<FlickReport.Check> checks, List<String> details) {
        List<String> early = new ArrayList<>();
        List<String> late = new ArrayList<>();
        List<String> slowImmediate = new ArrayList<>();
        IntSummaryStatistics lateness = new IntSummaryStatistics();
        List<Integer> latenessValues = new ArrayList<>();
        Map<Integer, Integer> histogram = new TreeMap<>();
        IntSummaryStatistics lateRequest = new IntSummaryStatistics();
        boolean immediateSeen = false;

        for (Map.Entry<PlannedFlick, Click> entry : matched.entrySet()) {
            PlannedFlick flick = entry.getKey();
            int sent = sentOffset(flick, entry.getValue());
            histogram.merge(sent / HISTOGRAM_BUCKET_MS * HISTOGRAM_BUCKET_MS, 1, Integer::sum);

            if ((flick.getKind() == Kind.SWEEP || flick.getKind() == Kind.PROBE)
                    && sent < flick.getOffsetMs() - EARLY_TOLERANCE_MS) {
                early.add("tick " + flick.getTick() + " asked " + flick.getOffsetMs() + " ms, sent " + sent + " ms");
            }
            if (sent > flick.deadlineMs(cutoffMs) + cycleMs) {
                late.add("tick " + flick.getTick() + " " + flick.getKind() + " deadline "
                        + flick.deadlineMs(cutoffMs) + " ms, sent " + sent + " ms");
            }
            if (flick.getKind() == Kind.IMMEDIATE) {
                immediateSeen = true;
                if (sent > cycleMs) {
                    slowImmediate.add("tick " + flick.getTick() + " sent " + sent + " ms into the tick");
                }
            }
            if (flick.getKind() == Kind.SWEEP) {
                lateness.accept(sent - flick.getOffsetMs());
                latenessValues.add(sent - flick.getOffsetMs());
            }
            if (flick.getKind() == Kind.LATE_REQUEST) {
                lateRequest.accept(sent);
            }
        }

        // With no click matched there is nothing to measure, which must not read as a pass.
        if (matched.isEmpty()) {
            early.add("no click was matched to a planned change, so no offset was measured");
            late.add("no click was matched to a planned change, so no deadline was measured");
        }
        if (!immediateSeen) {
            slowImmediate.add("the immediate click was not seen");
        }

        checks.add(new FlickReport.Check(WAITS_FOR_OFFSET, early.isEmpty(),
                early.isEmpty() ? "no click left before its offset" : String.join("; ", early)));
        checks.add(new FlickReport.Check(LEAVES_BY_DEADLINE, late.isEmpty(),
                late.isEmpty() ? "every click left by its deadline (cutoff " + cutoffMs + " ms)"
                        : String.join("; ", late)));
        checks.add(new FlickReport.Check(IMMEDIATE_AT_ONCE, slowImmediate.isEmpty(),
                slowImmediate.isEmpty() ? "sent within a client cycle of the tick starting"
                        : String.join("; ", slowImmediate)));

        if (lateness.getCount() > 0) {
            latenessValues.sort(Integer::compare);
            int p95 = latenessValues.get(Math.min(latenessValues.size() - 1, (int) (latenessValues.size() * 0.95)));
            details.add(String.format("Lateness of swept clicks: avg %.1f ms, 95th %d ms, max %d ms over %d clicks",
                    lateness.getAverage(), p95, lateness.getMax(), lateness.getCount()));
        }
        if (lateRequest.getCount() > 0) {
            details.add("Late request (asked " + (cutoffMs + PhantomBoss.LATE_REQUEST_PAST_CUTOFF_MS)
                    + " ms) sent at " + lateRequest.getMax() + " ms, cutoff " + cutoffMs + " ms");
        }
        StringBuilder bars = new StringBuilder("Sent offsets:");
        histogram.forEach((bucket, count) -> bars.append(' ').append(bucket).append('-')
                .append(bucket + HISTOGRAM_BUCKET_MS - 1).append("ms:").append(count));
        details.add(histogram.isEmpty() ? "Sent offsets: none, no click was matched to a planned change"
                : bars.toString());
    }

    /**
     * The feint's click must count as in flight once scheduled, stop counting once called off, and never go out.
     */
    private void checkFeint(PhantomBoss boss, List<FlickReport.Check> checks) {
        int feintTick = boss.getFeint().getTick();
        long sent = clicks.stream().filter(click -> tickOf(click.getMillis()) == feintTick).count();
        List<String> problems = new ArrayList<>();
        if (feintInFlight == null || feintInFlightAfterCancel == null) {
            problems.add("the feint never ran");
        } else {
            if (!feintInFlight || !feintWillBeActive) {
                problems.add("the scheduled click did not count as in flight (inFlight " + feintInFlight
                        + ", willBeActive " + feintWillBeActive + ")");
            }
            if (feintInFlightAfterCancel || feintWillBeActiveAfterCancel) {
                problems.add("the cancelled click still counted (inFlight " + feintInFlightAfterCancel
                        + ", willBeActive " + feintWillBeActiveAfterCancel + ")");
            }
        }
        if (sent > 0) {
            problems.add(sent + " click(s) went out in the feint's tick");
        }
        if (clicks.isEmpty()) {
            problems.add("no click was seen in the whole run, so a sent feint would not have been seen either");
        }

        checks.add(new FlickReport.Check(FEINT_CANCELLED, problems.isEmpty(),
                problems.isEmpty() ? "cancelled before it was due, nothing sent" : String.join("; ", problems)));
    }

    /**
     * Classifies every signal after each click as the client's own change (within {@link #LOCAL_CYCLES} cycles of the
     * click) or one that arrived with a tick update (in the cycle of a game tick, or the one before it). A click's
     * server tick is the earliest tick update that carried a matching signal; it was applied in time when that is the
     * tick after the click's own.
     */
    private void describeServerSignals(Map<PlannedFlick, Click> matched, List<String> details) {
        Map<SignalType, int[]> counts = new LinkedHashMap<>();
        for (SignalType type : SignalType.values()) {
            counts.put(type, new int[2]);
        }

        int onTime = 0;
        int slipped = 0;
        int latestOnTime = -1;
        int earliestSlipped = Integer.MAX_VALUE;
        for (Map.Entry<PlannedFlick, Click> entry : matched.entrySet()) {
            PlannedFlick flick = entry.getKey();
            Click click = entry.getValue();
            TickSample next = ticks.get(flick.getTick() + 1);
            TickSample afterNext = ticks.get(flick.getTick() + 2);
            if (next == null || afterNext == null || click.getCycle() >= next.getCycle() - LOCAL_CYCLES) {
                continue;
            }

            Integer serverTick = null;
            for (Signal signal : signals) {
                if (signal.isUp() != click.isOn() || signal.getMillis() < click.getMillis()
                        || signal.getCycle() > afterNext.getCycle()) {
                    continue;
                }

                if (signal.getCycle() - click.getCycle() <= LOCAL_CYCLES) {
                    counts.get(signal.getType())[0]++;
                    continue;
                }

                Integer arrived = tickUpdateCarrying(signal.getCycle(), flick.getTick() + 1, flick.getTick() + 2);
                if (arrived != null) {
                    counts.get(signal.getType())[1]++;
                    serverTick = serverTick == null ? arrived : Math.min(serverTick, arrived);
                }
            }

            if (serverTick == null) {
                continue;
            }

            int sent = sentOffset(flick, click);
            if (serverTick == flick.getTick() + 1) {
                onTime++;
                latestOnTime = Math.max(latestOnTime, sent);
            } else {
                slipped++;
                earliestSlipped = Math.min(earliestSlipped, sent);
            }
        }

        StringBuilder signalLine = new StringBuilder("Signals after clicks:");
        counts.forEach((type, count) -> signalLine.append(' ').append(type.name().toLowerCase())
                .append(" local ").append(count[0]).append(", with tick update ").append(count[1]).append(';'));
        details.add(signalLine.toString());

        if (onTime + slipped == 0) {
            details.add("No signal arrived with a tick update, so the client changes this prayer on its own and the "
                    + "server's tick cannot be measured from it");
            return;
        }

        details.add("Server applied " + onTime + " clicks on the next tick and " + slipped + " a tick late"
                + (latestOnTime >= 0 ? "; latest offset in time " + latestOnTime + " ms" : "")
                + (earliestSlipped != Integer.MAX_VALUE ? "; earliest offset that slipped " + earliestSlipped + " ms"
                : ""));
    }

    /**
     * Counts every recorded signal by type and state, whether or not it followed a matched click, so a run whose
     * clicks were not seen still shows whether varp changes and sounds arrived at all.
     */
    private void describeRecordedSignals(List<String> details) {
        StringBuilder line = new StringBuilder("Recorded " + clicks.size() + " clicks; signals:");
        for (SignalType type : SignalType.values()) {
            long up = signals.stream().filter(signal -> signal.getType() == type && signal.isUp()).count();
            long down = signals.stream().filter(signal -> signal.getType() == type && !signal.isUp()).count();
            line.append(' ').append(type.name().toLowerCase()).append(' ').append(up).append(" up, ")
                    .append(down).append(" down;");
        }
        details.add(line.toString());
    }

    /**
     * @return the tick among the given ones whose game tick arrived in the signal's cycle or the cycle after it, or
     * null when the signal arrived with neither
     */
    private Integer tickUpdateCarrying(int cycle, int... candidates) {
        for (int tick : candidates) {
            TickSample sample = ticks.get(tick);
            if (sample != null && cycle <= sample.getCycle() && cycle >= sample.getCycle() - 1) {
                return tick;
            }
        }
        return null;
    }

    /**
     * @return the game tick a moment falls in: the last tick that started at or before it, or the first tick when it
     * is earlier than every tick seen
     */
    private int tickOf(long millis) {
        int found = ticks.isEmpty() ? Integer.MIN_VALUE : ticks.firstKey();
        for (Map.Entry<Integer, TickSample> entry : ticks.entrySet()) {
            if (entry.getValue().getStartMillis() > millis) {
                break;
            }
            found = entry.getKey();
        }
        return found;
    }

    private int sentOffset(PlannedFlick flick, Click click) {
        TickSample sample = ticks.get(flick.getTick());
        return sample == null ? Integer.MAX_VALUE : (int) (click.getMillis() - sample.getStartMillis());
    }

    private static String direction(boolean on) {
        return on ? "on" : "off";
    }

    /**
     * What a timing signal is.
     */
    public enum SignalType {
        /**
         * A change of the varp that holds the prayer.
         */
        VARP,

        /**
         * The prayer's activation or deactivation sound.
         */
        SOUND
    }

    @Value
    private static class TickSample {
        long startMillis;
        int cycle;
        boolean up;
    }

    @Value
    private static class Click {
        long millis;
        int cycle;
        boolean on;
    }

    @Value
    private static class Signal {
        SignalType type;
        long millis;
        int cycle;
        boolean up;
    }
}
