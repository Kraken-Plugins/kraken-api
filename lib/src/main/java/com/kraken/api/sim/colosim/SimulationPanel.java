package com.kraken.api.sim.colosim;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.List;

public class SimulationPanel extends JPanel implements MouseListener, MouseMotionListener {
    public enum Tool {
        SELECT,
        SET_PLAYER,
        ADD_NPC,
        REMOVE_NPC
    }

    private static final int TILE_SIZE = 22;
    private static final int MAP_PIXEL_SIZE = Simulation.MAP_WIDTH * TILE_SIZE;
    private static final Color[] MANTICORE_STYLE_COLORS = {Color.GREEN, Color.BLUE, Color.RED};

    private final Simulation simulation;
    private final SpriteStore sprites = new SpriteStore();

    private Tool tool = Tool.SELECT;
    private NpcType placementNpcType = NpcType.SERPENT_SHAMAN;
    private String placementManticoreExtra = "u";
    private boolean showPlayerLos = true;
    private boolean showVenatorBounce = true;

    private int selectedMobIndex = -1;
    private int hoveredMobIndex = -1;

    private Runnable stateChangedCallback;

    public SimulationPanel(Simulation simulation) {
        this.simulation = simulation;
        setPreferredSize(new Dimension(MAP_PIXEL_SIZE + 24, MAP_PIXEL_SIZE + 24));
        setBackground(AppTheme.BG);
        addMouseListener(this);
        addMouseMotionListener(this);
    }

    public void setTool(Tool tool) {
        this.tool = tool;
    }

    public void setPlacementNpcType(NpcType placementNpcType) {
        this.placementNpcType = placementNpcType;
    }

    public void setPlacementManticoreExtra(String placementManticoreExtra) {
        this.placementManticoreExtra = placementManticoreExtra;
    }

    public void setShowPlayerLos(boolean showPlayerLos) {
        this.showPlayerLos = showPlayerLos;
        repaint();
    }

    public void setShowVenatorBounce(boolean showVenatorBounce) {
        this.showVenatorBounce = showVenatorBounce;
        repaint();
    }

    public int getSelectedMobIndex() {
        return selectedMobIndex;
    }

    public void setStateChangedCallback(Runnable stateChangedCallback) {
        this.stateChangedCallback = stateChangedCallback;
    }

    private void onStateChanged() {
        if (stateChangedCallback != null) {
            stateChangedCallback.run();
        }
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int mapX = 12;
        int mapY = 12;
        drawFrame(g, mapX, mapY);
        g.translate(mapX, mapY);

        drawBoard(g);

        if (showPlayerLos) {
            NpcInfo playerInfo = simulation.getNpcInfo(NpcType.PLAYER.typeId);
            Tile player = simulation.getPlayer();
            drawLosOverlay(g, player.x, player.y, playerInfo.size, playerInfo.range, false, new Color(255, 0, 0, 45));
        }

        if (selectedMobIndex >= 0 && selectedMobIndex < simulation.getMobs().size()) {
            Mob selected = simulation.getMobs().get(selectedMobIndex);
            NpcInfo info = simulation.getNpcInfo(selected.type);
            drawLosOverlay(g, selected.x, selected.y, info.size, info.range, true, new Color(0, 120, 255, 45));
        }

        drawUnits(g);

        g.setColor(AppTheme.MUTED);
        g.drawString("N", (Simulation.MAP_WIDTH / 2) * TILE_SIZE, 14);
        g.drawString("S", (Simulation.MAP_WIDTH / 2) * TILE_SIZE, Simulation.MAP_HEIGHT * TILE_SIZE - 6);
        g.dispose();
    }

    private void drawFrame(Graphics2D g, int x, int y) {
        g.setColor(new Color(28, 36, 46, 24));
        g.fill(new RoundRectangle2D.Double(x + 4, y + 6, MAP_PIXEL_SIZE, MAP_PIXEL_SIZE, 18, 18));
        g.setColor(Color.WHITE);
        g.fill(new RoundRectangle2D.Double(x, y, MAP_PIXEL_SIZE, MAP_PIXEL_SIZE, 18, 18));
    }

