package plugins.api.tests.service.timing;

import com.kraken.api.core.interaction.timing.ClickTiming;
import lombok.Getter;
import lombok.Value;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NavigableSet;
import java.util.TreeSet;

/**
 * A scripted attacker that stands in for a boss when testing timed prayer clicks on an account that has none to
 * fight. It "hits" on a fixed pattern of game ticks, and the test lazy flicks one prayer against it: the prayer is up
 * on every hit tick and down on every other tick, the way auto prayer plugins flick against real attacks.
 *
 * <p>The pattern mixes the cadences real fights produce: a steady four tick attack, faster three and two tick ones,
 * hits on back to back ticks (the prayer stays up between them) and longer pauses. Each change of prayer state is one
 * planned click, sent during the tick before the change. On clicks walk through the whole window before the cutoff
 * and off clicks do the same, so a run covers every part of the tick. A few clicks test the scheduler's edges: one is
 * sent at once, one asks for an offset past the cutoff, which the deadline must pull back in, and one attack is a
 * feint whose click is cancelled before it leaves. Calibration hits at the end send their on click past the cutoff on
 * purpose, to look for the point where the server applies a click a tick late.
 *
 * <p>Pure: it never touches the client, so the plan is unit tested.
 */
@Getter
public class PhantomBoss {

    /**
     * Ticks between consecutive hits of the main pattern.
     */
    public static final int[] DEFAULT_INTERVALS = {4, 4, 4, 3, 3, 2, 2, 1, 1, 1, 5, 4, 2, 1, 3, 6, 2, 2, 1, 4, 3, 5};

    /**
     * On click offsets of the calibration hits, in milliseconds after the tick starts.
     */
    public static final int[] PROBE_OFFSETS_MS = {320, 360, 400, 440, 480, 520};

    /**
     * The deadline of a calibration click, late enough that the requested offset is honoured.
     */
    public static final int PROBE_DEADLINE_MS = 560;

    /**
     * The earliest offset a swept click asks for.
     */
    public static final int FIRST_SWEEP_OFFSET_MS = 30;

    /**
     * Milliseconds between consecutive swept offsets.
     */
    public static final int SWEEP_STEP_MS = 20;

    /**
     * How far past the cutoff the late request asks to be sent.
     */
    public static final int LATE_REQUEST_PAST_CUTOFF_MS = 150;

    private static final int LATE_REQUEST_INDEX = 4;
    private static final int TICKS_BEFORE_FEINT = 3;
    private static final int TICKS_BEFORE_PROBES = 4;
    private static final int TICKS_BETWEEN_PROBES = 3;
    private static final int TICKS_AFTER_LAST_HIT = 3;

    /**
     * Every hit tick, main pattern and calibration.
     */
    private final NavigableSet<Integer> hitTicks;

    /**
     * The calibration hits, whose on clicks go out past the cutoff.
     */
    private final NavigableSet<Integer> probeHitTicks;

    /**
     * The last hit of the main pattern. Checks that must pass cover the ticks up to the tick after it, plus the feint.
     */
    private final int lastMainHitTick;

    /**
     * Every planned click, in tick order.
     */
    private final List<PlannedFlick> flicks;

    /**
     * The attack that is called off before its click leaves.
     */
    private final Feint feint;

    /**
     * The tick after which the run is over and the results can be read.
     */
    private final int endTick;

    private PhantomBoss(NavigableSet<Integer> hitTicks, NavigableSet<Integer> probeHitTicks, int lastMainHitTick,
                        List<PlannedFlick> flicks, Feint feint, int endTick) {
        this.hitTicks = Collections.unmodifiableNavigableSet(hitTicks);
        this.probeHitTicks = Collections.unmodifiableNavigableSet(probeHitTicks);
        this.lastMainHitTick = lastMainHitTick;
        this.flicks = Collections.unmodifiableList(flicks);
        this.feint = feint;
        this.endTick = endTick;
    }

