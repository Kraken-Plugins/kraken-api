package plugins.api.tests.interaction;

import com.google.inject.Singleton;
import com.kraken.api.Context;
import com.kraken.api.query.container.inventory.InventoryEntity;
import com.kraken.api.query.gameobject.GameObjectEntity;
import com.kraken.api.service.util.SleepService;
import lombok.extern.slf4j.Slf4j;
import plugins.api.requirements.ItemRequirement;
import plugins.api.requirements.SideEffect;
import plugins.api.requirements.TestRequirements;
import plugins.api.tests.BaseApiTest;
import plugins.api.world.Facility;
import plugins.api.world.NamedLocation;

/**
 * Exercises using an inventory item on a game object, by filling a bucket at a fountain.
 *
 * <p>A fountain is used because the result is observable: an empty bucket becomes a bucket of water,
 * so the test can assert the interaction actually landed rather than merely that it was dispatched.
 * The previous version called {@code useOn} and returned {@code true} unconditionally, which meant it
 * passed even when the interaction silently failed.</p>
 *
 * <p><b>Requires:</b> an empty bucket in the inventory and a fountain nearby. The Varrock Square
 * fountain, a few tiles from Varrock East Bank, is the intended one.</p>
 */
@Slf4j
@Singleton
public class WidgetTargetGameObjectTest extends BaseApiTest {

    private static final int FOUNTAIN = 7143;

    private static final String BUCKET = "Bucket";
    private static final String BUCKET_OF_WATER = "Bucket of water";

    private static final int FILL_TIMEOUT_MS = 12000;

    @Override
    public TestRequirements requirements() {
        // Runs at the fountain but collects its bucket from the hub bank beforehand.
        return TestRequirements.builder()
                .facility(Facility.WATER_SOURCE)
                .stagingLocation(NamedLocation.VARROCK_WEST_BANK)
                .inventoryItem(ItemRequirement.of(BUCKET))
                .sideEffect(SideEffect.CONSUMES_ITEMS)
                .build();
    }

    @Override
    protected boolean runTest(Context ctx) throws Exception {
        InventoryEntity bucket = ctx.inventory().withName(BUCKET).first();
        // first() returns a wrapper rather than null when nothing matches, so isPresent is the
        // correct emptiness check here; a plain null check silently passes on an empty result.
        if (!assertTrue(bucket != null && bucket.isPresent(),
                "Widget target on object test: no empty bucket in the inventory")) {
            return false;
        }

        GameObjectEntity fountain = ctx.gameObjects().withId(FOUNTAIN).nearest();
        if (!assertTrue(fountain != null && fountain.isPresent(),
                "Widget target on object test: no fountain (object " + FOUNTAIN + ") nearby")) {
            return false;
        }

        final long filledBefore = ctx.inventory().withName(BUCKET_OF_WATER).count();

        if (!assertTrue(bucket.useOn(fountain.raw()),
                "Widget target on object test: using the bucket on the fountain was rejected")) {
            return false;
        }

        return assertTrue(
                SleepService.sleepUntilTrue(
                        () -> ctx.inventory().withName(BUCKET_OF_WATER).count() > filledBefore,
                        FILL_TIMEOUT_MS),
                "Widget target on object test: used the bucket on the fountain but no bucket of water "
                        + "appeared. The item on object interaction did not reach the client");
    }

    @Override
    public String getTestName() {
        return "Widget Object";
    }
}