    private void drawBoard(Graphics2D g) {
        for (int y = 0; y < Simulation.MAP_HEIGHT; y++) {
            for (int x = 0; x < Simulation.MAP_WIDTH; x++) {
                g.setColor(((x + y) & 1) == 0 ? new Color(245, 248, 252) : new Color(237, 243, 250));
                g.fillRect(x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
            }
        }

        g.setColor(new Color(44, 52, 65));
        int[][][] blocked = simulation.getBlockedTileRanges();
        for (int y = 0; y < blocked.length; y++) {
            int[][] row = blocked[y];
            for (int i = 0; i < row.length; i++) {
                int[] range = row[i];
                g.fillRect(range[0] * TILE_SIZE, y * TILE_SIZE, (range[1] - range[0]) * TILE_SIZE, TILE_SIZE);
            }
        }

        g.setColor(new Color(58, 67, 82));
        int[][] pillars = simulation.getPillars();
        boolean[] filters = simulation.getPillarFilters();
        for (int i = 0; i < pillars.length; i++) {
            if (!filters[i]) {
                continue;
            }
            int[] p = pillars[i];
            g.fillRect(p[0] * TILE_SIZE, (p[1] - 2) * TILE_SIZE, 3 * TILE_SIZE, 3 * TILE_SIZE);
        }
    }

    private void drawLosOverlay(Graphics2D g, int x, int y, int s, int r, boolean isNpc, Color color) {
        g.setColor(color);
        for (int yy = 0; yy < Simulation.MAP_HEIGHT; yy++) {
            for (int xx = 0; xx < Simulation.MAP_WIDTH; xx++) {
                if (simulation.hasLOS(x, y, xx, yy, s, r, isNpc)) {
                    g.fillRect(xx * TILE_SIZE, yy * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                }
            }
        }
    }

    private void drawUnits(Graphics2D g) {
        Tile player = simulation.getPlayer();
        drawSpriteOrFallback(g, NpcType.PLAYER.typeId, player.x, player.y, 1, Color.RED, true);

        List<Mob> mobs = simulation.getMobs();
        for (int i = 0; i < mobs.size(); i++) {
            Mob mob = mobs.get(i);
            NpcInfo info = simulation.getNpcInfo(mob.type);
            int size = info.size;
            int topY = (mob.y - size + 1) * TILE_SIZE;

            Color c = namedColor(info.color);
            g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 120));
            g.fillRect(mob.x * TILE_SIZE, topY, size * TILE_SIZE, size * TILE_SIZE);
            g.setColor(c);
            g.setStroke(new BasicStroke(2f));
            g.drawRect(mob.x * TILE_SIZE + 1, topY + 1, size * TILE_SIZE - 2, size * TILE_SIZE - 2);
            g.setStroke(new BasicStroke(1f));

            drawSpriteOrFallback(g, mob.type, mob.x, mob.y, size, c, false);

            if (simulation.canAttackPlayer(mob)) {
                g.setColor(Color.BLACK);
                g.fillRect(mob.x * TILE_SIZE, mob.y * TILE_SIZE, TILE_SIZE / 4, TILE_SIZE / 4);
            }

            if (mob.type == Simulation.MANTICORE && mob.extra != null && !"u".equals(mob.extra)) {
                drawManticorePattern(g, mob.x, mob.y, mob.extra);
            }

            if (showVenatorBounce && hoveredMobIndex >= 0 && hoveredMobIndex < mobs.size() && hoveredMobIndex != i) {
                Mob source = mobs.get(hoveredMobIndex);
                int sourceSize = simulation.getNpcInfo(source.type).size;
                if (Venator.canBounce(source.x, source.y, sourceSize, mob.x, mob.y, size)) {
                    g.setColor(new Color(255, 105, 180));
                    g.setStroke(new BasicStroke(3f));
                    g.drawRect(mob.x * TILE_SIZE + 1, topY + 1, size * TILE_SIZE - 2, size * TILE_SIZE - 2);
                    g.setStroke(new BasicStroke(1f));
                }
            }
        }

        if (selectedMobIndex >= 0 && selectedMobIndex < mobs.size()) {
            Mob selected = mobs.get(selectedMobIndex);
            int size = simulation.getNpcInfo(selected.type).size;
            int topY = (selected.y - size + 1) * TILE_SIZE;
            g.setColor(Color.WHITE);
            g.setStroke(new BasicStroke(3f));
            g.drawRect(selected.x * TILE_SIZE + 1, topY + 1, size * TILE_SIZE - 2, size * TILE_SIZE - 2);
            g.setStroke(new BasicStroke(1f));
        }
    }

    private void drawSpriteOrFallback(Graphics2D g, int type, int x, int y, int size, Color fallback, boolean circle) {
        int topY = (y - size + 1) * TILE_SIZE;
        int width = size * TILE_SIZE;
        int height = size * TILE_SIZE;
        BufferedImage sprite = sprites.get(type);
        if (sprite != null) {
            Image scaled = sprites.scale(sprite, width, height);
            if (scaled != null) {
                g.drawImage(scaled, x * TILE_SIZE, topY, null);
                return;
            }
        }

        if (circle) {
            g.setColor(fallback);
            g.fillOval(x * TILE_SIZE + 2, y * TILE_SIZE + 2, TILE_SIZE - 4, TILE_SIZE - 4);
            return;
        }

        g.setPaint(new GradientPaint(x * TILE_SIZE, topY, fallback, x * TILE_SIZE + width, topY + height, fallback.brighter()));
        g.fillRoundRect(x * TILE_SIZE + 2, topY + 2, width - 4, height - 4, 10, 10);
    }