    /**
     * Builds the default fight.
     *
     * @param firstHitTick the game tick of the first hit; its on click goes out during the tick before
     * @param cutoffMs     the scheduler's cutoff, which bounds the swept offsets
     * @return the phantom boss
     */
    public static PhantomBoss create(int firstHitTick, int cutoffMs) {
        return create(firstHitTick, cutoffMs, DEFAULT_INTERVALS, PROBE_OFFSETS_MS);
    }

    /**
     * Builds a fight from a pattern of hits.
     *
     * @param firstHitTick the game tick of the first hit
     * @param cutoffMs     the scheduler's cutoff, which bounds the swept offsets
     * @param intervals    ticks between consecutive hits of the main pattern, each at least 1
     * @param probeOffsets on click offsets of the calibration hits that follow the main pattern
     * @return the phantom boss
     */
    public static PhantomBoss create(int firstHitTick, int cutoffMs, int[] intervals, int[] probeOffsets) {
        NavigableSet<Integer> hits = new TreeSet<>();
        int tick = firstHitTick;
        hits.add(tick);
        for (int interval : intervals) {
            if (interval < 1) {
                throw new IllegalArgumentException("intervals must be at least 1: " + interval);
            }
            tick += interval;
            hits.add(tick);
        }
        int lastMainHit = tick;

        // The feint needs the prayer down on its tick and no hit on the tick after it.
        Feint feint = new Feint(lastMainHit + TICKS_BEFORE_FEINT, cutoffMs - 40, Math.max(1, cutoffMs / 4));

        NavigableSet<Integer> probes = new TreeSet<>();
        int probeTick = feint.getTick() + TICKS_BEFORE_PROBES;
        for (int ignored : probeOffsets) {
            probes.add(probeTick);
            probeTick += TICKS_BETWEEN_PROBES;
        }
        hits.addAll(probes);

        List<PlannedFlick> flicks = planFlicks(hits, probes, probeOffsets, cutoffMs);
        return new PhantomBoss(hits, probes, lastMainHit, flicks, feint, hits.last() + TICKS_AFTER_LAST_HIT);
    }

    /**
     * Turns the hits into one click per change of prayer state, during the tick before the change.
     */
    private static List<PlannedFlick> planFlicks(NavigableSet<Integer> hits, NavigableSet<Integer> probes,
                                                 int[] probeOffsets, int cutoffMs) {
        List<Integer> sweep = new ArrayList<>();
        for (int offset = FIRST_SWEEP_OFFSET_MS; offset <= cutoffMs - 10; offset += SWEEP_STEP_MS) {
            sweep.add(offset);
        }
        if (sweep.isEmpty()) {
            sweep.add(Math.max(0, cutoffMs / 2));
        }

        List<Integer> probeTicks = new ArrayList<>(probes);
        List<PlannedFlick> flicks = new ArrayList<>();
        int onCount = 0;
        int offCount = 0;
        for (int tick = hits.first() - 1; tick <= hits.last(); tick++) {
            boolean up = hits.contains(tick);
            boolean upNext = hits.contains(tick + 1);
            if (up == upNext) {
                continue;
            }

            if (!upNext) {
                // Off clicks start half a sweep in, so on and off clicks do not repeat the same offsets.
                int offset = sweep.get((offCount++ + sweep.size() / 2) % sweep.size());
                flicks.add(new PlannedFlick(tick, false, Kind.SWEEP, offset, ClickTiming.SCHEDULER_CUTOFF));
                continue;
            }

            int probeIndex = probeTicks.indexOf(tick + 1);
            if (probeIndex >= 0) {
                flicks.add(new PlannedFlick(tick, true, Kind.PROBE, probeOffsets[probeIndex], PROBE_DEADLINE_MS));
            } else if (onCount == 0) {
                flicks.add(new PlannedFlick(tick, true, Kind.IMMEDIATE, 0, ClickTiming.SCHEDULER_CUTOFF));
            } else if (onCount == LATE_REQUEST_INDEX) {
                flicks.add(new PlannedFlick(tick, true, Kind.LATE_REQUEST, cutoffMs + LATE_REQUEST_PAST_CUTOFF_MS,
                        ClickTiming.SCHEDULER_CUTOFF));
            } else {
                flicks.add(new PlannedFlick(tick, true, Kind.SWEEP, sweep.get(onCount % sweep.size()),
                        ClickTiming.SCHEDULER_CUTOFF));
            }
            onCount++;
        }
        return flicks;
    }

