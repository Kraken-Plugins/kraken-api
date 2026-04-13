package plugins.api.tests.interaction;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kraken.api.Context;
import com.kraken.api.query.container.inventory.InventoryEntity;
import com.kraken.api.query.gameobject.GameObjectEntity;
import lombok.extern.slf4j.Slf4j;
import plugins.api.tests.BaseApiTest;

@Slf4j
@Singleton
public class WidgetTargetGameObjectTest extends BaseApiTest {

    @Inject
    private Context context;

    @Override
    protected boolean runTest(Context ctx) throws Exception {
        InventoryEntity e = context.inventory().withName("Bucket").first();
        if(e == null) {
            log.error("Widget target on object test failed, could not find a bucket");
            return false;
        }

        GameObjectEntity gameObject = context.gameObjects().withId(5125).nearest();
        if (gameObject == null) {
            log.error("Widget target on object test failed, could not find a fountain game object");
            return false;
        }

        e.useOn(gameObject.raw());
        return true;
    }

    @Override
    protected String getTestName() {
        return "Widget Object";
    }
}
