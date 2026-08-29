package colosim;

import colosim.model.Mob;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.util.List;

public class TimelinePanel extends JPanel {
    private static final int CELL_SIZE = 18;
    private static final int PADDING = 10;
    private static final int TICK_LABEL_WIDTH = 42;
    private static final int MIN_COLUMNS = 9;
    private static final Color[] MANTICORE_STYLE_COLORS = {Color.GREEN, Color.BLUE, Color.RED};

    private final Simulation simulation;

    public TimelinePanel(Simulation simulation) {
        this.simulation = simulation;
        setOpaque(false);
        setPreferredSize(new Dimension(230, Simulation.MAP_HEIGHT * CELL_SIZE));
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int rowCount = Math.max(1, simulation.getTape().size());
        int columns = Math.max(MIN_COLUMNS, simulation.getMobs().size());
        int innerWidth = TICK_LABEL_WIDTH + columns * CELL_SIZE;
        int innerHeight = rowCount * CELL_SIZE;
        updatePreferredSizeIfNeeded(innerWidth, innerHeight);

        g.setColor(new Color(26, 34, 44, 20));
        g.fill(new RoundRectangle2D.Double(2, 4, innerWidth + PADDING * 2, innerHeight + PADDING * 2, 14, 14));
        g.setColor(Color.WHITE);
        g.fill(new RoundRectangle2D.Double(0, 0, innerWidth + PADDING * 2, innerHeight + PADDING * 2, 14, 14));

        drawRows(g, columns, rowCount);
        drawAttackCells(g);
        drawGrid(g, columns, rowCount);

        g.dispose();
    }

    private void drawRows(Graphics2D g, int columns, int rowCount) {
        int width = TICK_LABEL_WIDTH + columns * CELL_SIZE;
        for (int i = 0; i < rowCount; i++) {
            boolean waveDelayRow = simulation.isFromWaveStart() && i < Simulation.DELAY_FIRST_ATTACK_TICKS;
            Color rowColor;
            if (waveDelayRow) {
                rowColor = (i & 1) == 0 ? new Color(103, 110, 120) : new Color(117, 124, 134);
            } else {
                rowColor = (i & 1) == 0 ? new Color(232, 238, 247) : new Color(242, 246, 252);
            }
            g.setColor(rowColor);
            g.fillRect(PADDING, PADDING + i * CELL_SIZE, width, CELL_SIZE);

            g.setColor(new Color(74, 82, 92));
            g.drawString(String.format("%02d", i + 1), PADDING + 8, PADDING + i * CELL_SIZE + 13);
        }
    }

    private void drawGrid(Graphics2D g, int columns, int rowCount) {
        int xStart = PADDING + TICK_LABEL_WIDTH;
        int yStart = PADDING;
        g.setColor(new Color(194, 205, 220));
        g.setStroke(new BasicStroke(1f));
        for (int x = 0; x <= columns; x++) {
            int px = xStart + x * CELL_SIZE;
            g.drawLine(px, yStart, px, yStart + rowCount * CELL_SIZE);
        }
        for (int y = 0; y <= rowCount; y++) {
            int py = yStart + y * CELL_SIZE;
            g.drawLine(PADDING, py, PADDING + TICK_LABEL_WIDTH + columns * CELL_SIZE, py);
        }
    }

    private void drawAttackCells(Graphics2D g) {
        List<int[]> tape = simulation.getTape();
        List<Mob> mobs = simulation.getMobs();
        for (int row = 0; row < tape.size(); row++) {
            int[] line = tape.get(row);
            for (int col = 0; col < line.length && col < mobs.size(); col++) {
                int value = line[col];
                int attacked = value & 0xff;
                if (attacked == 0) {
                    continue;
                }
                Mob mob = mobs.get(col);
                int x = PADDING + TICK_LABEL_WIDTH + col * CELL_SIZE;
                int y = PADDING + row * CELL_SIZE;

                g.setColor(namedColor(simulation.getNpcInfo(mob.getType()).getColor()));
                g.fillRect(x, y, CELL_SIZE, CELL_SIZE);

                if (mob.getType() == Simulation.MANTICORE) {
                    int style = (value >> 8) & 0xff;
                    if (style >= 0 && style < MANTICORE_STYLE_COLORS.length) {
                        g.setColor(MANTICORE_STYLE_COLORS[style]);
                        g.fillOval(x + 2, y + 2, CELL_SIZE - 4, CELL_SIZE - 4);
                        g.setColor(Color.WHITE);
                        g.drawOval(x + 2, y + 2, CELL_SIZE - 4, CELL_SIZE - 4);
                    }
                }
            }
        }
    }

    private void updatePreferredSizeIfNeeded(int innerWidth, int innerHeight) {
        Dimension desired = new Dimension(innerWidth + PADDING * 2, innerHeight + PADDING * 2);
        Dimension current = getPreferredSize();
        if (current.width != desired.width || current.height != desired.height) {
            setPreferredSize(desired);
            revalidate();
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
}
