package plugins.colosseumv2.overlay;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import plugins.colosseumv2.AutoColosseumPrayersV2Config;
import plugins.colosseumv2.AutoColosseumPrayersV2Plugin;
import plugins.colosseumv2.engine.NpcDebugInfo;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Stroke;
import java.util.Map;

/**
 * Scene debug rendering: per-NPC line of sight tile fields, predicted paths toward the player,
 * and diagnostic labels. All data comes from the engine's once-per-tick debug caches; nothing
 * is computed per frame.
 */
@Singleton
public class ColosseumSceneOverlay extends Overlay {

    private static final Color[] PALETTE = {
            new Color(0, 188, 212),
            new Color(255, 193, 7),
            new Color(233, 30, 99),
            new Color(139, 195, 74),
            new Color(171, 71, 188),
            new Color(255, 112, 67),
            new Color(3, 169, 244),
            new Color(255, 235, 59),
    };

    private static final Stroke THIN = new BasicStroke(1);
    private static final Stroke THICK = new BasicStroke(2);

    private final AutoColosseumPrayersV2Plugin plugin;
    private final AutoColosseumPrayersV2Config config;
    private final Client client;

    @Inject
    public ColosseumSceneOverlay(AutoColosseumPrayersV2Plugin plugin, AutoColosseumPrayersV2Config config, Client client) {
        this.plugin = plugin;
        this.config = config;
        this.client = client;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        boolean anyEnabled = config.showNpcLineOfSightDebug()
                || config.showNpcPathingDebug()
                || config.showNpcDebugLabels();
        if (!anyEnabled || !plugin.getColosseumState().isInColosseum()) {
            return null;
        }

        Map<Integer, NpcDebugInfo> debugInfo = plugin.getEngine().getDebugInfo();
        if (debugInfo.isEmpty()) {
            return null;
        }

        WorldView worldView = client.getTopLevelWorldView();
        if (worldView == null) {
            return null;
        }

        int paletteIndex = 0;
        for (NpcDebugInfo info : debugInfo.values()) {
            Color color = PALETTE[paletteIndex++ % PALETTE.length];

            if (config.showNpcLineOfSightDebug() && info.getLosTiles() != null) {
                Color fill = new Color(color.getRed(), color.getGreen(), color.getBlue(), 35);
                graphics.setStroke(THIN);
                for (int[] tile : info.getLosTiles()) {
                    Polygon poly = tilePoly(worldView, tile[0], tile[1]);
                    if (poly != null) {
                        graphics.setColor(fill);
                        graphics.fill(poly);
                    }
                }
            }

            if (config.showNpcPathingDebug() && info.getPath() != null && !info.getPath().isEmpty()) {
                graphics.setStroke(THICK);
                Color outline = new Color(color.getRed(), color.getGreen(), color.getBlue(), 180);
                int step = 1;
                for (int[] tile : info.getPath()) {
                    Polygon poly = tilePoly(worldView, tile[0], tile[1]);
                    if (poly != null) {
                        graphics.setColor(outline);
                        graphics.draw(poly);
                        Point text = Perspective.getCanvasTextLocation(
                                client, graphics, LocalPoint.fromScene(tile[0], tile[1], worldView), String.valueOf(step), 0);
                        if (text != null) {
                            graphics.drawString(String.valueOf(step), text.getX(), text.getY());
                        }
                    }
                    step++;
                }
            }

            // Footprint outline: red when the NPC can currently attack the player.
            graphics.setStroke(THICK);
            Color footprint = info.isLineOfSight()
                    ? new Color(244, 67, 54, 220)
                    : new Color(color.getRed(), color.getGreen(), color.getBlue(), 220);
            for (int dx = 0; dx < info.getSize(); dx++) {
                for (int dy = 0; dy < info.getSize(); dy++) {
                    Polygon poly = tilePoly(worldView, info.getSceneX() + dx, info.getSceneY() + dy);
                    if (poly != null) {
                        graphics.setColor(footprint);
                        graphics.draw(poly);
                    }
                }
            }

            if (config.showNpcDebugLabels()) {
                renderLabel(graphics, worldView, info, color);
            }
        }

        return null;
    }

    private void renderLabel(Graphics2D graphics, WorldView worldView, NpcDebugInfo info, Color color) {
        LocalPoint sw = LocalPoint.fromScene(info.getSceneX(), info.getSceneY(), worldView);
        if (sw == null) {
            return;
        }

        int offset = (info.getSize() - 1) * Perspective.LOCAL_TILE_SIZE / 2;
        LocalPoint center = new LocalPoint(sw.getX() + offset, sw.getY() + offset, worldView);

        StringBuilder label = new StringBuilder(info.getName());
        label.append(info.isLineOfSight() ? " [LoS]" : " [--]");
        if (info.isStuck()) {
            label.append(" [stuck]");
        }
        if (info.getReadyIn() > 0) {
            label.append(" rdy ").append(info.getReadyIn()).append('t');
        }
        int tick = plugin.getEngine().getCurrentTick();
        if (info.getNextAttackTick() > tick) {
            label.append(" atk +").append(info.getNextAttackTick() - tick).append('t');
        } else if (info.getPathAttackTick() > tick) {
            label.append(" eta +").append(info.getPathAttackTick() - tick).append('t');
        }

        Point location = Perspective.getCanvasTextLocation(client, graphics, center, label.toString(), 220);
        if (location != null) {
            graphics.setColor(Color.BLACK);
            graphics.drawString(label.toString(), location.getX() + 1, location.getY() + 1);
            graphics.setColor(color);
            graphics.drawString(label.toString(), location.getX(), location.getY());
        }

        if (info.getManticorePhase() != null) {
            Point phaseLocation = Perspective.getCanvasTextLocation(client, graphics, center, info.getManticorePhase(), 200);
            if (phaseLocation != null) {
                graphics.setColor(Color.BLACK);
                graphics.drawString(info.getManticorePhase(), phaseLocation.getX() + 1, phaseLocation.getY() + 1);
                graphics.setColor(new Color(255, 152, 0));
                graphics.drawString(info.getManticorePhase(), phaseLocation.getX(), phaseLocation.getY());
            }
        }
    }

    private Polygon tilePoly(WorldView worldView, int sceneX, int sceneY) {
        LocalPoint local = LocalPoint.fromScene(sceneX, sceneY, worldView);
        if (local == null) {
            return null;
        }
        return Perspective.getCanvasTilePoly(client, local);
    }
}
