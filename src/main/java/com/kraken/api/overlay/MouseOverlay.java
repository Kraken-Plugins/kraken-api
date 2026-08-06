package com.kraken.api.overlay;

import lombok.Setter;
import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

import javax.inject.Inject;
import java.awt.*;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

public class MouseOverlay extends Overlay {
    private final Client client;
    private final Deque<TrailPoint> trail = new ArrayDeque<>();

    // TODO Can optionally make these configurable as well
    private static final long MAX_TRAIL_LIFETIME_MS = 2000; // 3 seconds max
    private static final int MAX_TRAIL_POINTS = 200; // Performance cap
    private static final int CROSSHAIR_SIZE = 10; // Length of each arm in pixels
    private static final int CROSSHAIR_GAP = 3;   // Empty space in the dead center

    // Loop-invariant paint resources, allocated once rather than per frame / per trail segment.
    private static final Stroke THIN_STROKE = new BasicStroke(1);
    private static final Font COORDS_FONT = new Font("Arial", Font.BOLD, 12);
    private static final Color[] TRAIL_COLORS = new Color[256];

    static {
        for (int alpha = 0; alpha < TRAIL_COLORS.length; alpha++) {
            TRAIL_COLORS[alpha] = new Color(0, 255, 255, alpha); // Cyan trail at each alpha level
        }
    }

    @Setter
    private boolean renderCrosshair = true;

    @Setter
    private boolean renderTrail = true;

    @Inject
    public MouseOverlay(Client client) {
        this.client = client;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        net.runelite.api.Point mousePos = client.getMouseCanvasPosition();

        if(renderTrail) {
            updateTrail(mousePos);
            renderTrail(graphics);
        }

        if(renderCrosshair) {
            if (mousePos.getX() != -1 && mousePos.getY() != -1) {
                renderCrosshair(graphics, mousePos);
            }
        }

        return null;
    }

    private void updateTrail(net.runelite.api.Point currentPos) {
        long now = System.currentTimeMillis();

        // Only add a new point if it's on the canvas
        if (currentPos.getX() != -1 && currentPos.getY() != -1) {
            // Calculate speed (distance from last point) to determine fade factor
            double speed = 0;
            if (!trail.isEmpty()) {
                Point last = trail.peekLast().point;
                speed = Math.sqrt(Math.pow(currentPos.getX() - last.getX(), 2) + Math.pow(currentPos.getY() - last.getY(), 2));
            }

            trail.add(new TrailPoint(currentPos, now, speed));
        }

        // Prune old points based on time
        while (!trail.isEmpty() && (now - trail.peekFirst().timestamp > MAX_TRAIL_LIFETIME_MS)) {
            trail.removeFirst();
        }

        // Prune if too many points
        while (trail.size() > MAX_TRAIL_POINTS) {
            trail.removeFirst();
        }
    }

    private void renderTrail(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setStroke(THIN_STROKE);

        long now = System.currentTimeMillis();
        Iterator<TrailPoint> it = trail.iterator();
        TrailPoint prev = null;

        while (it.hasNext()) {
            TrailPoint current = it.next();

            if (prev != null) {
                // Calculate age
                long age = now - current.timestamp;

                // Logic: High speed movement creates a "faster" fade visually
                // because points are further apart.
                // We map age to alpha (0.0 to 1.0).
                float alpha = 1.0f - ((float) age / MAX_TRAIL_LIFETIME_MS);

                // Clamp alpha
                alpha = Math.max(0.0f, Math.min(1.0f, alpha));

                g.setColor(TRAIL_COLORS[(int)(alpha * 255)]);
                g.drawLine(prev.point.getX(), prev.point.getY(), current.point.getX(), current.point.getY());
            }
            prev = current;
        }
    }

    private void renderCrosshair(Graphics2D g, net.runelite.api.Point p) {
        g.setColor(Color.CYAN);
        g.setStroke(THIN_STROKE);

        int x = p.getX();
        int y = p.getY();

        // Draw 4 separate segments around the center point
        // Top arm
        g.drawLine(x, y - CROSSHAIR_SIZE, x, y - CROSSHAIR_GAP);
        // Bottom arm
        g.drawLine(x, y + CROSSHAIR_GAP, x, y + CROSSHAIR_SIZE);
        // Left arm
        g.drawLine(x - CROSSHAIR_SIZE, y, x - CROSSHAIR_GAP, y);
        // Right arm
        g.drawLine(x + CROSSHAIR_GAP, y, x + CROSSHAIR_SIZE, y);

        String coords = "(" + x + ", " + y + ")";
        g.setColor(Color.WHITE);
        g.setFont(COORDS_FONT);

        // Offset text slightly to the bottom right of the crosshair
        int textOffsetX = CROSSHAIR_SIZE + 5;
        int textOffsetY = CROSSHAIR_SIZE + 15;

        g.setColor(Color.BLACK);
        g.drawString(coords, x + textOffsetX + 1, y + textOffsetY + 1);
        g.setColor(Color.WHITE);
        g.drawString(coords, x + textOffsetX, y + textOffsetY);
    }

    private static class TrailPoint {
        net.runelite.api.Point point;
        long timestamp;
        double speed;

        TrailPoint(net.runelite.api.Point point, long timestamp, double speed) {
            this.point = point;
            this.timestamp = timestamp;
            this.speed = speed;
        }
    }
}
