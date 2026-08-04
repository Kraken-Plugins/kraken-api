package plugins.api.tests;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kraken.api.Context;
import com.kraken.api.query.container.bank.BankEntity;
import com.kraken.api.query.gameobject.GameObjectEntity;
import com.kraken.api.query.npc.NpcEntity;
import com.kraken.api.service.bank.BankService;
import com.kraken.api.service.movement.MovementService;
import com.kraken.api.service.pathfinding.LocalPathfinder;
import com.kraken.api.service.util.SleepService;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.coords.WorldPoint;
import plugins.api.requirements.TestRequirements;
import plugins.api.world.Facility;

import java.util.List;

/**
 * Verifies the small set of API primitives that every other test's setup depends on.
 *
 * <p>This test exists because the harness drives the client using the very API it is meant to
 * validate. Object queries, {@code interact}, the bank container and player movement are how any
 * test reaches its starting state, so when one of them regresses after a client update every test
 * fails during setup and reports an indistinguishable, uninformative failure. Running this first
 * turns that into a single precise answer: either the harness can drive the client, or it cannot and
 * the rest of the run is meaningless.</p>
 *
 * <p>Each stage is deliberately narrow and reports the exact primitive that broke. The test is
 * intentionally non-destructive: it withdraws a single item and immediately deposits it back, and it
 * walks a handful of tiles it has already confirmed are reachable.</p>
 *
 * <p><b>Requires:</b> to be logged in and standing near a bank booth or banker NPC with at least one
 * item in the bank and a free inventory slot. Varrock West Bank satisfies all of this.</p>
 */
@Slf4j
@Singleton
public class SelfCheckTest extends BaseApiTest {

    private static final int INTERFACE_TIMEOUT_MS = 6000;
    private static final int CONTAINER_TIMEOUT_MS = 4000;
    private static final int WALK_TIMEOUT_MS = 12000;

    @Inject
    private BankService bankService;

    @Inject
    private MovementService movementService;

    @Inject
    private LocalPathfinder localPathfinder;

    @Override
    public TestRequirements requirements() {
        // Deliberately minimal. This test validates the primitives the precondition engine itself is
        // built from, so it must not depend on that engine doing anything beyond putting it at a bank.
        return TestRequirements.builder()
                .facility(Facility.BANK_BOOTH)
                .build();
    }

    @Override
    protected boolean runTest(Context ctx) throws Exception {
        // Ordered cheapest-first so the earliest broken primitive is the one reported. Each stage
        // assumes the stages above it succeeded.
        if (!checkPlayerState(ctx)) return false;
        if (!checkObjectQuery(ctx)) return false;
        if (!checkBankInteraction(ctx)) return false;
        if (!checkBankContainer(ctx)) return false;
        if (!checkBankClose(ctx)) return false;
        if (!checkMovement(ctx)) return false;

        log.info("Self check passed, the harness can drive the client.");
        return true;
    }

    /**
     * Confirms player state is readable from a non-client thread, which every other query relies on.
     *
     * @param ctx the injected API context
     * @return true when the local player and its world location resolve
     */
    private boolean checkPlayerState(Context ctx) {
        if (!assertNotNull(ctx.players().local(), "Self check: local player query returned nothing")) {
            return false;
        }

        WorldPoint location = ctx.players().local().location();
        if (!assertNotNull(location, "Self check: could not read the local player's world location. "
                + "Either the client thread hop timed out or the player location mapping has changed")) {
            return false;
        }

        log.info("Self check: player state readable, standing at {}", location);
        return true;
    }

    /**
     * Confirms the game object and NPC queries can still resolve a bank, filtering by menu action.
     *
     * <p>Filtering on the {@code "Bank"} action rather than an object name is what the precondition
     * setup will do, because it resolves booths, chests and banker NPCs uniformly.</p>
     *
     * @param ctx the injected API context
     * @return true when a bank booth or banker NPC is found nearby
     */
    private boolean checkObjectQuery(Context ctx) {
        GameObjectEntity booth = ctx.gameObjects().withAction("Bank").nearest();
        if (booth != null && booth.isPresent()) {
            log.info("Self check: game object query resolved a bank via the 'Bank' action");
            return true;
        }

        NpcEntity banker = ctx.npcs().withAction("Bank").nearest();
        if (banker != null && banker.isPresent()) {
            log.info("Self check: npc query resolved a banker via the 'Bank' action");
            return true;
        }

        return assertThat(false, "Self check: neither the game object nor the npc query could find "
                + "anything offering a 'Bank' action. Stand next to a bank booth or banker. If you are "
                + "already next to one, object action mapping has regressed");
    }

    /**
     * Confirms interaction dispatch reaches the client by opening the bank.
     *
     * @param ctx the injected API context
     * @return true when the bank interface opens within the timeout
     */
    private boolean checkBankInteraction(Context ctx) {
        if (bankService.isOpen()) {
            log.info("Self check: bank was already open, skipping the open interaction");
            return true;
        }

        if (!openBank(ctx)) {
            return assertThat(false, "Self check: clicked a bank but the interface never opened. "
                    + "Either interaction dispatch has regressed or a bank pin is blocking the interface");
        }

        log.info("Self check: interaction dispatch opened the bank");
        return true;
    }

