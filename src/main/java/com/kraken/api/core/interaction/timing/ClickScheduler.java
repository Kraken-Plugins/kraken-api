package com.kraken.api.core.interaction.timing;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kraken.api.core.interaction.ClickHistory;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Point;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BooleanSupplier;
import java.util.function.LongFunction;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * Sends clicks at chosen points inside a game tick instead of the instant a {@code GameTick} handler runs, and keeps a
 * human gap between clicks from every plugin.
 *
 * <p>The server reads queued client packets at the start of its tick, so a click sent any time before the cutoff
 * (about {@code 600 - round trip - processing time} ms after the tick started) applies on the next tick, just like a
 * click sent at once. The scheduler sends each click at its {@link ClickTiming} offset when the last click, from any
 * plugin, is at least a {@link ClickGapModel} gap old. A click whose deadline would pass before the next client cycle
 * goes out at once, gap or not, so a late client cycle never pushes a click past the cutoff.
 *
 * <p>The gap counts every click the game sends: Kraken's own click packets and the presses the game reads from the
 * canvas, the player's own included ({@link ClickHistory}). The travel part of the gap runs from wherever the latest
 * of those landed.
 *
 * <p>One scheduler serves every plugin: Guice creates it in RuneLite's injector, it registers itself with the event
 * bus and runs on the client thread from {@code ClientTick}, which fires every client cycle (about 20 ms). Clicks
 * scheduled with {@link ClickTiming#immediate()}, or while #isImmediateMode() is on, run inside
 * {@link #schedule} exactly as an unscheduled interaction would.
 */
@Slf4j
@Singleton
public class ClickScheduler {

    /**
     * The length of a game tick in milliseconds.
     */
    public static final int TICK_LENGTH_MS = 600;

    /**
     * The cutoff until it is calibrated: safe for round trips up to about 250 ms.
     */
    public static final int DEFAULT_CUTOFF_MS = 300;

    /**
     * The length of a client cycle until one has been measured.
     */
    public static final int DEFAULT_CYCLE_MS = 20;

    /**
     * The system property that starts the scheduler in  #isImmediateMode() immediate mode, for example
     * {@code -Dkraken.clicks.immediate=true}.
     */
    public static final String IMMEDIATE_MODE_PROPERTY = "kraken.clicks.immediate";

    /**
     *  Turns immediate mode on or off for every plugin. With it on, every click is sent within the call that
     *  schedules it, the way interactions worked before scheduling existed, for comparison and emergencies.
     */
    @Setter
    @Getter
    private static volatile boolean immediateMode = Boolean.getBoolean(IMMEDIATE_MODE_PROPERTY);

    private final LongSupplier clock;
    private final Random random;
    private final LongSupplier lastClickMillis;
    private final Supplier<Point> lastClickPoint;
    private final LongSupplier accountHash;
    private final List<ScheduledClick> pending = new ArrayList<>();
    private final ClickGapModel baseGapModel;

    @Getter
    private volatile ClickGapModel gapModel;
    private volatile LongFunction<ClickGapModel> gapModelFactory;
    private volatile boolean gapModelStale;
    private long gapModelAccountHash = Long.MIN_VALUE;
    private long sequence;
    private long lastClientTickMillis;

    /**
     * When this scheduler last sent a click. Written from the client thread and from any thread that schedules an
     * immediate click, so it only ever moves forward through an atomic max.
     */
    private final AtomicLong lastSentMillis = new AtomicLong();

    /**
     * When the current game tick started, from the scheduler's clock, or 0 before the first tick.
     */
    @Getter
    private volatile long tickStartMillis;

    /**
     * Moving average of the client cycle length. Read and written only on the client thread, from
     * {@link #onClientTick}; {@link #cycleMs} publishes it to other threads.
     */
    private double cycleEstimateMs = DEFAULT_CYCLE_MS;

    /**
     * The measured length of a client cycle in milliseconds, a moving average of recent cycles. Only
     * {@link #onClientTick} writes it.
     * -- GETTER --
     *
     * @return the measured length of a client cycle in milliseconds, a moving average of recent cycles.

     */
    @Getter
    private volatile int cycleMs = DEFAULT_CYCLE_MS;

    /**
     * Milliseconds after the start of a tick by which a click must leave to apply on the next tick. Clicks scheduled
     * with {@link ClickTiming#SCHEDULER_CUTOFF} use it as their deadline.
     */
    @Getter
    private volatile int cutoffMs = DEFAULT_CUTOFF_MS;

    /**
     * Creates the shared scheduler and registers it with the event bus.
     *
     * @param eventBus     the event bus that delivers game ticks and client ticks.
     * @param client       the client, read for the account hash that seeds the gap model.
     * @param clickHistory the time and place of the last click from any source: Kraken's click packets and the
     *                     presses the game read from the canvas.
     */
    @Inject
    public ClickScheduler(EventBus eventBus, Client client, ClickHistory clickHistory) {
        this(System::currentTimeMillis, new Random(), HumanClickGap.DEFAULT, clickHistory::getLastClickMillis,
                clickHistory::getLastClickPoint, client::getAccountHash);
        eventBus.register(this);
    }

    /**
     * Creates a scheduler driven by the given clock that is not registered with an event bus, for tests and tools
     * that call its event handlers themselves.
     *
     * @param clock           the time source, in milliseconds.
     * @param random          the random source for gaps.
     * @param gapModel        the gap model to start with.
     * @param lastClickMillis when the last click from any source was sent, or 0.
     * @param lastClickPoint  where the last click from any source was, or null.
     * @param accountHash     the logged in account's hash for a per-account gap model, or null to keep
     *                        {@code gapModel} unless a {@link #setGapModelFactory gap model factory} gives one.
     */
    public ClickScheduler(LongSupplier clock, Random random, ClickGapModel gapModel, LongSupplier lastClickMillis,
                          Supplier<Point> lastClickPoint, LongSupplier accountHash) {
        this.clock = clock;
        this.random = random;
        this.gapModel = gapModel;
        this.baseGapModel = gapModel;
        this.lastClickMillis = lastClickMillis;
        this.lastClickPoint = lastClickPoint;
        this.accountHash = accountHash;
    }

    /**
     * Sets the cutoff that clicks without their own deadline must leave by.
     *
     * @param cutoffMs milliseconds after the start of a tick, from 1 to {@link #TICK_LENGTH_MS}.
     */
    public void setCutoffMs(int cutoffMs) {
        if (cutoffMs < 1 || cutoffMs > TICK_LENGTH_MS) {
            throw new IllegalArgumentException("cutoffMs must be between 1 and " + TICK_LENGTH_MS + ": " + cutoffMs);
        }
        this.cutoffMs = cutoffMs;
    }

    /**
     * Supplies the gap model for each account, in place of the seeded {@link HumanClickGap#forAccount}. The factory
     * runs on the client thread at the next game tick, and again whenever the account changes or
     * {@link #refreshGapModel()} is called, so a model set this way survives account changes.
     *
     * @param factory takes the account hash (-1 when logged out) and returns the model, or null for the seeded
     *                model; pass null to go back to the seeded model for every account.
     */
    public void setGapModelFactory(LongFunction<ClickGapModel> factory) {
        this.gapModelFactory = factory;
        this.gapModelStale = true;
    }

    /**
     * Rebuilds the gap model at the next game tick, for a {@link #setGapModelFactory factory} whose answer changed,
     * such as a click profile that was fitted again.
     */
    public void refreshGapModel() {
        this.gapModelStale = true;
    }

    /**
     * Schedules a click.
     *
     * <p>The action runs on the client thread when the click is due (or, for an immediate click, within this call on
     * the calling thread). It should check the game state and return false without clicking when the click is no
     * longer needed, so a click that was overtaken by the player or another plugin is skipped rather than reversing
     * their change.
     *
     * @param owner    the object scheduling the click, used by {@link #cancel(Object)}.
     * @param timing   when the click should leave.
     * @param location where on the canvas the click will land, read on the client thread for the travel time from
     *                 the previous click, or null when unknown.
     * @param action   sends the click and returns true, or returns false when no click is needed.
     * @return the scheduled click.
     */
    public ScheduledClick schedule(Object owner, ClickTiming timing, Supplier<Point> location, BooleanSupplier action) {
        return scheduleTargeted(owner, timing, location == null ? null : () -> ClickLocation.of(location.get()), action);
    }

    /**
     * Schedules a click whose location also gives the bounds of its target, so the travel time from the previous
     * click uses the target's real size. Otherwise the same as {@link #schedule}.
     *
     * @param owner    the object scheduling the click, used by {@link #cancel(Object)}.
     * @param timing   when the click should leave.
     * @param location where on the canvas the click will land and the bounds of its target, read on the client thread
     *                 for the travel time from the previous click, or null when unknown.
     * @param action   sends the click and returns true, or returns false when no click is needed.
     * @return the scheduled click.
     */
    public ScheduledClick scheduleTargeted(Object owner, ClickTiming timing, Supplier<ClickLocation> location,
                                           BooleanSupplier action) {
        Objects.requireNonNull(timing, "timing");
        Objects.requireNonNull(action, "action");
        long now = clock.getAsLong();

        if (timing.isImmediate() || immediateMode) {
            ScheduledClick click;
            synchronized (this) {
                click = new ScheduledClick(owner, now, now, 0, location, action, sequence++);
            }
            send(click, now);
            return click;
        }

        long tickStart = currentTickStart(now);
        int deadlineMs = timing.getDeadlineMs() == ClickTiming.SCHEDULER_CUTOFF ? cutoffMs : timing.getDeadlineMs();
        synchronized (this) {
            ScheduledClick click = new ScheduledClick(owner, tickStart + timing.getOffsetMs(), tickStart + deadlineMs,
                    gapModel.baseGapMs(random), location, action, sequence++);
            pending.add(click);
            return click;
        }
    }

    /**
     * Cancels every pending click scheduled by {@code owner}. Call it when a plugin stops.
     *
     * @param owner the owner passed to {@link #schedule}.
     */
    public synchronized void cancel(Object owner) {
        pending.removeIf(click -> click.getOwner() == owner && click.cancel());
    }

    /**
     * Cancels every pending click from every owner.
     */
    public synchronized void cancelAll() {
        pending.forEach(ScheduledClick::cancel);
        pending.clear();
    }

    /**
     * @return the number of clicks waiting to be sent.
     */
    public synchronized int getPendingCount() {
        pending.removeIf(click -> !click.isPending());
        return pending.size();
    }

    /**
     * @return the scheduler's clock in milliseconds, which {@link ScheduledClick} times are read against.
     */
    public long currentTimeMillis() {
        return clock.getAsLong();
    }

    /**
     * @return milliseconds since the current game tick started, or 0 before the first tick.
     */
    public long getMillisIntoTick() {
        long start = tickStartMillis;
        return start == 0 ? 0 : clock.getAsLong() - start;
    }

    /**
     * Records the start of the tick before plugins' own game tick handlers schedule clicks against it, and sends any
     * click from the last tick that a client stall kept past its deadline.
     *
     * @param event the game tick.
     */
    @Subscribe(priority = 19999)
    public void onGameTick(GameTick event) {
        long now = clock.getAsLong();
        sendOverdue(now);
        tickStartMillis = now;
        updateGapModel();
    }

    /**
     * Sends the clicks that are due this client cycle.
     *
     * @param event the client tick.
     */
    @Subscribe
    public void onClientTick(ClientTick event) {
        long now = clock.getAsLong();
        measureCycle(now);
        process(now);
    }

    /**
     * Drops every pending click when the client leaves the game, since none of them can be sent.
     *
     * @param event the game state change.
     */
    @Subscribe
    public void onGameStateChanged(GameStateChanged event) {
        GameState state = event.getGameState();
        if (state != GameState.LOGGED_IN && state != GameState.LOADING) {
            cancelAll();
        }
    }

    /**
     * Sends due clicks in order of their send time. A click goes out once its send time has come and the last click
     * is at least its gap old, or at once when the next client cycle would be past its deadline. Sending one click
     * resets the gap, so clicks that fall due together leave one after another. Runs on the client thread only,
     * from {@link #onClientTick}, which is what lets it read the cycle estimate without synchronization.
     *
     * @param now the scheduler's clock.
     */
    void process(long now) {
        List<ScheduledClick> due;
        synchronized (this) {
            pending.removeIf(click -> !click.isPending());
            if (pending.isEmpty()) {
                return;
            }
            pending.sort(Comparator.comparingLong(ScheduledClick::getSendAtMillis).thenComparingLong(ScheduledClick::getSequence));
            due = new ArrayList<>(pending);
        }

        for (ScheduledClick click : due) {
            boolean lastChance = now + cycleEstimateMs > click.getDeadlineMillis();
            if (!lastChance && (now < click.getSendAtMillis() || !gapElapsed(click, now))) {
                continue;
            }
            send(click, now);
        }
    }

    /**
     * Sends every pending click whose deadline has already passed.
     *
     * @param now the scheduler's clock.
     */
    private void sendOverdue(long now) {
        List<ScheduledClick> overdue = new ArrayList<>();
        synchronized (this) {
            for (ScheduledClick click : pending) {
                if (click.isPending() && click.getDeadlineMillis() <= now) {
                    overdue.add(click);
                }
            }
        }
        overdue.sort(Comparator.comparingLong(ScheduledClick::getSequence));
        overdue.forEach(click -> send(click, now));
    }

    private boolean gapElapsed(ScheduledClick click, long now) {
        long lastClick = Math.max(lastSentMillis.get(), lastClickMillis.getAsLong());
        if (lastClick == 0) {
            return true;
        }

        ClickLocation to = location(click);
        int travel = to == null ? 0 : gapModel.travelMs(lastClickPoint.get(), to.getPoint(), to.getTargetWidthPx());
        return now - lastClick >= click.getBaseGapMs() + travel;
    }

    private ClickLocation location(ScheduledClick click) {
        if (click.getLocation() == null) {
            return null;
        }

        try {
            return click.getLocation().get();
        } catch (RuntimeException e) {
            log.debug("Could not read a scheduled click's location", e);
            return null;
        }
    }

    /**
     * Runs a click's action unless it was cancelled, and records the outcome.
     *
     * @param click the click to send.
     * @param now   the scheduler's clock.
     */
    private void send(ScheduledClick click, long now) {
        if (!click.claim()) {
            return;
        }

        boolean clicked = false;
        try {
            clicked = click.getAction().getAsBoolean();
        } catch (RuntimeException e) {
            log.warn("Scheduled click failed", e);
        }

        click.finish(clicked, now);
        if (clicked) {
            lastSentMillis.accumulateAndGet(now, Math::max);
        }
    }

    /**
     * The start of the tick a new click is timed against: the last game tick, or now when no tick has been seen for
     * two ticks (logging in, loading, a frozen server).
     *
     * @param now the scheduler's clock.
     * @return the tick start.
     */
    private long currentTickStart(long now) {
        long start = tickStartMillis;
        return start == 0 || now - start > 2L * TICK_LENGTH_MS ? now : start;
    }

    private void measureCycle(long now) {
        long previous = lastClientTickMillis;
        lastClientTickMillis = now;
        if (previous == 0) {
            return;
        }

        long interval = now - previous;
        if (interval >= 5 && interval <= 200) {
            cycleEstimateMs = Math.max(10, Math.min(100, cycleEstimateMs * 0.8 + interval * 0.2));
            cycleMs = (int) Math.round(cycleEstimateMs);
        }
    }

    /**
     * Rebuilds the gap model when the account changed or a refresh was asked for: the factory's model when it gives
     * one, otherwise the account's seeded model (or the starting model when there is no account source). Runs on the
     * client thread only, from {@link #onGameTick}.
     */
    private void updateGapModel() {
        long hash = accountHash == null ? -1 : accountHash.getAsLong();
        if (!gapModelStale && (accountHash == null || hash == gapModelAccountHash)) {
            return;
        }

        gapModelStale = false;
        gapModelAccountHash = hash;
        LongFunction<ClickGapModel> factory = gapModelFactory;
        ClickGapModel model = null;
        if (factory != null) {
            try {
                model = factory.apply(hash);
            } catch (RuntimeException e) {
                log.warn("Gap model factory failed", e);
            }
        }

        if (model == null) {
            if (accountHash == null) {
                model = baseGapModel;
            } else {
                model = hash == -1 ? HumanClickGap.DEFAULT : HumanClickGap.forAccount(hash);
            }
        }
        gapModel = model;
    }
}
