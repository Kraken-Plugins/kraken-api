package plugins.api.tests.interaction;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kraken.api.Context;
import com.kraken.api.query.container.inventory.InventoryEntity;
import com.kraken.api.service.util.SleepService;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.coords.WorldPoint;
import plugins.api.tests.BaseApiTest;
import plugins.api.requirements.ItemRequirement;
import plugins.api.requirements.SideEffect;
import plugins.api.requirements.TestRequirements;

/**
 * Exercises a nested widget sub action: an item menu option that resolves to a second level choice.
 *
 * <p>Rubbing a ring of dueling offers several destinations, so it is a clean case of "pick option X
 * of action Y" rather than a plain single action click.</p>
 *
 * <p>The destination is Emir's Arena rather than Fortis Colosseum. Fortis Colosseum is members only,
 * which made the whole suite require membership for one interaction check, and it stranded the player
 * a very long way from where the rest of the suite runs. Emir's Arena is free to play and in Al Kharid,
 * a short trip from Varrock.</p>
 *
 * <p><b>Requires:</b> a ring of dueling in the inventory. <b>Teleports the player</b>, so it belongs
 * late in any sequenced run.</p>
 */
@Slf4j
@Singleton
public class WidgetSubActionTest extends BaseApiTest {

    private static final String RING_OF_DUELING = "Ring of dueling";
    private static final String RUB = "Rub";
    private static final String DESTINATION = "Emir's Arena";

    /** Approximate centre of the Emir's Arena teleport arrival area. */
    private static final WorldPoint EMIRS_ARENA = new WorldPoint(3315, 3235, 0);

    private static final int ARRIVAL_RADIUS = 25;
    private static final int TELEPORT_TIMEOUT_MS = 12000;

    @Inject
    private Context context;

    @Override
    public TestRequirements requirements() {
        // Teleports to Al Kharid, so it is ordered last within its group and the runner walks or
        // teleports back afterwards.
        return TestRequirements.builder()
                .inventoryItem(ItemRequirement.of(RING_OF_DUELING))
                .sideEffect(SideEffect.TELEPORTS)
                .sideEffect(SideEffect.CONSUMES_ITEMS)
                .orderHint(100)
                .build();
    }

    @Override
    protected boolean runTest(Context ctx) throws Exception {
        InventoryEntity ringOfDueling = ctx.inventory().nameContains(RING_OF_DUELING).first();
        if (!assertTrue(ringOfDueling != null && ringOfDueling.isPresent(),
                "Widget sub action test: no ring of dueling in the inventory")) {
            return false;
        }

        WorldPoint before = ctx.players().local().location();
        if (!assertNotNull(before, "Widget sub action test: could not read the player's location")) {
            return false;
        }

        if (before.distanceTo(EMIRS_ARENA) <= ARRIVAL_RADIUS) {
            return assertThat(false, "Widget sub action test: already at Emir's Arena, so a successful "
                    + "teleport would be indistinguishable from doing nothing. Move away and re-run");
        }

        context.getInteractionManager().interact(ringOfDueling.raw().getWidget(), RUB, DESTINATION);

        return assertTrue(
                SleepService.sleepUntilTrue(() -> {
                    WorldPoint now = ctx.players().local().location();
                    return now != null && now.distanceTo(EMIRS_ARENA) <= ARRIVAL_RADIUS;
                }, TELEPORT_TIMEOUT_MS),
                "Widget sub action test: rubbed the ring and chose '" + DESTINATION + "' but never "
                        + "arrived. The nested sub action did not resolve to a menu entry");
    }

    @Override
    public String getTestName() {
        return "Widget Sub";
    }
}
