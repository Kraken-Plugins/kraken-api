package com.kraken.api.sim.colosim;

import javax.swing.*;
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
}
