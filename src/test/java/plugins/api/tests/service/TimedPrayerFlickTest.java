package plugins.api.tests.service;

import com.google.inject.Inject;
import com.kraken.api.Context;
import com.kraken.api.core.interaction.timing.ClickScheduler;
import com.kraken.api.core.interaction.timing.ClickTiming;
import com.kraken.api.core.packet.entity.MousePackets;
import com.kraken.api.service.prayer.PrayerService;
import com.kraken.api.service.util.SleepService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Prayer;
import net.runelite.api.Skill;
import net.runelite.api.SoundEffectID;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.SoundEffectPlayed;
import net.runelite.api.events.VarbitChanged;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import plugins.api.requirements.CustomRequirement;
import plugins.api.requirements.SideEffect;
import plugins.api.requirements.TestRequirements;
import plugins.api.tests.BaseApiTest;
import plugins.api.tests.service.timing.FlickRecorder;
import plugins.api.tests.service.timing.FlickRecorder.SignalType;
import plugins.api.tests.service.timing.FlickReport;
import plugins.api.tests.service.timing.PhantomBoss;
import plugins.api.tests.service.timing.PhantomBoss.Feint;
import plugins.api.tests.service.timing.PhantomBoss.PlannedFlick;

/**
 * Lazy flicks Thick Skin against a {@link PhantomBoss} through {@link PrayerService#schedule}, the path humanized
 * auto prayers send their clicks down, and checks the {@link ClickScheduler} kept every click in its tick: the prayer
 * is up on every hit tick and down between hits, each change is exactly one click, no click leaves before its offset
 * or after its deadline, an immediate click leaves at once and a cancelled click never leaves.
 *
 * <p>Thick Skin needs Prayer level 1, so any account can run it anywhere. Free to play accounts have no boss to
 * attack them, so the phantom boss supplies the hits: a scripted pattern of hit ticks that stands in for the attacks
 * a plugin would flick against. The prayer itself protects nothing, which is fine: only its timing is under test.
 *
 * <p>Kraken's clicks are counted from the time {@link MousePackets} stamps on each click packet, since they never
 * post a {@code MenuOptionClicked} event. Leave the mouse alone during the run: the prayer must be the only thing
 * Kraken clicks.
 *
 * <p>The client shows a clicked prayer's new state by the next client cycle, whether or not the server applied the
 * click in time, so the prayer state checks prove each click went out in the right tick, not that the server got it
 * in time. To look for the server's side, the run also records every change of the prayer's varp and the prayer's
 * sounds, and the log reports any that arrived with a tick update instead of right after the click. Its calibration
 * hits send their clicks past the cutoff on purpose to find where the server starts applying them a tick late. Those
 * measurements never fail the test.
 *
 * <p>The run takes about a minute, starts and ends with every prayer off, and should drain little or no
 * prayer: a prayer held for one tick is not meant to drain (unverified), and the log reports the points before and
 * after.
 */
@Slf4j
public class TimedPrayerFlickTest extends BaseApiTest {

    private static final Prayer PRAYER = Prayer.THICK_SKIN;
    private static final int TICKS_BEFORE_FIRST_HIT = 3;
    private static final long RUN_TIMEOUT_MS = 180_000;
    private static final String LOG_PREFIX = "TIMED_PRAYER_FLICKS ";

    @Inject
    private PrayerService prayerService;

    @Inject
    private ClickScheduler clickScheduler;

    @Inject
    private EventBus eventBus;

    @Inject
    private MousePackets mousePackets;

    private Driver driver;

    @Override
    public TestRequirements requirements() {
        return TestRequirements.builder()
                .customCheck(CustomRequirement.of("prayer points left",
                        context -> context.getClient().getBoostedSkillLevel(Skill.PRAYER) > 0))
                .customCheck(CustomRequirement.of("the click scheduler sending timed clicks (immediate mode off)",
                        context -> !ClickScheduler.isImmediateMode()))
                .sideEffect(SideEffect.TOGGLES_PRAYER)
                .timeoutMs(RUN_TIMEOUT_MS + 60_000)
                .build();
    }

