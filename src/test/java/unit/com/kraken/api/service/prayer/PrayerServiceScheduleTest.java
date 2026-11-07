package com.kraken.api.service.prayer;

import com.kraken.api.Context;
import com.kraken.api.core.interaction.InteractionManager;
import com.kraken.api.core.interaction.timing.ClickGapModel;
import com.kraken.api.core.interaction.timing.ClickLocation;
import com.kraken.api.core.interaction.timing.ClickScheduler;
import com.kraken.api.core.interaction.timing.ClickTiming;
import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.api.Prayer;
import net.runelite.api.Skill;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.GameTick;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.Rectangle;
import java.lang.reflect.Field;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Runs {@link PrayerService#schedule} against a fake client whose prayer state changes when the prayer widget is
 * clicked, the way the real client shows a clicked prayer by the next client cycle.
 */
class PrayerServiceScheduleTest {

    private static final int MAGIC_WIDGET = InteractablePrayer.PROTECT_MAGIC.getIndex();

    private final AtomicLong clock = new AtomicLong(1_000);
    private final Set<Prayer> active = EnumSet.noneOf(Prayer.class);
    private final AtomicBoolean clientShowsClicks = new AtomicBoolean(true);
    private final Client client = mock(Client.class);
    private final Context ctx = mock(Context.class);
    private final InteractionManager interactionManager = mock(InteractionManager.class);
    private ClickScheduler scheduler;
    private PrayerService prayerService;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() throws Exception {
        ClickGapModel noGap = new ClickGapModel() {
            @Override
            public int baseGapMs(Random random) {
                return 1;
            }

            @Override
            public int travelMs(Point from, Point to) {
                return 0;
            }
        };
        scheduler = new ClickScheduler(clock::get, new Random(1), noGap, () -> 0L, () -> null, null);
        scheduler.onGameTick(new GameTick());

        when(ctx.getClient()).thenReturn(client);
        when(client.getBoostedSkillLevel(Skill.PRAYER)).thenReturn(99);
        when(client.getRealSkillLevel(Skill.PRAYER)).thenReturn(99);
        when(client.isPrayerActive(any(Prayer.class))).thenAnswer(invocation -> active.contains(invocation.<Prayer>getArgument(0)));
        when(ctx.runOnClientThreadOptional(any(Callable.class)))
                .thenAnswer(invocation -> Optional.ofNullable(invocation.<Callable<Object>>getArgument(0).call()));
        when(ctx.getVarbitValue(anyInt())).thenReturn(0);
        when(interactionManager.interact(anyInt(), anyInt(), anyInt(), anyInt())).thenAnswer(invocation -> {
            if (clientShowsClicks.get() && invocation.<Integer>getArgument(0) == MAGIC_WIDGET) {
                if (!active.remove(Prayer.PROTECT_FROM_MAGIC)) {
                    active.add(Prayer.PROTECT_FROM_MAGIC);
                }
            }
            return true;
        });

        prayerService = new PrayerService();
        inject("ctx", ctx);
        inject("interactionManager", interactionManager);
        inject("clickScheduler", scheduler);
    }

    private void inject(String name, Object value) throws Exception {
        Field field = PrayerService.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(prayerService, value);
    }

    private void cycleAt(long at) {
        clock.set(at);
        scheduler.onClientTick(new ClientTick());
    }

    @Test
    void theClickLeavesAtItsOffset() {
        assertTrue(prayerService.schedule(this, Prayer.PROTECT_FROM_MAGIC, true, ClickTiming.at(150)));
        assertTrue(prayerService.isInFlight(Prayer.PROTECT_FROM_MAGIC));
        assertTrue(prayerService.willBeActive(Prayer.PROTECT_FROM_MAGIC));
        assertFalse(prayerService.isActive(Prayer.PROTECT_FROM_MAGIC));

        cycleAt(1_100);
        verify(interactionManager, never()).interact(anyInt(), anyInt(), anyInt(), anyInt());

        cycleAt(1_160);
        verify(interactionManager).interact(MAGIC_WIDGET, -1, -1, 1);
        assertTrue(prayerService.isActive(Prayer.PROTECT_FROM_MAGIC));
        assertFalse(prayerService.isInFlight(Prayer.PROTECT_FROM_MAGIC));
    }

    @Test
    void asksForTheSameStateOnlyClickOnce() {
        assertTrue(prayerService.schedule(this, Prayer.PROTECT_FROM_MAGIC, true, ClickTiming.at(50)));
        assertFalse(prayerService.schedule(this, Prayer.PROTECT_FROM_MAGIC, true, ClickTiming.at(80)));

        cycleAt(1_060);
        cycleAt(1_100);

        verify(interactionManager, times(1)).interact(anyInt(), anyInt(), anyInt(), anyInt());
    }

    @Test
    void theOppositeStateCancelsAClickNotYetSent() {
        prayerService.schedule(this, Prayer.PROTECT_FROM_MAGIC, true, ClickTiming.at(100));

        assertFalse(prayerService.schedule(this, Prayer.PROTECT_FROM_MAGIC, false, ClickTiming.at(100)));
        assertFalse(prayerService.willBeActive(Prayer.PROTECT_FROM_MAGIC));

        cycleAt(1_100);
        verify(interactionManager, never()).interact(anyInt(), anyInt(), anyInt(), anyInt());
    }

    @Test
    void aSentClickCountsAsAppliedForUpToATick() {
        clientShowsClicks.set(false);
        prayerService.schedule(this, Prayer.PROTECT_FROM_MAGIC, true, ClickTiming.at(40));
        cycleAt(1_040);

        clock.set(1_500);
        assertTrue(prayerService.isInFlight(Prayer.PROTECT_FROM_MAGIC));
        assertTrue(prayerService.willBeActive(Prayer.PROTECT_FROM_MAGIC));

        clock.set(1_640);
        assertFalse(prayerService.isInFlight(Prayer.PROTECT_FROM_MAGIC));
        assertFalse(prayerService.willBeActive(Prayer.PROTECT_FROM_MAGIC));
    }

    @Test
    void theOppositeStateAfterTheClickLeftNeedsASecondClick() {
        clientShowsClicks.set(false);
        prayerService.schedule(this, Prayer.PROTECT_FROM_MAGIC, true, ClickTiming.at(40));
        cycleAt(1_040);

        // The "on" click has left but the client does not show it yet, so turning the prayer off takes a click.
        assertTrue(prayerService.schedule(this, Prayer.PROTECT_FROM_MAGIC, false, ClickTiming.at(200)));

        active.add(Prayer.PROTECT_FROM_MAGIC);
        clientShowsClicks.set(true);
        cycleAt(1_200);

        verify(interactionManager, times(2)).interact(MAGIC_WIDGET, -1, -1, 1);
        assertFalse(prayerService.isActive(Prayer.PROTECT_FROM_MAGIC));
    }

    @Test
    void aClickDueAfterThePlayerMadeTheChangeIsSkipped() {
        prayerService.schedule(this, Prayer.PROTECT_FROM_MAGIC, true, ClickTiming.at(200));
        active.add(Prayer.PROTECT_FROM_MAGIC);

        cycleAt(1_200);

        verify(interactionManager, never()).interact(anyInt(), anyInt(), anyInt(), anyInt());
        assertTrue(prayerService.isActive(Prayer.PROTECT_FROM_MAGIC));
    }

    @Test
    void anImmediateClickLeavesWithinTheCall() {
        assertTrue(prayerService.schedule(this, Prayer.PROTECT_FROM_MAGIC, true, ClickTiming.immediate()));

        verify(interactionManager).interact(MAGIC_WIDGET, -1, -1, 1);
        assertTrue(prayerService.isActive(Prayer.PROTECT_FROM_MAGIC));
    }

    @Test
    void nothingIsScheduledForAPrayerAlreadyInTheWantedState() {
        active.add(Prayer.PROTECT_FROM_MAGIC);

        assertFalse(prayerService.schedule(this, Prayer.PROTECT_FROM_MAGIC, true, ClickTiming.at(50)));
        assertFalse(prayerService.isInFlight(Prayer.PROTECT_FROM_MAGIC));
    }

    @Test
    void nothingIsScheduledWithoutPrayerPoints() {
        when(client.getBoostedSkillLevel(Skill.PRAYER)).thenReturn(0);

        assertFalse(prayerService.schedule(this, Prayer.PROTECT_FROM_MAGIC, true, ClickTiming.at(50)));
    }

    @Test
    void cancellingAnOwnerDropsItsPendingClicks() {
        prayerService.schedule(this, Prayer.PROTECT_FROM_MAGIC, true, ClickTiming.at(100));

        prayerService.cancelScheduled(this);
        cycleAt(1_100);

        verify(interactionManager, never()).interact(anyInt(), anyInt(), anyInt(), anyInt());
        assertFalse(prayerService.isInFlight(Prayer.PROTECT_FROM_MAGIC));
    }

    @Test
    void theClickLandsOnThePointItsTravelWasTimedTo() {
        Point chosen = new Point(612, 318);
        when(interactionManager.chooseWidgetClick(MAGIC_WIDGET, 1))
                .thenReturn(ClickLocation.of(chosen, new Rectangle(600, 300, 37, 37)));
        when(interactionManager.interact(anyInt(), anyInt(), anyInt(), anyInt(), any(Point.class))).thenAnswer(invocation -> {
            active.add(Prayer.PROTECT_FROM_MAGIC);
            return true;
        });

        prayerService.schedule(this, Prayer.PROTECT_FROM_MAGIC, true, ClickTiming.at(150));
        cycleAt(1_160);

        verify(interactionManager, times(1)).chooseWidgetClick(MAGIC_WIDGET, 1);
        verify(interactionManager).interact(MAGIC_WIDGET, -1, -1, 1, chosen);
        verify(interactionManager, never()).interact(anyInt(), anyInt(), anyInt(), anyInt());
        assertTrue(prayerService.isActive(Prayer.PROTECT_FROM_MAGIC));
    }
}
