package plugins.colosseum;

import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.kraken.api.Context;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@Slf4j
@Singleton
@PluginDescriptor(
        name = "Auto Colosseum Prayers",
        description = "Automatically prays for you in the Colosseum.",
        tags = {"auto", "prayers", "colo", "colosseum"}
)
public class AutoColosseumPrayers extends Plugin {

    @Inject
    private Context ctx;

    @Inject
    private AutoColosseumPrayersConfig config;

    private static final int MANTICORE_MAGE_GRAPHIC = 2681; // use npc.hasSpotAnim(MANTICORE_MAGE_GRAPHIC); to know if mage is first while the manticore is charging, same for range/melee
    private static final int MANTICORE_RANGE_GRAPHIC = 2683;
    private static final int MANTICORE_MELEE_GRAPHIC = 2685;

    @Provides
    AutoColosseumPrayersConfig provideConfig(final ConfigManager configManager) {
        return configManager.getConfig(AutoColosseumPrayersConfig.class);
    }

    @Override
    public void startUp() {
        ctx.initializePackets();
    }

    @Override
    public void shutDown() {

    }
}