    @Override
    protected boolean runTest(Context ctx) {
        Client client = ctx.getClient();
        prayerService.deactivateAll();
        SleepService.sleepFor(2);
        if (!assertTrue(!ctx.runOnClientThread(() -> client.isPrayerActive(PRAYER)), "Thick Skin is off before the run")) {
            return false;
        }

        int varp = ctx.runOnClientThread(() -> client.getVarbit(PRAYER.getVarbit()).getIndex());
        int startPoints = ctx.runOnClientThread(() -> client.getBoostedSkillLevel(Skill.PRAYER));

        driver = new Driver(client, varp, mousePackets.getLastClickMillis());
        eventBus.register(driver);
        boolean finished;
        try {
            driver.arm();
            finished = SleepService.sleepUntil(driver::isDone, RUN_TIMEOUT_MS);
        } finally {
            eventBus.unregister(driver);
            prayerService.cancelScheduled(this);
        }

        int endPoints = ctx.runOnClientThread(() -> client.getBoostedSkillLevel(Skill.PRAYER));
        prayerService.toggle(PRAYER, false);

        boolean passed = assertTrue(finished && driver.getBoss() != null,
                "the phantom boss fight finished within " + RUN_TIMEOUT_MS / 1000 + " s");
        if (driver.getBoss() == null) {
            return false;
        }

        FlickRecorder recorder = driver.getRecorder();
        recorder.prayerPoints(startPoints, endPoints);
        FlickReport report = recorder.report(driver.getBoss(), clickScheduler.getCutoffMs(),
                clickScheduler.getCycleMs());
        for (FlickReport.Check check : report.getChecks()) {
            log.info("{}{} {}: {}", LOG_PREFIX, check.isPassed() ? "PASS" : "FAIL", check.getName(), check.getDetail());
            passed &= assertTrue(check.isPassed(), check.getName() + ": " + check.getDetail());
        }
        report.getDetails().forEach(line -> log.info("{}{}", LOG_PREFIX, line));
        return passed;
    }

    @Override
    protected void onFinish() {
        if (driver != null) {
            eventBus.unregister(driver);
            driver = null;
        }
        prayerService.cancelScheduled(this);
    }

    @Override
    public String getTestName() {
        return "Timed Prayer Flicks";
    }

    /**
     * Runs the phantom boss on the client thread: schedules each tick's planned clicks, runs the feint, and feeds the
     * recorder every game tick, every click Kraken sends, every change of the prayer's varp and every prayer sound.
     *
     * <p>Kraken sends a click by calling the client's {@code doAction} directly, so it never shows up as a
     * {@code MenuOptionClicked} event. It does go through {@link MousePackets}, which stamps the time of every click
     * packet, so the driver watches that stamp: after each scheduling call, at every game tick (a click the scheduler
     * sends late goes out at the start of the next one) and every client cycle (after the scheduler has sent the
     * cycle's clicks). The prayer is the only thing clicked during the run, so every new stamp is a click on it, and
     * the prayer's state at the previous look tells which way it went.
     */
    private final class Driver {
        private final Client client;
        private final int varp;
        private final int varbit;

        @Getter
        private final FlickRecorder recorder = new FlickRecorder();

        @Getter
        private volatile PhantomBoss boss;

        private volatile boolean armed;
        private volatile boolean done;
        private boolean feintPending;
        private long lastClickMillis;
        private boolean upBeforeClick;
        private int lastVarpCycle = -1;
        private boolean lastVarpUp;

        /**
         * @param client          the client
         * @param varp            the varp that holds the prayer
         * @param lastClickMillis when the last click packet before the run was sent, so it is not counted
         */
        Driver(Client client, int varp, long lastClickMillis) {
            this.client = client;
            this.varp = varp;
            this.varbit = PRAYER.getVarbit();
            this.lastClickMillis = lastClickMillis;
        }

        void arm() {
            armed = true;
        }

        boolean isDone() {
            return done;
        }

