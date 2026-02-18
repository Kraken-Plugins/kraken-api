package com.kraken.api.sim.colosim;

import javax.swing.*;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;

public final class AppTheme {
    public static final Color BG = new Color(245, 248, 252);
    public static final Color PANEL_BG = new Color(255, 255, 255);
    public static final Color ACCENT = new Color(15, 110, 240);
    public static final Color TEXT = new Color(24, 30, 37);
    public static final Color MUTED = new Color(113, 122, 133);

    private AppTheme() {
    }

    public static void install() {
        installNimbus();
        Font base = new Font("Segoe UI", Font.PLAIN, 13);
        UIManager.put("defaultFont", base);
        UIManager.put("control", BG);
        UIManager.put("nimbusBase", new Color(66, 95, 128));
        UIManager.put("nimbusSelectionBackground", ACCENT);
        UIManager.put("text", TEXT);
        UIManager.put("Panel.background", BG);
        UIManager.put("Label.foreground", TEXT);
        UIManager.put("ComboBox.background", PANEL_BG);
        UIManager.put("ComboBox.foreground", TEXT);
        UIManager.put("CheckBox.background", PANEL_BG);
        UIManager.put("TextArea.background", new Color(248, 250, 253));
    }

    private static void installNimbus() {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    return;
                }
            }
        } catch (ClassNotFoundException ignored) {
        } catch (InstantiationException ignored) {
        } catch (IllegalAccessException ignored) {
        } catch (UnsupportedLookAndFeelException ignored) {
        }
    }

    public static void styleCard(JComponent c) {
        c.setOpaque(true);
        c.setBackground(PANEL_BG);
        c.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 227, 236), 1, true),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
    }

    public static void styleButton(JButton b) {
        b.setBackground(ACCENT);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        b.setPreferredSize(new Dimension(90, 34));
    }

    public static void styleActionButton(JButton button, Color baseColor) {
        button.setUI(new BasicButtonUI());
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBorderPainted(false);
        button.setBackground(baseColor);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        button.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 12));
        button.setPreferredSize(new Dimension(68, 24));
        button.setMinimumSize(new Dimension(68, 24));
    }

    public static void styleCleanScrollBars(JScrollPane scrollPane) {
        scrollPane.setOpaque(false);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setOpaque(false);
        JScrollBar vertical = scrollPane.getVerticalScrollBar();
        JScrollBar horizontal = scrollPane.getHorizontalScrollBar();
        vertical.setUI(new CleanScrollBarUI());
        horizontal.setUI(new CleanScrollBarUI());
        vertical.setPreferredSize(new Dimension(10, 0));
        horizontal.setPreferredSize(new Dimension(0, 10));
        vertical.setUnitIncrement(14);
        horizontal.setUnitIncrement(14);
    }

    private static final class CleanScrollBarUI extends BasicScrollBarUI {
        private static final Color TRACK = new Color(223, 231, 240);
        private static final Color THUMB = new Color(104, 118, 137);
        private static final Color THUMB_HOVER = new Color(86, 100, 119);

        @Override
        protected void configureScrollBarColors() {
            trackColor = TRACK;
            thumbColor = THUMB;
            thumbDarkShadowColor = THUMB;
            thumbLightShadowColor = THUMB;
            thumbHighlightColor = THUMB_HOVER;
        }

        @Override
        protected JButton createDecreaseButton(int orientation) {
            return createScrollButton();
        }

        @Override
        protected JButton createIncreaseButton(int orientation) {
            return createScrollButton();
        }

        @Override
        protected Dimension getMinimumThumbSize() {
            return scrollbar != null && scrollbar.getOrientation() == Adjustable.HORIZONTAL
                    ? new Dimension(24, 8)
                    : new Dimension(8, 24);
        }

        @Override
        protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(TRACK);
            g2.fillRoundRect(trackBounds.x + 1, trackBounds.y + 1, trackBounds.width - 2, trackBounds.height - 2, 8, 8);
            g2.dispose();
        }

        @Override
        protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
            if (!scrollbar.isEnabled() || thumbBounds.width <= 0 || thumbBounds.height <= 0) {
                return;
            }
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(isThumbRollover() ? THUMB_HOVER : THUMB);
            g2.fillRoundRect(thumbBounds.x + 1, thumbBounds.y + 1, thumbBounds.width - 2, thumbBounds.height - 2, 8, 8);
            g2.dispose();
        }

        private static JButton createScrollButton() {
            JButton button = new JButton();
            button.setPreferredSize(new Dimension(0, 0));
            button.setMinimumSize(new Dimension(0, 0));
            button.setMaximumSize(new Dimension(0, 0));
            return button;
        }
    }
}
