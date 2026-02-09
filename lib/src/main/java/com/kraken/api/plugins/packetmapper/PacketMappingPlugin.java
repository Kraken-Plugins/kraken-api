package com.kraken.api.plugins.packetmapper;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

import javax.inject.Inject;
import java.awt.image.BufferedImage;

/**
 * RuneLite plugin for packet mapping tool
 * 
 * Usage:
 * 1. Start the plugin from the plugin list
 * 2. Click the packet mapping icon in the sidebar
 * 3. Click "Start Monitoring"
 * 4. Perform actions in game (click objects, NPCs, items, etc.)
 * 5. Click "Stop Monitoring" when done
 * 6. Select packets from the dropdown to view their mappings
 * 7. Export mappings to a file or copy to clipboard
 * 
 * The tool will correlate your in-game actions with the packets being sent
 * and generate the obfuscated packet mappings automatically.
 */
@Slf4j
@PluginDescriptor(
    name = "Packet Mapper",
    description = "Maps OSRS packet structures by analyzing outgoing packets",
    tags = {"packet", "mapping", "debug", "development"}
)
public class PacketMappingPlugin extends Plugin {

    @Inject
    private ClientToolbar clientToolbar;

    @Inject
    private EventBus eventBus;

    @Inject
    private PacketMappingTool mappingTool;

    @Inject
    private PacketInterceptor interceptor;

    @Inject
    private PacketMappingPanel panel;

    private NavigationButton navButton;

    @Override
    protected void startUp() {
        eventBus.register(mappingTool);
        eventBus.register(interceptor);
        final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "/packet_icon.png");
        
        navButton = NavigationButton.builder()
            .tooltip("Packet Mapper")
            .icon(icon)
            .priority(10)
            .panel(panel)
            .build();

        clientToolbar.addNavigation(navButton);
    }

    @Override
    protected void shutDown() {
        log.info("Packet Mapping Plugin stopped");
        interceptor.stopInterception();
        eventBus.unregister(mappingTool);
        clientToolbar.removeNavigation(navButton);
    }

    @Provides
    PacketMappingConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(PacketMappingConfig.class);
    }

    @Subscribe
    private void onMenuOptionClicked(MenuOptionClicked event) {

    }

    @Subscribe
    private void onPacketSent(PacketSent event) {
        log.info("Packet sent: {}", event.getPacketBuffer());
    }
}
