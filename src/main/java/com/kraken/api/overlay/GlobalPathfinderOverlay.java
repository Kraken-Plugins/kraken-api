package com.kraken.api.overlay;

import com.google.inject.Inject;
import com.kraken.api.service.map.WorldMapService;
import com.kraken.api.service.pathfinding.GlobalPathfinder;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.OverlayUtil;

import javax.inject.Singleton;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Line2D;
import java.util.List;

@Singleton
public class GlobalPathfinderOverlay extends Overlay {
    private static final Color PATH_FILL = new Color(60, 165, 250, 60);
    private static final Color PATH_OUTLINE = new Color(56, 189, 248, 220);
    private static final Color SPARSE_FILL = new Color(245, 158, 11, 105);
    private static final Color SPARSE_OUTLINE = new Color(249, 115, 22, 235);
    private static final Color TRANSPORT_COLOR = new Color(236, 72, 153, 225);
    private static final Color LABEL_BACKGROUND = new Color(15, 23, 42, 210);
    private static final int WORLD_MAP_TILE_SIZE_FLOOR = 3;

    private final Client client;
    private final GlobalPathfinder globalPathfinder;
    private final WorldMapService worldMapService;

    @Inject
    public GlobalPathfinderOverlay(Client client, GlobalPathfinder globalPathfinder, WorldMapService worldMapService) {
        this.client = client;
        this.globalPathfinder = globalPathfinder;
        this.worldMapService = worldMapService;

        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
        setPriority(OverlayPriority.MED);
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        GlobalPathfinder.PathResult result = globalPathfinder.getLastResult();
        if (result == null || result.getPath().isEmpty()) {
            return null;
        }

        renderScenePath(graphics, result.getPath(), result.getSparsePath());
        renderSceneTransports(graphics, result.getTransports());
        renderWorldMapPath(graphics, result.getPath(), result.getSparsePath(), result.getTransports());
        return null;
    }

    private void renderScenePath(Graphics2D graphics, List<WorldPoint> densePath, List<WorldPoint> sparsePath) {
        Point previous = null;
        for (WorldPoint point : densePath) {
            LocalPoint localPoint = LocalPoint.fromWorld(client.getTopLevelWorldView(), point);
            if (localPoint == null || point.getPlane() != client.getTopLevelWorldView().getPlane()) {
                continue;
            }

            Polygon polygon = Perspective.getCanvasTilePoly(client, localPoint);
            if (polygon != null) {
                graphics.setColor(PATH_FILL);
                graphics.fillPolygon(polygon);
                graphics.setColor(PATH_OUTLINE);
                graphics.setStroke(new BasicStroke(1.5f));
                graphics.drawPolygon(polygon);
            }

            Point canvasPoint = Perspective.localToCanvas(client, localPoint, client.getTopLevelWorldView().getPlane());
            if (canvasPoint != null && previous != null) {
                graphics.setColor(PATH_OUTLINE);
                graphics.setStroke(new BasicStroke(1.25f));
                graphics.drawLine(previous.getX(), previous.getY(), canvasPoint.getX(), canvasPoint.getY());
            }
            previous = canvasPoint;
        }

        for (WorldPoint point : sparsePath) {
            LocalPoint localPoint = LocalPoint.fromWorld(client.getTopLevelWorldView(), point);
            if (localPoint == null || point.getPlane() != client.getTopLevelWorldView().getPlane()) {
                continue;
            }

            Polygon polygon = Perspective.getCanvasTilePoly(client, localPoint);
            if (polygon == null) {
                continue;
            }

            graphics.setColor(SPARSE_FILL);
            graphics.fillPolygon(polygon);
            graphics.setColor(SPARSE_OUTLINE);
            graphics.setStroke(new BasicStroke(2f));
            graphics.drawPolygon(polygon);
        }
    }

    private void renderSceneTransports(Graphics2D graphics, List<GlobalPathfinder.TransportUsage> transports) {
        for (GlobalPathfinder.TransportUsage transport : transports) {
            if (transport.getOrigin().getPlane() != client.getTopLevelWorldView().getPlane()) {
                continue;
            }

            LocalPoint originPoint = LocalPoint.fromWorld(client.getTopLevelWorldView(), transport.getOrigin());
            LocalPoint destinationPoint = LocalPoint.fromWorld(client.getTopLevelWorldView(), transport.getDestination());
            if (originPoint == null || destinationPoint == null) {
                continue;
            }

            Point start = Perspective.localToCanvas(client, originPoint, client.getTopLevelWorldView().getPlane());
            Point end = Perspective.localToCanvas(client, destinationPoint, client.getTopLevelWorldView().getPlane());
            if (start == null || end == null) {
                continue;
            }

            graphics.setColor(TRANSPORT_COLOR);
            graphics.setStroke(new BasicStroke(2.5f));
            graphics.draw(new Line2D.Float(start.getX(), start.getY(), end.getX(), end.getY()));
            drawLabel(graphics, midpoint(start, end), transportLabel(transport, false));
        }
    }

