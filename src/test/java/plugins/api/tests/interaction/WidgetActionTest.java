package plugins.api.tests.interaction;

import com.google.inject.Singleton;
import com.kraken.api.Context;
import com.kraken.api.service.util.SleepService;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.widgets.Widget;
import plugins.api.requirements.SideEffect;
import plugins.api.requirements.TestRequirements;
import plugins.api.tests.BaseApiTest;

@Slf4j
@Singleton
public class WidgetActionTest extends BaseApiTest {
    // Using run over spec to keep things F2P
    // private static final int WIDGET_SPECIAL_ATTACK_ORB = 10485796;
    private static final int WIDGET_RUN_ENERGY = 10485788;

    @Override
    public TestRequirements requirements() {
        return TestRequirements.builder()
                .sideEffect(SideEffect.TOGGLES_RUN)
                .build();
    }

    @Override
    protected boolean runTest(Context ctx) throws Exception {
        Widget w = ctx.runOnClientThread(() -> ctx.getClient().getWidget(WIDGET_RUN_ENERGY));

        if(w == null) return false;

        boolean runEnabled = ctx.players().local().isRunEnabled();

        ctx.runOnClientThread(() -> ctx.getInteractionManager().interact(w, "Toggle Run"));
        SleepService.sleepFor(1);

        return runEnabled != ctx.players().local().isRunEnabled();
    }

    @Override
    public String getTestName() {
        return "Widget Action";
    }
}

