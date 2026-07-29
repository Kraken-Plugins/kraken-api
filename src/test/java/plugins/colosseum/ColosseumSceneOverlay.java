package plugins.colosseum;

import com.google.inject.Inject;
import com.kraken.api.simulation.colosseum.ColoCoords;
import com.kraken.api.simulation.colosseum.ColoGrid;
import com.kraken.api.simulation.colosseum.live.ColoCapture;
import com.kraken.api.simulation.colosseum.plan.ColoDecision;
import com.kraken.api.simulation.colosseum.plan.DangerMap;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.OverlayUtil;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.util.List;
import java.util.Map;

/**
 * Scene overlay: candidate-tile score heat map (green best to red worst), the danger map
 * (tiles NPCs can currently hit, shaded by expected damage), the chosen plan's predicted
 * player path, and predicted NPC movement.
 */
public class ColosseumSceneOverlay extends Overlay {
    private final Client client;
    private final AutoColosseumPlugin plugin;
    private final AutoColosseumConfig config;

    @Inject
    public ColosseumSceneOverlay(Client client, AutoColosseumPlugin plugin, AutoColosseumConfig config) {
        this.client = client;
        this.plugin = plugin;
        this.config = config;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
        setPriority(OverlayPriority.HIGH);
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        if (!config.enabled()) {
            return null;
        }
        ColoCapture capture = plugin.getLastCapture();
        ColoDecision decision = plugin.getLastDecision();
        if (capture == null) {
            return null;
        }

        if (config.showDangerTiles()) {
            renderDangerMap(graphics, capture.getFrame().getGrid());
        }
        if (config.showCandidateTiles() && decision != null) {
            renderCandidates(graphics, decision);
        }
        if (config.showPlannedPath() && decision != null) {
            renderPlannedPath(graphics, decision);
        }
        if (config.showNpcPaths()) {
            renderNpcPaths(graphics);
        }
        return null;
    }

    private void renderDangerMap(Graphics2D graphics, ColoGrid grid) {
        DangerMap dangerMap = plugin.dangerMap();
        short center = dangerMap.center();
        if (!ColoCoords.isPresent(center)) {
            return;
        }
        int radius = dangerMap.radius();
        int cx = ColoCoords.x(center);
        int cy = ColoCoords.y(center);
        for (int y = cy - radius; y <= cy + radius; y++) {
            for (int x = cx - radius; x <= cx + radius; x++) {
                if (!grid.inBounds(x, y)) {
                    continue;
                }
                short tile = ColoCoords.pack(x, y);
                int damage = dangerMap.expectedDamagePerTickX100(tile);
                if (damage <= 0) {
                    continue;
                }
                // Shade by expected damage per tick; ~8 hp/tick maps to full intensity.
                int alpha = Math.min(110, 25 + damage / 8);
                fillTile(graphics, grid.toWorld(tile), new Color(255, 40, 40, alpha), null);
            }
        }
    }

    private void renderCandidates(Graphics2D graphics, ColoDecision decision) {
        int count = decision.candidateCount();
        if (count == 0) {
            return;
        }
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < count; i++) {
            double score = decision.candidateScore(i);
            if (Double.isFinite(score)) {
                min = Math.min(min, score);
                max = Math.max(max, score);
            }
        }
        double span = Math.max(1e-6, max - min);
        WorldPoint best = decision.getMoveDestination();
        for (int i = 0; i < count; i++) {
            double score = decision.candidateScore(i);
            if (!Double.isFinite(score)) {
                continue;
            }
            float goodness = (float) ((score - min) / span);
            Color color = new Color(
                    (int) (215 * (1 - goodness)) + 40,
                    (int) (205 * goodness) + 50,
                    55,
                    95
            );
            WorldPoint point = decision.candidateWorldPoint(i);
            fillTile(graphics, point, color, null);
            if (best != null && best.equals(point)) {
                fillTile(graphics, point, null, new Color(255, 255, 255, 230));
            }
            // Normalized goodness on the tile itself: 100 = best candidate this tick.
            drawTileText(graphics, point, Integer.toString(Math.round(goodness * 100)),
                    new Color(255, 255, 255, 220));
        }
    }

    private void drawTileText(Graphics2D graphics, WorldPoint point, String text, Color color) {
        LocalPoint localPoint = LocalPoint.fromWorld(client.getTopLevelWorldView(), point);
        if (localPoint == null) {
            return;
        }
        Point textLocation = Perspective.getCanvasTextLocation(client, graphics, localPoint, text, 0);
        if (textLocation == null) {
            return;
        }
        OverlayUtil.renderTextLocation(graphics, textLocation, text, color);
    }

    private void renderPlannedPath(Graphics2D graphics, ColoDecision decision) {
        WorldPoint[] path = decision.plannedPathWorld();
        WorldPoint previous = null;
        for (WorldPoint point : path) {
            if (previous != null && previous.equals(point)) {
                continue;
            }
            previous = point;
            fillTile(graphics, point, new Color(60, 200, 255, 45), new Color(60, 200, 255, 180));
        }
    }

    private void renderNpcPaths(Graphics2D graphics) {
        for (Map.Entry<Integer, List<WorldPoint>> entry : plugin.getNpcPredictedPaths().entrySet()) {
            Color base = colorForIndex(entry.getKey());
            for (WorldPoint point : entry.getValue()) {
                fillTile(graphics, point, withAlpha(base, 35), withAlpha(base, 160));
            }
        }
    }

    private void fillTile(Graphics2D graphics, WorldPoint point, Color fill, Color border) {
        LocalPoint localPoint = LocalPoint.fromWorld(client.getTopLevelWorldView(), point);
        if (localPoint == null) {
            return;
        }
        Polygon poly = Perspective.getCanvasTilePoly(client, localPoint);
        if (poly == null) {
            return;
        }
        if (fill != null) {
            graphics.setColor(fill);
            graphics.fill(poly);
        }
        if (border != null) {
            graphics.setColor(border);
            graphics.setStroke(new BasicStroke(1.4f));
            graphics.draw(poly);
        }
    }

    private static Color colorForIndex(int npcIndex) {
        float hue = (npcIndex * 0.61803399f) % 1f;
        return Color.getHSBColor(hue, 0.75f, 1f);
    }

    private static Color withAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }
}
