package plugins.colosseum.overlay;

import com.google.inject.Inject;
import com.kraken.api.service.prayer.InteractablePrayer;
import net.runelite.api.Client;
import net.runelite.api.Prayer;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import plugins.colosseum.AutoColosseumPrayersConfig;
import plugins.colosseum.AutoColosseumPrayersPlugin;
import plugins.colosseum.model.PrayerQueueEntry;

import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AutoColosseumPrayerQueueOverlay extends Overlay {
    private static final int BOX_WIDTH = 20;
    private static final int BOX_HEIGHT = 9;
    private static final int TICK_PIXEL_SIZE = 16;

    private final Client client;
    private final AutoColosseumPrayersPlugin plugin;
    private final AutoColosseumPrayersConfig config;

    @Inject
    public AutoColosseumPrayerQueueOverlay(
            Client client,
            AutoColosseumPrayersPlugin plugin,
            AutoColosseumPrayersConfig config
    ) {
        this.client = client;
        this.plugin = plugin;
        this.config = config;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
        setPriority(OverlayPriority.HIGH);
    }

    @Override
    public Dimension render(Graphics2D graphics2D) {
        if (!config.showPrayerQueueOnPrayerTab()) {
            return null;
        }

        List<PrayerQueueEntry> queueEntries = new ArrayList<>(plugin.getPrayerQueue());
        if (queueEntries.isEmpty()) {
            return null;
        }

        int currentTick = client.getTickCount();
        int lookahead = Math.max(1, config.prayerTabLookaheadTicks());
        Map<String, Integer> renderedCounts = new LinkedHashMap<>();

        for (PrayerQueueEntry queueEntry : queueEntries) {
            int tick = queueEntry.getTick() - currentTick;
            if (tick < 0 || tick > lookahead) {
                continue;
            }

            String key = queueEntry.getPrayer().name() + ":" + tick;
            renderedCounts.put(key, renderedCounts.getOrDefault(key, 0) + 1);
        }

        for (Map.Entry<String, Integer> entry : renderedCounts.entrySet()) {
            String[] keyParts = entry.getKey().split(":");
            renderDescendingBoxes(
                    graphics2D,
                    Prayer.valueOf(keyParts[0]),
                    Integer.parseInt(keyParts[1]),
                    entry.getValue()
            );
        }

        return null;
    }

    private void renderDescendingBoxes(final Graphics2D graphics2D, final Prayer prayer, final int tick, final int count) {
        final Color color = colorForTick(tick);
        final Widget prayerWidget = widgetForPrayer(client, prayer);

        if (prayerWidget == null || prayerWidget.isHidden()) {
            return;
        }

        int baseX = (int) prayerWidget.getBounds().getX();
        baseX += prayerWidget.getBounds().getWidth() / 2;
        baseX -= BOX_WIDTH / 2;

        int baseY = (int) prayerWidget.getBounds().getY() - tick * TICK_PIXEL_SIZE - BOX_HEIGHT;
        baseY += TICK_PIXEL_SIZE - (int) ((plugin.getLastTickTime() + 600 - System.currentTimeMillis()) / 600.0 * TICK_PIXEL_SIZE);

        final Rectangle boxRectangle = new Rectangle(BOX_WIDTH, BOX_HEIGHT);
        boxRectangle.translate(baseX, baseY);

        renderFilledPolygon(graphics2D, boxRectangle, color);
        if (count > 1) {
            renderCount(graphics2D, boxRectangle, count);
        }
    }

    private Widget widgetForPrayer(Client client, Prayer prayer) {
        InteractablePrayer interactablePrayer = InteractablePrayer.of(prayer);
        if (interactablePrayer == null) {
            return null;
        }
        return client.getWidget(interactablePrayer.getIndex());
    }

    private Color colorForTick(int tick) {
        if (tick <= 1) {
            return new Color(220, 55, 55, 170);
        }
        if (tick == 2) {
            return new Color(255, 132, 0, 160);
        }
        if (tick == 3) {
            return new Color(235, 210, 0, 140);
        }
        return new Color(70, 185, 90, 120);
    }

    private void renderFilledPolygon(Graphics2D graphics, Shape poly, Color color) {
        graphics.setColor(color);
        final Stroke originalStroke = graphics.getStroke();
        graphics.setStroke(new BasicStroke(2));
        graphics.draw(poly);
        graphics.fill(poly);
        graphics.setStroke(originalStroke);
    }

    private void renderCount(Graphics2D graphics, Rectangle boxRectangle, int count) {
        String text = String.valueOf(count);
        FontMetrics metrics = graphics.getFontMetrics();
        int x = boxRectangle.x + (boxRectangle.width - metrics.stringWidth(text)) / 2;
        int y = boxRectangle.y + ((boxRectangle.height - metrics.getHeight()) / 2) + metrics.getAscent();

        graphics.setColor(Color.WHITE);
        graphics.drawString(text, x, y);
    }
}