    private void drawManticorePattern(Graphics2D g, int x, int y, String mobExtra) {
        int[] pattern = simulation.getManticorePattern(mobExtra);
        if (pattern == null) {
            return;
        }
        boolean uncharged = mobExtra.startsWith("u");
        for (int i = 0; i < pattern.length; i++) {
            int style = pattern[i];
            Color color = MANTICORE_STYLE_COLORS[style];
            if (uncharged) {
                color = new Color(color.getRed(), color.getGreen(), color.getBlue(), 90);
            }
            g.setColor(color);
            int centerX = (int) ((x + 2.5) * TILE_SIZE);
            int centerY = (int) ((y - i + 0.5) * TILE_SIZE);
            int radius = TILE_SIZE / 3;
            g.fillOval(centerX - radius, centerY - radius, radius * 2, radius * 2);
        }
    }

    private Color namedColor(String name) {
        if ("cyan".equals(name)) return Color.CYAN;
        if ("lime".equals(name)) return Color.GREEN;
        if ("orange".equals(name)) return Color.ORANGE;
        if ("purple".equals(name)) return new Color(128, 0, 128);
        if ("brown".equals(name)) return new Color(120, 72, 0);
        if ("blue".equals(name)) return Color.BLUE;
        if ("red".equals(name)) return Color.RED;
        return Color.GRAY;
    }

    private Tile toTile(MouseEvent e) {
        int mapX = e.getX() - 12;
        int mapY = e.getY() - 12;
        if (mapX < 0 || mapY < 0) {
            return null;
        }
        int x = mapX / TILE_SIZE;
        int y = mapY / TILE_SIZE;
        if (x < 0 || x >= Simulation.MAP_WIDTH || y < 0 || y >= Simulation.MAP_HEIGHT) {
            return null;
        }
        return new Tile(x, y);
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        Tile tile = toTile(e);
        if (tile == null) {
            return;
        }

        if (e.getButton() == MouseEvent.BUTTON3) {
            toggleManticoreCharge(tile.x, tile.y);
            return;
        }

        if (tool == Tool.SET_PLAYER) {
            simulation.setPlayer(tile.x, tile.y);
            onStateChanged();
            repaint();
            return;
        }
        if (tool == Tool.ADD_NPC) {
            String extra = placementNpcType == NpcType.MANTICORE ? placementManticoreExtra : null;
            simulation.placeMob(tile.x, tile.y, placementNpcType, extra);
            onStateChanged();
            repaint();
            return;
        }
        if (tool == Tool.REMOVE_NPC) {
            simulation.removeMobAtTile(tile.x, tile.y);
            selectedMobIndex = -1;
            onStateChanged();
            repaint();
            return;
        }

        selectedMobIndex = simulation.findMobIndexAtTile(tile.x, tile.y);
        onStateChanged();
        repaint();
    }

    private void toggleManticoreCharge(int x, int y) {
        int idx = simulation.findMobIndexAtTile(x, y);
        if (idx < 0) {
            return;
        }
        Mob mob = simulation.getMobs().get(idx);
        if (mob.type != Simulation.MANTICORE) {
            return;
        }
        String currentExtra = mob.extra;
        String originalExtra = mob.originalExtra;
        if (currentExtra == null || "u".equals(originalExtra)) {
            return;
        }
        boolean isCurrentlyUncharged = currentExtra.startsWith("u");
        if (isCurrentlyUncharged) {
            mob.extra = currentExtra.substring(1);
            mob.originalExtra = currentExtra.substring(1);
        } else {
            String uncharged = "u" + currentExtra;
            mob.extra = uncharged;
            mob.originalExtra = uncharged;
        }
        mob.cooldown = 0;
        onStateChanged();
        repaint();
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        Tile tile = toTile(e);
        if (tile == null) {
            hoveredMobIndex = -1;
        } else {
            hoveredMobIndex = simulation.findMobIndexAtTile(tile.x, tile.y);
        }
        repaint();
    }

    @Override
    public void mousePressed(MouseEvent e) {
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
        hoveredMobIndex = -1;
        repaint();
    }

    @Override
    public void mouseDragged(MouseEvent e) {
    }
}