    private void renderWorldMapPath(
            Graphics2D graphics,
            List<WorldPoint> densePath,
            List<WorldPoint> sparsePath,
            List<GlobalPathfinder.TransportUsage> transports
    ) {
        if (!worldMapService.isWorldMapOpen()) {
            return;
        }

        Widget mapWidget = client.getWidget(InterfaceID.Worldmap.MAP_CONTAINER);
        if (mapWidget == null) {
            return;
        }

        Rectangle bounds = mapWidget.getBounds();
        Shape previousClip = graphics.getClip();
        graphics.setClip(bounds);

        int tileSize = Math.max(WORLD_MAP_TILE_SIZE_FLOOR, Math.round(client.getWorldMap().getWorldMapZoom()));
        for (WorldPoint point : densePath) {
            Integer x = worldMapService.worldPointToMapPointX(point);
            Integer y = worldMapService.worldPointToMapPointY(point);
            if (x == null || y == null) {
                continue;
            }

            graphics.setColor(PATH_FILL);
            graphics.fillRect(x - tileSize / 2, y - tileSize / 2, tileSize, tileSize);
            graphics.setColor(PATH_OUTLINE);
            graphics.drawRect(x - tileSize / 2, y - tileSize / 2, tileSize, tileSize);
        }

        for (WorldPoint point : sparsePath) {
            Integer x = worldMapService.worldPointToMapPointX(point);
            Integer y = worldMapService.worldPointToMapPointY(point);
            if (x == null || y == null) {
                continue;
            }

            int sparseSize = tileSize + 2;
            graphics.setColor(SPARSE_FILL);
            graphics.fillRect(x - sparseSize / 2, y - sparseSize / 2, sparseSize, sparseSize);
            graphics.setColor(SPARSE_OUTLINE);
            graphics.drawRect(x - sparseSize / 2, y - sparseSize / 2, sparseSize, sparseSize);
        }

        for (GlobalPathfinder.TransportUsage transport : transports) {
            Integer startX = worldMapService.worldPointToMapPointX(transport.getOrigin());
            Integer startY = worldMapService.worldPointToMapPointY(transport.getOrigin());
            Integer endX = worldMapService.worldPointToMapPointX(transport.getDestination());
            Integer endY = worldMapService.worldPointToMapPointY(transport.getDestination());
            if (startX == null || startY == null || endX == null || endY == null) {
                continue;
            }

            graphics.setColor(TRANSPORT_COLOR);
            graphics.setStroke(new BasicStroke(2f));
            graphics.drawLine(startX, startY, endX, endY);
            drawLabel(graphics, new Point((startX + endX) / 2, (startY + endY) / 2), transportLabel(transport, true));
        }

        graphics.setClip(previousClip);
    }

    private Point midpoint(Point start, Point end) {
        return new Point((start.getX() + end.getX()) / 2, (start.getY() + end.getY()) / 2);
    }

    private void drawLabel(Graphics2D graphics, Point anchor, String label) {
        if (anchor == null || label == null || label.isBlank()) {
            return;
        }

        FontMetrics metrics = graphics.getFontMetrics();
        int padding = 4;
        int width = metrics.stringWidth(label) + padding * 2;
        int height = metrics.getHeight();
        int x = anchor.getX() - width / 2;
        int y = anchor.getY() - height;

        graphics.setColor(LABEL_BACKGROUND);
        graphics.fillRoundRect(x, y, width, height, 6, 6);
        graphics.setColor(Color.WHITE);
        OverlayUtil.renderTextLocation(graphics, new Point(x + padding, y + metrics.getAscent()), label, Color.WHITE);
    }

    private String transportLabel(GlobalPathfinder.TransportUsage transport, boolean includeDetail) {
        String type = transport.getType().name().replace('_', ' ').toLowerCase();
        String normalizedType = Character.toUpperCase(type.charAt(0)) + type.substring(1);
        if (!includeDetail || transport.getDisplayInfo() == null || transport.getDisplayInfo().isBlank()) {
            return normalizedType;
        }
        return normalizedType + ": " + transport.getDisplayInfo();
    }
}
