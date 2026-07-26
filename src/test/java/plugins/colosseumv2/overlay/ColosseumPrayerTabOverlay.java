package plugins.colosseumv2.overlay;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kraken.api.service.prayer.InteractablePrayer;
import net.runelite.api.Client;
import net.runelite.api.Prayer;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import plugins.colosseumv2.AutoColosseumPrayersV2Config;
import plugins.colosseumv2.AutoColosseumPrayersV2Plugin;
import plugins.colosseumv2.engine.QueuedAttack;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.HashSet;
import java.util.Set;

/**
 * Renders descending boxes above the protection prayer widgets: each box is one queued attack,
 * sliding down one row per tick and reaching the prayer icon on the tick the prayer must be
 * active. Boxes are colored by protection style.
 */
@Singleton
public class ColosseumPrayerTabOverlay extends Overlay {

    private static final int BOX_WIDTH = 10;
    private static final int BOX_HEIGHT = 5;
    private static final int TICK_PIXEL_SIZE = 8;

    private static final Color MAGE = new Color(60, 160, 255, 220);
    private static final Color RANGE = new Color(60, 200, 80, 220);
    private static final Color MELEE = new Color(255, 90, 80, 220);
    private static final Color TARGET_OUTLINE = new Color(255, 255, 255, 200);

    private final AutoColosseumPrayersV2Plugin plugin;
    private final AutoColosseumPrayersV2Config config;
    private final Client client;

    @Inject
    public ColosseumPrayerTabOverlay(AutoColosseumPrayersV2Plugin plugin, AutoColosseumPrayersV2Config config, Client client) {
        this.plugin = plugin;
        this.config = config;
        this.client = client;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        if (!config.showPrayerQueueOnPrayerTab() || !plugin.getColosseumState().isInColosseum()) {
            return null;
        }

        int tick = plugin.getEngine().getCurrentTick();
        if (tick < 0) {
            return null;
        }

        Set<Long> rendered = new HashSet<>();
        for (QueuedAttack attack : plugin.getEngine().getQueueView()) {
            Prayer prayer = attack.getPrayer();
            if (prayer == null) {
                continue;
            }

            // 0 = the prayer must be active for the very next tick (its box touches the icon).
            int ticksRemaining = attack.getLandTick() - tick - 1;
            if (ticksRemaining < 0 || ticksRemaining > config.prayerTabLookaheadTicks()) {
                continue;
            }

            long key = ((long) prayer.ordinal() << 32) | ticksRemaining;
            if (!rendered.add(key)) {
                continue;
            }

            renderDescendingBox(graphics, prayer, ticksRemaining);
        }

        Prayer target = plugin.getActiveTargetPrayer();
        if (target != null) {
            Widget widget = widgetForPrayer(target);
            if (widget != null && !widget.isHidden()) {
                graphics.setColor(TARGET_OUTLINE);
                Rectangle bounds = widget.getBounds();
                graphics.drawRect(bounds.x, bounds.y, bounds.width, bounds.height);
            }
        }

        return null;
    }

    private void renderDescendingBox(Graphics2D graphics, Prayer prayer, int tick) {
        Widget prayerWidget = widgetForPrayer(prayer);
        if (prayerWidget == null || prayerWidget.isHidden()) {
            return;
        }

        int baseX = (int) prayerWidget.getBounds().getX();
        baseX += prayerWidget.getBounds().getWidth() / 2;
        baseX -= BOX_WIDTH / 2;

        int baseY = (int) prayerWidget.getBounds().getY() - tick * TICK_PIXEL_SIZE - BOX_HEIGHT;
        baseY += TICK_PIXEL_SIZE - ((plugin.getLastTickTime() + 600 - System.currentTimeMillis()) / 600.0 * TICK_PIXEL_SIZE);

        Rectangle boxRectangle = new Rectangle(BOX_WIDTH, BOX_HEIGHT);
        boxRectangle.translate(baseX, baseY);

        Color color = colorForPrayer(prayer);
        graphics.setColor(color);
        graphics.fill(boxRectangle);
        graphics.setColor(color.darker().darker());
        graphics.draw(boxRectangle);
    }

    private Widget widgetForPrayer(Prayer prayer) {
        InteractablePrayer interactable = InteractablePrayer.of(prayer);
        if (interactable == null) {
            return null;
        }
        return client.getWidget(interactable.getIndex());
    }

    private Color colorForPrayer(Prayer prayer) {
        switch (prayer) {
            case PROTECT_FROM_MAGIC:
                return MAGE;
            case PROTECT_FROM_MISSILES:
                return RANGE;
            case PROTECT_FROM_MELEE:
                return MELEE;
            default:
                return Color.WHITE;
        }
    }
}
