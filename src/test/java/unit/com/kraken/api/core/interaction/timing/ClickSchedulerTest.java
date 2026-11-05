package com.kraken.api.core.interaction.timing;

import net.runelite.api.GameState;
import net.runelite.api.Point;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Drives the scheduler with a fake clock: a game tick starts at 1000 ms and client cycles are processed at chosen
 * times, so each test can check exactly which cycle a click leaves in.
 */
class ClickSchedulerTest {

    private static final int TICK_START = 1_000;

    /** A gap of exactly {@code base} ms between clicks, plus {@code travel} ms when the two points differ. */
    private static ClickGapModel fixedGap(int base, int travel) {
        return new ClickGapModel() {
            @Override
            public int baseGapMs(Random random) {
                return base;
            }

            @Override
            public int travelMs(Point from, Point to) {
                return from == null || to == null || from.equals(to) ? 0 : travel;
            }
        };
    }

    private final AtomicLong clock = new AtomicLong();
    private final AtomicLong externalClick = new AtomicLong();
    private final List<String> sent = new ArrayList<>();
    private Point lastPoint;
    private ClickScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = schedulerWithGap(fixedGap(100, 0));
        startTick(TICK_START);
    }

    @AfterEach
    void tearDown() {
        ClickScheduler.setImmediateMode(false);
    }

    private ClickScheduler schedulerWithGap(ClickGapModel gap) {
        return new ClickScheduler(clock::get, new Random(1), gap, externalClick::get, () -> lastPoint, null);
    }

    private void startTick(long at) {
        clock.set(at);
        scheduler.onGameTick(new GameTick());
    }

    private void cycleAt(long at) {
        clock.set(at);
        scheduler.onClientTick(new ClientTick());
    }

    private BooleanSupplier click(String name) {
        return click(name, null);
    }

    private BooleanSupplier click(String name, Point at) {
        return () -> {
            sent.add(name + "@" + clock.get());
            lastPoint = at;
            return true;
        };
    }

    @Test
    void immediateTimingRunsTheActionWithinTheCall() {
        clock.set(TICK_START + 5);
        ScheduledClick click = scheduler.schedule(this, ClickTiming.immediate(), null, click("a"));

        assertEquals(List.of("a@1005"), sent);
        assertTrue(click.isSent());
        assertEquals(0, scheduler.getPendingCount());
    }

    @Test
    void immediateModeIgnoresTheTiming() {
        ClickScheduler.setImmediateMode(true);

        scheduler.schedule(this, ClickTiming.at(250), null, click("a"));

        assertEquals(List.of("a@1000"), sent);
    }

    @Test
    void waitsForTheOffset() {
        ScheduledClick click = scheduler.schedule(this, ClickTiming.at(150), null, click("a"));

        cycleAt(1_100);
        cycleAt(1_140);
        assertTrue(sent.isEmpty());
        assertTrue(click.isPending());

        cycleAt(1_160);
        assertEquals(List.of("a@1160"), sent);
        assertEquals(1_160, click.getSentAtMillis());
    }

    @Test
    void clicksDueTogetherLeaveAGapApart() {
        scheduler.schedule(this, ClickTiming.at(40), null, click("a"));
        scheduler.schedule(this, ClickTiming.at(40), null, click("b"));

        cycleAt(1_040);
        cycleAt(1_060);
        cycleAt(1_120);
        assertEquals(List.of("a@1040"), sent);

        cycleAt(1_140);
        assertEquals(List.of("a@1040", "b@1140"), sent);
    }

    @Test
    void theGapCountsClicksFromOtherPlugins() {
        externalClick.set(1_030);
        scheduler.schedule(this, ClickTiming.at(40), null, click("a"));

        cycleAt(1_040);
        cycleAt(1_120);
        assertTrue(sent.isEmpty());

        cycleAt(1_140);
        assertEquals(List.of("a@1140"), sent);
    }

    @Test
    void travelBetweenTargetsLengthensTheGap() {
        scheduler = schedulerWithGap(fixedGap(100, 60));
        startTick(TICK_START);
        Point first = new Point(10, 10);
        Point second = new Point(200, 10);
        scheduler.schedule(this, ClickTiming.at(20), () -> first, click("a", first));
        scheduler.schedule(this, ClickTiming.at(20), () -> second, click("b", second));

        cycleAt(1_020);
        cycleAt(1_120);
        cycleAt(1_160);
        assertEquals(List.of("a@1020"), sent);

        cycleAt(1_180);
        assertEquals(List.of("a@1020", "b@1180"), sent);
    }

    @Test
    void theDeadlineBeatsTheGap() {
        scheduler.schedule(this, ClickTiming.at(200), null, click("a"));
        scheduler.schedule(this, ClickTiming.at(220), null, click("b"));

        cycleAt(1_200);
        cycleAt(1_220);
        cycleAt(1_260);
        assertEquals(List.of("a@1200"), sent);

        // The next cycle, about 20 ms away, would be past the 300 ms cutoff, so the click leaves now.
        cycleAt(1_281);
        assertEquals(List.of("a@1200", "b@1281"), sent);
    }

    @Test
    void anOffsetPastTheDeadlineLeavesInTheLastCycleBeforeIt() {
        scheduler.schedule(this, ClickTiming.at(450), null, click("a"));

        cycleAt(1_260);
        cycleAt(1_280);
        assertTrue(sent.isEmpty());

        cycleAt(1_290);
        assertEquals(List.of("a@1290"), sent);
    }

    @Test
    void anExplicitDeadlineReplacesTheCutoff() {
        scheduler.schedule(this, ClickTiming.at(450, 500), null, click("a"));

        cycleAt(1_300);
        cycleAt(1_440);
        assertTrue(sent.isEmpty());

        cycleAt(1_450);
        assertEquals(List.of("a@1450"), sent);
    }

    @Test
    void theCutoffCanBeChanged() {
        scheduler.setCutoffMs(200);
        scheduler.schedule(this, ClickTiming.at(250), null, click("a"));

        cycleAt(1_160);
        assertTrue(sent.isEmpty());

        cycleAt(1_190);
        assertEquals(List.of("a@1190"), sent);
    }

    @Test
    void aSkippedClickDoesNotStartAGap() {
        ScheduledClick skipped = scheduler.schedule(this, ClickTiming.at(40), null, () -> false);
        scheduler.schedule(this, ClickTiming.at(40), null, click("b"));

        cycleAt(1_040);

        assertEquals(ScheduledClick.State.SKIPPED, skipped.getState());
        assertEquals(List.of("b@1040"), sent);
    }

    @Test
    void cancellingAnOwnerDropsOnlyItsClicks() {
        Object other = new Object();
        ScheduledClick mine = scheduler.schedule(this, ClickTiming.at(40), null, click("a"));
        scheduler.schedule(other, ClickTiming.at(40), null, click("b"));

        scheduler.cancel(this);
        cycleAt(1_040);

        assertEquals(ScheduledClick.State.CANCELLED, mine.getState());
        assertEquals(List.of("b@1040"), sent);
    }

    @Test
    void aSentClickCannotBeCancelled() {
        ScheduledClick click = scheduler.schedule(this, ClickTiming.at(40), null, click("a"));
        cycleAt(1_040);

        assertFalse(click.cancel());
        assertTrue(click.isSent());
    }

    @Test
    void theNextGameTickSendsClicksAStallKeptPastTheirDeadline() {
        scheduler.schedule(this, ClickTiming.at(100), null, click("a"));

        startTick(1_650);

        assertEquals(List.of("a@1650"), sent);
    }

    @Test
    void leavingTheGameCancelsPendingClicks() {
        ScheduledClick click = scheduler.schedule(this, ClickTiming.at(100), null, click("a"));

        GameStateChanged loggedOut = new GameStateChanged();
        loggedOut.setGameState(GameState.LOGIN_SCREEN);
        scheduler.onGameStateChanged(loggedOut);
        cycleAt(1_100);

        assertEquals(ScheduledClick.State.CANCELLED, click.getState());
        assertTrue(sent.isEmpty());
    }

    @Test
    void loadingKeepsPendingClicks() {
        ScheduledClick click = scheduler.schedule(this, ClickTiming.at(100), null, click("a"));

        GameStateChanged loading = new GameStateChanged();
        loading.setGameState(GameState.LOADING);
        scheduler.onGameStateChanged(loading);

        assertTrue(click.isPending());
    }

    @Test
    void aClickWithNoRecentTickIsTimedFromNow() {
        clock.set(5_000);
        scheduler.schedule(this, ClickTiming.at(100), null, click("a"));

        cycleAt(5_080);
        assertTrue(sent.isEmpty());

        cycleAt(5_100);
        assertEquals(List.of("a@5100"), sent);
    }

    @Test
    void measuresTheClientCycle() {
        for (long at = 2_000; at <= 2_600; at += 25) {
            cycleAt(at);
        }

        assertEquals(25, scheduler.getCycleMs());
    }

    @Test
    void reportsTheTimeIntoTheTick() {
        clock.set(TICK_START + 230);

        assertEquals(230, scheduler.getMillisIntoTick());
        assertEquals(TICK_START, scheduler.getTickStartMillis());
    }
}
