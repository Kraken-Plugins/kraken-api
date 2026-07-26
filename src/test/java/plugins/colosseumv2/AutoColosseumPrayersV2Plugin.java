package plugins.colosseumv2;

import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.kraken.api.Context;
import com.kraken.api.service.prayer.PrayerService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Prayer;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import plugins.colosseum.AutoColosseumPrayersConfig;
import plugins.colosseum.model.ColosseumStateChanged;

import java.util.Map;

@Slf4j
@Singleton
@PluginDescriptor(
        name = "Auto Colo Prayers V2",
        description = "Automatically prays for you in the Colosseum.",
        tags = {"auto", "prayers", "colo", "colosseum"}
)
public class AutoColosseumPrayersV2Plugin extends Plugin {
    private static final String CONFIG_GROUP = "autocoloprayersv2";

    private static final int MANTICORE_MAGE_GRAPHIC = 2681;
    private static final int MANTICORE_RANGE_GRAPHIC = 2683;
    private static final int MANTICORE_MELEE_GRAPHIC = 2685;
    private static final int MANTICORE_VOLLEY_SIZE = 3;

    private static final Map<Integer, Prayer> WAVE_PRE_PRAYER_MAP = Map.ofEntries(
            Map.entry(1, Prayer.PROTECT_FROM_MAGIC),
            Map.entry(2, Prayer.PROTECT_FROM_MISSILES),
            Map.entry(3, Prayer.PROTECT_FROM_MISSILES),
            Map.entry(4, Prayer.PROTECT_FROM_MAGIC),
            Map.entry(5, Prayer.PROTECT_FROM_MISSILES),
            Map.entry(6, Prayer.PROTECT_FROM_MISSILES),
            Map.entry(7, Prayer.PROTECT_FROM_MAGIC),
            Map.entry(8, Prayer.PROTECT_FROM_MAGIC),
            Map.entry(9, Prayer.PROTECT_FROM_MISSILES),
            Map.entry(10, Prayer.PROTECT_FROM_MISSILES),
            Map.entry(11, Prayer.PROTECT_FROM_MAGIC)
    );

    @Inject
    private Context ctx;

    @Inject
    private Client client;

    @Inject
    private PrayerService prayerService;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private AutoColosseumPrayersV2Config config;

    @Getter
    private long lastTickTime = -1;

    @Getter
    private Prayer activeTargetPrayer;

    @Getter
    private Prayer prePrayPrayer;

    @Getter
    private boolean oneTickFlickEnabled;

    @Getter
    private boolean runtimeEnabled;

    @Provides
    AutoColosseumPrayersConfig provideConfig(final ConfigManager configManager) {
        return configManager.getConfig(AutoColosseumPrayersConfig.class);
    }

    @Override
    protected void startUp() {
    }

    @Override
    protected void shutDown() {

    }

    @Subscribe
    private void onColosseumStateChanged(ColosseumStateChanged event) {

    }
}
