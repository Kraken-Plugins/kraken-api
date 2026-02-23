package plugins.colosseum;

import com.google.inject.Inject;
import net.runelite.api.Prayer;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.List;

public class AutoColosseumPrayersOverlay extends OverlayPanel {
    private final AutoColosseumPrayersPlugin plugin;
    private final AutoColosseumPrayersConfig config;

    @Inject
    public AutoColosseumPrayersOverlay(AutoColosseumPrayersPlugin plugin, AutoColosseumPrayersConfig config) {
        this.plugin = plugin;
        this.config = config;
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

        boolean runtimeEnabled = plugin.isRuntimeEnabled();
        panelComponent.getChildren().add(
                LineComponent.builder()
                        .left("State")
                        .right(runtimeEnabled ? "ON" : "OFF")
                        .rightColor(runtimeEnabled ? Color.GREEN : Color.RED)
                        .build()
        );

        Prayer activeTarget = plugin.getActiveTargetPrayer();
        panelComponent.getChildren().add(
                LineComponent.builder()
                        .left("Target")
                        .right(activeTarget == null ? "-" : shortPrayerName(activeTarget))
                        .rightColor(activeTarget == null ? Color.GRAY : Color.WHITE)
                        .build()
        );

        Prayer activeOverhead = plugin.getActiveProtectionPrayer();
        panelComponent.getChildren().add(
                LineComponent.builder()
                        .left("Overhead")
                        .right(activeOverhead == null ? "Off" : shortPrayerName(activeOverhead))
                        .rightColor(activeOverhead == null ? Color.GRAY : Color.GREEN)
                        .build()
        );

        Prayer prePray = plugin.getPrePrayPrayer();
        int prePrayTicks = plugin.getRemainingPrePrayTicks();
        if (prePray != null && prePrayTicks > 0) {
            panelComponent.getChildren().add(
                    LineComponent.builder()
                            .left("Pre-Pray")
                            .right(shortPrayerName(prePray) + " (+" + prePrayTicks + ")")
                            .rightColor(new Color(255, 195, 0))
                            .build()
            );
        }

        if (config.showQueueOverlay()) {
            List<PrayerQueueEntry> queueEntries = plugin.getPrayerQueueSnapshot();
            int maxRows = Math.max(1, config.queueOverlayLines());
            int currentTick = plugin.getCurrentTick();
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

        return super.render(graphics);
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