    /**
     * Clicks the nearest bank booth, falling back to a banker NPC, and waits for the interface.
     *
     * @param ctx the injected API context
     * @return true when the bank interface is open
     */
    private boolean openBank(Context ctx) {
        GameObjectEntity booth = ctx.gameObjects().withAction("Bank").nearest();
        if (booth != null && booth.isPresent()) {
            booth.interact("Bank");
        } else {
            NpcEntity banker = ctx.npcs().withAction("Bank").nearest();
            if (banker == null || banker.isNull()) {
                return false;
            }
            banker.interact("Bank");
        }

        return SleepService.sleepUntilTrue(bankService::isOpen, INTERFACE_TIMEOUT_MS);
    }

    /**
     * Confirms the bank and inventory containers read correctly and that a withdrawal takes effect.
     *
     * <p>The first bank item is used rather than a named one so the check does not depend on any
     * particular bank stock. The item is deposited straight back so the test leaves no trace.</p>
     *
     * @param ctx the injected API context
     * @return true when an item round-trips from bank to inventory and back
     */
    private boolean checkBankContainer(Context ctx) {
        BankEntity item = ctx.bank().first();
        if (item == null || item.isNull()) {
            return assertThat(false, "Self check: the bank is open but the bank query returned no items. "
                    + "Either your bank is empty or the bank container mapping has regressed");
        }

        if (ctx.inventory().isFull()) {
            return assertThat(false, "Self check: no free inventory slot to withdraw into. "
                    + "Free a slot and re-run");
        }

        final int itemId = item.getId();
        final String itemName = item.getName();
        final long before = ctx.inventory().withId(itemId).count();

        if (!item.withdrawOne()) {
            return assertThat(false, "Self check: withdrawOne on '" + itemName + "' was rejected");
        }

        if (!SleepService.sleepUntilTrue(() -> ctx.inventory().withId(itemId).count() > before, CONTAINER_TIMEOUT_MS)) {
            return assertThat(false, "Self check: withdrew '" + itemName + "' but the inventory count "
                    + "never rose. Either the withdraw action or the inventory container mapping has regressed");
        }

        log.info("Self check: withdrew '{}' and observed the inventory update", itemName);

        // Put it straight back. bankInventory() is the correct container view while the bank is open.
        if (!ctx.bankInventory().withId(itemId).first().depositAll()) {
            return assertThat(false, "Self check: could not deposit '" + itemName + "' back into the bank");
        }

        if (!SleepService.sleepUntilTrue(() -> ctx.inventory().withId(itemId).count() <= before, CONTAINER_TIMEOUT_MS)) {
            return assertThat(false, "Self check: deposited '" + itemName + "' back but the inventory "
                    + "count never returned to its starting value");
        }

        log.info("Self check: deposited '{}' back, bank container round trip works", itemName);
        return true;
    }

    /**
     * Confirms the bank interface can be closed again, which setup relies on between tests.
     *
     * @param ctx the injected API context
     * @return true when the bank interface closes within the timeout
     */
    private boolean checkBankClose(Context ctx) {
        if (!bankService.close()) {
            return assertThat(false, "Self check: the bank close action was rejected");
        }

        if (!SleepService.sleepUntilTrue(bankService::isClosed, INTERFACE_TIMEOUT_MS)) {
            return assertThat(false, "Self check: closed the bank but the interface stayed open");
        }

        log.info("Self check: bank closed");
        return true;
    }

    /**
     * Confirms the player can be walked a short distance, the primitive all travel is built on.
     *
     * <p>A destination is chosen from tiles the local pathfinder already reports as reachable, so a
     * failure here means movement is broken rather than that the chosen tile was unwalkable.</p>
     *
     * @param ctx the injected API context
     * @return true when the player reaches the chosen tile
     */
    private boolean checkMovement(Context ctx) {
        WorldPoint start = ctx.players().local().location();
        if (start == null) {
            return assertThat(false, "Self check: lost the player's location before the movement check");
        }

        List<WorldPoint> reachable = localPathfinder.reachableTiles(start);
        if (reachable == null || reachable.isEmpty()) {
            return assertThat(false, "Self check: the local pathfinder reported no reachable tiles at all. "
                    + "Scene collision data is not being read correctly");
        }

        WorldPoint target = reachable.stream()
                .filter(tile -> tile.distanceTo(start) == 1)
                .findFirst()
                .orElse(null);

        if (target == null) {
            return assertThat(false, "Self check: no reachable tiles found to walk to");
        }

        movementService.moveTo(target);

        if (!SleepService.sleepUntilTrue(() -> {
            WorldPoint now = ctx.players().local().location();
            return now != null && now.distanceTo(target) <= 1;
        }, WALK_TIMEOUT_MS)) {
            return assertThat(false, "Self check: asked to walk from " + start + " to " + target
                    + " but never arrived. Movement packets or the walk action have regressed");
        }

        log.info("Self check: walked from {} to {}", start, target);
        return true;
    }

    @Override
    public String getTestName() {
        return "Self Check";
    }
}
