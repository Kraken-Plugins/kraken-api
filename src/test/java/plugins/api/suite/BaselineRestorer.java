package plugins.api.suite;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kraken.api.Context;
import com.kraken.api.service.prayer.PrayerService;
import lombok.extern.slf4j.Slf4j;
import plugins.api.TargetTileProvider;
import plugins.api.precondition.BankHelper;
import plugins.api.requirements.SideEffect;
import plugins.api.requirements.TestRequirements;

/**
 * Cleans up between tests, doing only what the precondition engine cannot.
 *
 * <p>A full restore after every test would be mostly wasted work: the engine already drives the world
 * into the next test's requirements starting from wherever it happens to be, so undoing a teleport
 * just to walk back is pure cost. What it cannot undo is state that no requirement describes — an
 * interface someone left open, a prayer still draining, a special attack still armed.</p>
 *
 * <p>Nothing here travels, and nothing here throws: it runs in cleanup paths where a secondary
 * failure would mask the real result.</p>
 */
@Slf4j
@Singleton
public class BaselineRestorer {

    /** Special attack orb, matching the id the widget action test drives. */
    private static final int WIDGET_SPECIAL_ATTACK_ORB = 10485796;

    @Inject
    private Context ctx;

    @Inject
    private BankHelper bankHelper;

    @Inject
    private PrayerService prayerService;

    @Inject
    private TargetTileProvider targetTiles;

    /**
     * Cheap cleanup after one test, before the next.
     *
     * @param justRan the test that finished; may be null
     * @param token cancellation token, honoured so a stop is not delayed by cleanup
     */
    public void restoreBetween(RegisteredTest justRan, CancellationToken token) {
        bankHelper.closeEverything(token);
        targetTiles.clearSuiteTile();

        if (justRan == null) {
            return;
        }

        TestRequirements requirements = justRan.requirements();

        if (requirements.hasSideEffect(SideEffect.TOGGLES_PRAYER)) {
            try {
                // Prayers drain, and a partially drained prayer level confuses the dps test's
                // stable-levels requirement later in the run.
                prayerService.deactivateAll();
            } catch (Exception e) {
                log.warn("Could not turn prayers off after {}", justRan.getId(), e);
            }
        }
    }

    /**
     * Final cleanup once, at the end of a run.
     *
     * @param token cancellation token
     */
    public void restoreAtEndOfRun(CancellationToken token) {
        bankHelper.closeEverything(token);
        targetTiles.clearSuiteTile();

        try {
            prayerService.deactivateAll();
        } catch (Exception e) {
            log.debug("Could not turn prayers off at the end of the run", e);
        }
    }
}