        /**
         * Records the tick, then sends the tick's planned clicks. Runs after the click scheduler has recorded the
         * tick's start, which every offset is measured from, and after it has sent any click from the last tick that
         * missed its deadline.
         *
         * @param event the game tick
         */
        @Subscribe
        public void onGameTick(GameTick event) {
            int tick = client.getTickCount();
            if (armed && !done) {
                pollClicks();
            }
            recorder.tick(tick, clickScheduler.getTickStartMillis(), client.getGameCycle(), client.isPrayerActive(PRAYER));
            if (!armed || done) {
                return;
            }

            if (boss == null) {
                boss = PhantomBoss.create(tick + TICKS_BEFORE_FIRST_HIT, clickScheduler.getCutoffMs());
                log.info("{}phantom boss hits on {} ticks from tick {} to {}", LOG_PREFIX, boss.getHitTicks().size(),
                        boss.getHitTicks().first(), boss.getHitTicks().last());
            }

            for (PlannedFlick flick : boss.flicksAt(tick)) {
                recorder.scheduled(flick, prayerService.schedule(TimedPrayerFlickTest.this, PRAYER, flick.isOn(),
                        flick.timing()));
                pollClicks();
            }

            Feint feint = boss.getFeint();
            if (tick == feint.getTick()) {
                prayerService.schedule(TimedPrayerFlickTest.this, PRAYER, true, ClickTiming.at(feint.getOnOffsetMs()));
                recorder.feintScheduled(prayerService.isInFlight(PRAYER), prayerService.willBeActive(PRAYER));
                pollClicks();
                feintPending = true;
            }

            if (tick >= boss.getEndTick()) {
                done = true;
            }
        }

        /**
         * Records the clicks the scheduler sent this client cycle, and calls the feint off partway through its tick,
         * before its click is due. Registered after the scheduler, so it runs after the scheduler's own handler.
         *
         * @param event the client tick
         */
        @Subscribe
        public void onClientTick(ClientTick event) {
            if (!armed || done || boss == null) {
                return;
            }

            pollClicks();
            if (!feintPending || clickScheduler.getMillisIntoTick() < boss.getFeint().getCancelAfterMs()) {
                return;
            }

            feintPending = false;
            prayerService.schedule(TimedPrayerFlickTest.this, PRAYER, false,
                    ClickTiming.at(boss.getFeint().getOnOffsetMs()));
            recorder.feintCancelled(prayerService.isInFlight(PRAYER), prayerService.willBeActive(PRAYER));
            pollClicks();
        }

        /**
         * Records a click when the time of the last click packet has moved on since the last look, then notes the
         * prayer's state, which the next click starts from.
         */
        private void pollClicks() {
            long sent = mousePackets.getLastClickMillis();
            if (sent != lastClickMillis) {
                lastClickMillis = sent;
                recorder.clicked(sent, client.getGameCycle(), !upBeforeClick);
            }
            upBeforeClick = client.isPrayerActive(PRAYER);
        }

        /**
         * Records every change of the varp that holds the prayer, local or from the server, whether RuneLite reports
         * it for the varp or for the prayer's varbit, once per client cycle and state.
         *
         * @param event the varp change
         */
        @Subscribe
        public void onVarbitChanged(VarbitChanged event) {
            boolean prayerVar = (event.getVarbitId() == -1 && event.getVarpId() == varp) || event.getVarbitId() == varbit;
            if (!armed || !prayerVar) {
                return;
            }

            int cycle = client.getGameCycle();
            boolean up = client.isPrayerActive(PRAYER);
            if (cycle == lastVarpCycle && up == lastVarpUp) {
                return;
            }

            lastVarpCycle = cycle;
            lastVarpUp = up;
            recorder.signal(SignalType.VARP, System.currentTimeMillis(), cycle, up);
        }

        /**
         * Records the prayer's activation and deactivation sounds.
         *
         * @param event the sound
         */
        @Subscribe
        public void onSoundEffectPlayed(SoundEffectPlayed event) {
            if (!armed) {
                return;
            }

            if (event.getSoundId() == SoundEffectID.PRAYER_ACTIVATE_THICK_SKIN) {
                recorder.signal(SignalType.SOUND, System.currentTimeMillis(), client.getGameCycle(), true);
            } else if (event.getSoundId() == SoundEffectID.PRAYER_DEACTIVE_VWOOP) {
                recorder.signal(SignalType.SOUND, System.currentTimeMillis(), client.getGameCycle(), false);
            }
        }
    }
}
