package plugins.api.tests.interaction;

import com.google.inject.Singleton;
import com.kraken.api.Context;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.widgets.Widget;
import plugins.api.tests.BaseApiTest;

@Slf4j
@Singleton
public class WidgetActionTest extends BaseApiTest {
    private static final int WIDGET_SPECIAL_ATTACK_ORB = 10485796;

    @Override
    protected boolean runTest(Context ctx) throws Exception {
        Widget w = ctx.runOnClientThread(() -> ctx.getClient().getWidget(WIDGET_SPECIAL_ATTACK_ORB));

        if(w == null || w.isHidden()) return false;

        // Spec should be disabled at the start
        if(ctx.players().local().isSpecEnabled()) return false;

        ctx.getInteractionManager().interact(w, "Use");
        Thread.sleep(600);

        return ctx.players().local().isSpecEnabled();
    }

    @Override
    protected String getTestName() {
        return "Widget Action";
    }
}

