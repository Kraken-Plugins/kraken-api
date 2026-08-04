package plugins.api.tests.service;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kraken.api.Context;
import com.kraken.api.query.gameobject.GameObjectEntity;
import com.kraken.api.service.bank.BankService;
import com.kraken.api.service.pathfinding.LocalPathfinder;
import com.kraken.api.service.util.SleepService;
import com.kraken.api.service.util.TaskChain;
import plugins.api.tests.BaseApiTest;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.coords.WorldPoint;

import java.util.List;

/**
 * Exercises {@link TaskChain} composition: {@code walkTo}, {@code run}, {@code waitUntil},
 * {@code delayTicks} and the short circuiting behaviour of {@code execute}.
 *
 * <p>This previously walked to the Lumbridge canoe station and built a canoe. That made it by far the
 * most expensive test in the suite for no added coverage: what is under test is the chain's own
 * sequencing, not canoes. Driving a bank booth instead exercises the identical primitives while
 * standing where the rest of the suite already runs.</p>
 *
 * <p><b>Requires:</b> a nearby bank booth or banker, and open ground to take a few steps on.</p>
 */
@Slf4j
@Singleton
public class TaskChainTest extends BaseApiTest {

    private static final int MIN_WALK_TILES = 3;
    private static final int MAX_WALK_TILES = 6;
    private static final int ARRIVAL_TOLERANCE = 3;
    private static final int INTERFACE_TIMEOUT_MS = 8000;

    @Inject
    private BankService bankService;

    @Inject
    private LocalPathfinder localPathfinder;

    @Override
    protected boolean runTest(Context ctx) throws Exception {
        if (bankService.isOpen()) {
            // The chain asserts it can open the bank, so it must start closed.
            bankService.close();
            if (!waitForBankClosed()) {
                return assertThat(false, "Task chain test: the bank was open and would not close");
            }
        }

        WorldPoint start = ctx.players().local().location();
        if (!assertNotNull(start, "Task chain test: could not read the player's location")) {
            return false;
        }

        WorldPoint target = pickNearbyTile(start);
        if (target == null) {
            return assertThat(false, "Task chain test: no reachable tile between " + MIN_WALK_TILES
                    + " and " + MAX_WALK_TILES + " tiles away to walk to");
        }

        log.info("Task chain test: walking {} -> {} then driving a bank booth", start, target);

        boolean completed = TaskChain.builder(ctx)
                .walkTo(target)
                .waitUntil(() -> hasArrived(ctx, target), INTERFACE_TIMEOUT_MS)
                .run(() -> openNearestBank(ctx))
                .waitUntil(bankService::isOpen, INTERFACE_TIMEOUT_MS)
                .delayTicks(1)
                .run(() -> bankService.close())
                .waitUntil(bankService::isClosed, INTERFACE_TIMEOUT_MS)
                .execute();

        if (!assertTrue(completed, "Task chain test: the chain did not run to completion. "
                + "One of walkTo, run or waitUntil failed or timed out")) {
            return false;
        }

        // execute() returning true is the chain's own report; verify the world actually matches it.
        boolean testsPassed = assertTrue(hasArrived(ctx, target),
                "Task chain test: the chain reported success but the player is not at " + target);

        testsPassed &= assertTrue(bankService.isClosed(),
                "Task chain test: the chain reported success but the bank is still open");

        return testsPassed;
    }

    /**
     * Checks whether the player has reached the requested tile, within the chain's own tolerance.
     *
     * <p>{@link TaskChain#walkTo(WorldPoint)} considers itself arrived within three tiles, so this
     * must not demand an exact match or it would contradict the behaviour under test.</p>
     *
     * @param ctx the injected API context
     * @param target the tile the chain was asked to walk to
     * @return true when the player is within the arrival tolerance of the target
     */
    private boolean hasArrived(Context ctx, WorldPoint target) {
        WorldPoint current = ctx.players().local().location();
        return current != null && current.distanceTo(target) <= ARRIVAL_TOLERANCE;
    }

    /**
     * Picks a walkable tile a short distance away, chosen from tiles already known to be reachable.
     *
     * @param start the player's current tile
     * @return a nearby reachable tile, or null when the player is too confined to move
     */
    private WorldPoint pickNearbyTile(WorldPoint start) {
        List<WorldPoint> reachable = localPathfinder.reachableTiles(start);
        if (reachable == null || reachable.isEmpty()) {
            return null;
        }

        return reachable.stream()
                .filter(tile -> {
                    int distance = tile.distanceTo(start);
                    return distance >= MIN_WALK_TILES && distance <= MAX_WALK_TILES;
                })
                .findFirst()
                .orElse(null);
    }

    /**
     * Opens the nearest bank, preferring a booth and falling back to a banker NPC.
     *
     * @param ctx the injected API context
     */
    private void openNearestBank(Context ctx) {
        GameObjectEntity booth = ctx.gameObjects().withAction("Bank").nearest();
        if (booth != null && booth.isPresent()) {
            booth.interact("Bank");
            return;
        }
        ctx.npcs().withAction("Bank").nearest().interact("Bank");
    }

    /**
     * Waits for the bank interface to close so the chain can start from a known state.
     *
     * @return true when the bank is closed
     */
    private boolean waitForBankClosed() {
        return SleepService.sleepUntilTrue(bankService::isClosed, INTERFACE_TIMEOUT_MS);
    }

    @Override
    protected String getTestName() {
        return "Task Chain";
    }
}
