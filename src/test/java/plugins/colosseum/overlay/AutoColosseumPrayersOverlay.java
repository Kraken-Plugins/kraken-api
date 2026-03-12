package plugins.colosseum.overlay;

import com.google.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Prayer;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;
import plugins.colosseum.AutoColosseumPrayersConfig;
import plugins.colosseum.AutoColosseumPrayersPlugin;
import plugins.colosseum.ColosseumStateTracker;
import plugins.colosseum.model.PrayerQueueEntry;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;

public class AutoColosseumPrayersOverlay extends OverlayPanel {
    private final AutoColosseumPrayersPlugin plugin;
    private final AutoColosseumPrayersConfig config;
    private final ColosseumStateTracker tracker;
    private final Client client;

    @Inject
    public AutoColosseumPrayersOverlay(AutoColosseumPrayersPlugin plugin, AutoColosseumPrayersConfig config, ColosseumStateTracker tracker, Client client) {
        this.plugin = plugin;
        this.config = config;
        this.tracker = tracker;
        this.client = client;
        setPosition(OverlayPosition.TOP_RIGHT);
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        panelComponent.getChildren().clear();

        panelComponent.getChildren().add(
                TitleComponent.builder()
                        .text("Auto Colo Prayers")
                        .color(Color.CYAN)
                        .build()
        );

        boolean runtimeEnabled = config.enabled() && plugin.isRuntimeEnabled();
        panelComponent.getChildren().add(
                LineComponent.builder()
                        .left("Prayers")
                        .right(runtimeEnabled ? "ON" : "OFF")
                        .rightColor(runtimeEnabled ? Color.GREEN : Color.RED)
                        .build()
        );

        panelComponent.getChildren().add(
                LineComponent.builder()
                        .left("One Tick")
                        .right(plugin.isOneTickFlickEnabled() ? "ON" : "OFF")
                        .rightColor(plugin.isOneTickFlickEnabled() ? Color.GREEN : Color.RED)
                        .build()
        );

        if (config.showQueueOverlay()) {
            List<PrayerQueueEntry> queueEntries = new ArrayList<>(plugin.getPrayerQueue());
            int maxRows = Math.max(1, config.queueOverlayLines());
            int currentTick = client.getTickCount();
            int rows = 0;

            for (PrayerQueueEntry entry : queueEntries) {
                if (rows >= maxRows) {
                    break;
                }

                int ticksAway = entry.getTick() - currentTick;
                if (ticksAway < 0) {
                    continue;
                }

                panelComponent.getChildren().add(
                        LineComponent.builder()
                                .left("+" + ticksAway + " " + shortPrayerName(entry.getPrayer()))
                                .right(entry.getMob().name())
                                .rightColor(entry.isJaguarPriority() ? Color.RED : Color.LIGHT_GRAY)
                                .build()
                );
                rows++;
            }

            if (rows == 0) {
                panelComponent.getChildren().add(
                        LineComponent.builder()
                                .left("Queue")
                                .right("Empty")
                                .rightColor(Color.GRAY)
                                .build()
                );
            }
        }

        if (config.expandDebugOverlay()) {
            panelComponent.getChildren().add(
                    LineComponent.builder()
                            .left("Area")
                            .right(areaText())
                            .rightColor(tracker.getCurrentState().isInColosseum() ? Color.GREEN : Color.GRAY)
                            .build()
            );

            panelComponent.getChildren().add(
                    LineComponent.builder()
                            .left("Wave")
                            .right(String.valueOf(tracker.getCurrentState().getWaveNumber()))
                            .rightColor(Color.WHITE)
                            .build()
            );

            panelComponent.getChildren().add(
                    LineComponent.builder()
                            .left("Wave Started")
                            .right(tracker.getCurrentState().isWaveStarted() ? "Yes" : "No")
                            .rightColor(tracker.getCurrentState().isWaveStarted() ? Color.GREEN : Color.GRAY)
                            .build()
            );

            panelComponent.getChildren().add(
                    LineComponent.builder()
                            .left("Wave Start Tick")
                            .right(String.valueOf(tracker.getWaveStartTick()))
                            .rightColor(Color.WHITE)
                            .build()
            );

            panelComponent.getChildren().add(
                    LineComponent.builder()
                            .left("Client Tick")
                            .right(String.valueOf(client.getTickCount()))
                            .rightColor(Color.WHITE)
                            .build()
            );

            panelComponent.getChildren().add(
                    LineComponent.builder()
                            .left("Tracked NPCs")
                            .right(String.valueOf(plugin.getTrackedMobStates().size()))
                            .rightColor(Color.WHITE)
                            .build()
            );

            panelComponent.getChildren().add(
                    LineComponent.builder()
                            .left("Queue Size")
                            .right(String.valueOf(plugin.getPrayerQueue().size()))
                            .rightColor(Color.WHITE)
                            .build()
            );
        }

        return super.render(graphics);
    }

    private String areaText() {
        if (tracker.getCurrentState().isInLobby()) {
            return "Lobby";
        }
        if (tracker.getCurrentState().isInColosseum()) {
            return "Arena";
        }

        return "Outside";
    }

    private String shortPrayerName(Prayer prayer) {
        if (prayer == Prayer.PROTECT_FROM_MAGIC) {
            return "Mage";
        }
        if (prayer == Prayer.PROTECT_FROM_MISSILES) {
            return "Range";
        }
        if (prayer == Prayer.PROTECT_FROM_MELEE) {
            return "Melee";
        }
        return prayer.name();
    }
}