    /**
     * @param tick a game tick
     * @return true when the phantom boss hits on that tick, so the prayer must be up
     */
    public boolean isHit(int tick) {
        return hitTicks.contains(tick);
    }

    /**
     * @param tick a game tick
     * @return the clicks to schedule during that tick
     */
    public List<PlannedFlick> flicksAt(int tick) {
        List<PlannedFlick> due = new ArrayList<>();
        for (PlannedFlick flick : flicks) {
            if (flick.getTick() == tick) {
                due.add(flick);
            }
        }
        return due;
    }

    /**
     * @param tick a game tick
     * @return true when the tick's prayer state is covered by the checks that must pass: the main pattern from the
     * tick before its first hit to the tick after its last, and the tick after the feint
     */
    public boolean isChecked(int tick) {
        return (tick >= hitTicks.first() - 1 && tick <= lastMainHitTick + 1) || tick == feint.getTick() + 1;
    }

    /**
     * What a planned click tests.
     */
    public enum Kind {
        /**
         * A click at a swept offset before the cutoff.
         */
        SWEEP,

        /**
         * A click sent within the call that schedules it.
         */
        IMMEDIATE,

        /**
         * A click that asks for an offset past the cutoff, which its deadline must pull back in.
         */
        LATE_REQUEST,

        /**
         * A calibration click past the cutoff, honoured by a late deadline.
         */
        PROBE
    }

    /**
     * One planned prayer click.
     */
    @Value
    public static class PlannedFlick {
        /**
         * The game tick the click goes out in; the prayer changes from the tick after.
         */
        int tick;

        /**
         * True to turn the prayer on.
         */
        boolean on;

        Kind kind;

        /**
         * Milliseconds after the tick starts at which the click asks to leave; 0 for {@link Kind#IMMEDIATE}.
         */
        int offsetMs;

        /**
         * The click's deadline in milliseconds after the tick starts, or {@link ClickTiming#SCHEDULER_CUTOFF}.
         */
        int deadlineMs;

        /**
         * @return the timing to schedule the click with
         */
        public ClickTiming timing() {
            if (kind == Kind.IMMEDIATE) {
                return ClickTiming.immediate();
            }
            return deadlineMs == ClickTiming.SCHEDULER_CUTOFF
                    ? ClickTiming.at(offsetMs) : ClickTiming.at(offsetMs, deadlineMs);
        }

        /**
         * @param cutoffMs the scheduler's cutoff
         * @return the deadline in milliseconds after the tick starts
         */
        public int deadlineMs(int cutoffMs) {
            return deadlineMs == ClickTiming.SCHEDULER_CUTOFF ? cutoffMs : deadlineMs;
        }
    }

    /**
     * An attack the phantom boss starts and then calls off: the on click is scheduled at the start of the tick and
     * cancelled partway through, before it is due.
     */
    @Value
    public static class Feint {
        /**
         * The tick the on click is scheduled and cancelled in.
         */
        int tick;

        /**
         * When the on click asks to leave, in milliseconds after the tick starts.
         */
        int onOffsetMs;

        /**
         * When the click is called off, in milliseconds after the tick starts; earlier than {@link #onOffsetMs}.
         */
        int cancelAfterMs;
    }
}
