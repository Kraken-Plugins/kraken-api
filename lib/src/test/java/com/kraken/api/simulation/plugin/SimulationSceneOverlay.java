package com.kraken.api.simulation.plugin;

import com.google.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.*;

import java.awt.*;
import java.util.List;
import java.util.Map;

public class SimulationSceneOverlay extends Overlay {
    private final Client client;
    private final SimulationPlugin plugin;
    private final SimulationPluginConfig config;

    @Inject
    public SimulationSceneOverlay(Client client, SimulationPlugin plugin, SimulationPluginConfig config) {
        this.client = client;
        this.plugin = plugin;
        this.config = config;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
        setPriority(OverlayPriority.HIGH);
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        if (!config.enabled() || !config.showSceneOverlay() || plugin.getRootState() == null) {
            return null;
        }

        if (config.showNpcLosTiles()) {
            for (Map.Entry<Integer, List<WorldPoint>> entry : plugin.getNpcLineOfSightTiles().entrySet()) {
                Color base = colorForNpc(entry.getKey(), config.npcLosColor());
                Color fill = withAlpha(base, 35);
                Color border = withAlpha(base, 120);
                drawTiles(graphics, entry.getValue(), fill, border, 1f, false);
            }
        }

        if (config.showNpcPaths()) {
            for (Map.Entry<Integer, List<WorldPoint>> entry : plugin.getNpcPredictedPaths().entrySet()) {
                Color base = colorForNpc(entry.getKey(), config.npcPathColor());
                Color fill = withAlpha(base, 32);
                Color border = withAlpha(base, 195);
                drawTiles(graphics, entry.getValue(), fill, border, 1.2f, true);

                if (config.showNpcDebugLabels()) {
                    drawNpcPathLabel(graphics, entry.getKey(), entry.getValue(), border);
                }
            }
        }

        if (config.showBestMoveTile() && plugin.getLastDecisionResult() != null) {
            Color best = config.bestMoveColor();
            drawTile(graphics, plugin.getLastDecisionResult().getBestPlayerWorldPoint(), withAlpha(best, 52), withAlpha(best, 220), 2f);
        }

        return null;
    }

    private void drawNpcPathLabel(Graphics2D graphics, int npcIndex, List<WorldPoint> path, Color color) {
        if (path == null || path.isEmpty() || plugin.getRootState() == null) {
            return;
        }

        int slot = plugin.getRootState().findNpcSlotByIndex(npcIndex);
        if (slot < 0) {
            return;
        }

        WorldPoint source = plugin.getRootState().getNpcWorldPoint(slot);
        LocalPoint localPoint = LocalPoint.fromWorld(client.getTopLevelWorldView(), source);
        if (localPoint == null) {
            return;
        }

        String label = "idx=" + npcIndex + " path=" + path.size();
        net.runelite.api.Point text = Perspective.getCanvasTextLocation(client, graphics, localPoint, label, 30);
        if (text != null) {
            OverlayUtil.renderTextLocation(graphics, text, label, color);
        }
    }

    private void drawTiles(
            Graphics2D graphics,
            List<WorldPoint> points,
            Color fill,
            Color border,
            float strokeWidth,
            boolean connectCenters
    ) {
        if (points == null || points.isEmpty()) {
            return;
        }

        net.runelite.api.Point previousCenter = null;
        for (WorldPoint point : points) {
            LocalPoint localPoint = LocalPoint.fromWorld(client.getTopLevelWorldView(), point);
            if (localPoint == null) {
                continue;
            }

            Polygon polygon = Perspective.getCanvasTilePoly(client, localPoint);
            if (polygon != null) {
                graphics.setColor(fill);
                graphics.fillPolygon(polygon);
                graphics.setColor(border);
                graphics.setStroke(new BasicStroke(strokeWidth));
                graphics.drawPolygon(polygon);
            }

            if (connectCenters) {
                net.runelite.api.Point center = Perspective.localToCanvas(client, localPoint, client.getTopLevelWorldView().getPlane());
                if (center != null && previousCenter != null) {
                    graphics.setColor(border);
                    graphics.setStroke(new BasicStroke(1f));
                    graphics.drawLine(previousCenter.getX(), previousCenter.getY(), center.getX(), center.getY());
                }
                previousCenter = center;
            }
        }
    }

    private void drawTile(Graphics2D graphics, WorldPoint point, Color fill, Color border, float strokeWidth) {
        if (point == null) {
            return;
        }

        LocalPoint localPoint = LocalPoint.fromWorld(client.getTopLevelWorldView(), point);
        if (localPoint == null) {
            return;
        }

        Polygon polygon = Perspective.getCanvasTilePoly(client, localPoint);
        if (polygon == null) {
            return;
        }

        graphics.setColor(fill);
        graphics.fillPolygon(polygon);
        graphics.setColor(border);
        graphics.setStroke(new BasicStroke(strokeWidth));
        graphics.drawPolygon(polygon);
    }

    private Color colorForNpc(int npcIndex, Color base) {
        float hue = ((npcIndex * 37) % 360) / 360f;
        Color derived = Color.getHSBColor(hue, 0.76f, 1.0f);
        int alpha = base == null ? 160 : base.getAlpha();
        return new Color(derived.getRed(), derived.getGreen(), derived.getBlue(), alpha);
    }

    private Color withAlpha(Color color, int alpha) {
        int safeAlpha = Math.max(0, Math.min(255, alpha));
        if (color == null) {
            return new Color(255, 255, 255, safeAlpha);
        }
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), safeAlpha);
    }
}
