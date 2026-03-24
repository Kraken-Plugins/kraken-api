package com.kraken.api.overlay.log;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.PanelComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import java.awt.*;
import java.util.List;

@Singleton
public class LogOverlayComponent {

    private static final Color HEADER_COLOR = Color.CYAN;
    private static final Color INFO_COLOR   = Color.LIGHT_GRAY;
    private static final Color WARN_COLOR   = Color.YELLOW;
    private static final Color ERROR_COLOR  = new Color(255, 80, 80);
    private static final Color META_COLOR   = Color.GRAY;

    private final PluginLogger pluginLogger;

    @Inject
    public LogOverlayComponent(PluginLogger pluginLogger) {
        this.pluginLogger = pluginLogger;
    }

    /**
     * Appends a log section directly to the given panel.
     * Call this at the end of your overlay's render() method.
     *
     * @param panelComponent The panel component to add to
     * <pre>
     *   {@literal @}Override
     *   public Dimension render(Graphics2D graphics) {
     *       // ... your existing stats ...
     *       logOverlayComponent.addTo(panelComponent);
     *       return super.render(graphics);
     *   }
     * </pre>
     */
    public void addTo(PanelComponent panelComponent) {
        List<LogEntry> entries = pluginLogger.getEntries();
        if (entries.isEmpty()) return;

        panelComponent.getChildren().add(TitleComponent.builder()
                .text("Log")
                .color(HEADER_COLOR)
                .build());

        for (LogEntry entry : entries) {
            panelComponent.getChildren().add(LineComponent.builder()
                    .left(String.format("[%s] %s (%s)",
                            entry.getTimestamp(),
                            entry.getSource(),
                            entry.getThread()))
                    .leftColor(META_COLOR)
                    .build());
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("  " + entry.getMessage())
                    .leftColor(colorFor(entry.getLevel()))
                    .build());
        }
    }

    private static Color colorFor(LogLevel level) {
        switch (level) {
            case WARN:  return WARN_COLOR;
            case ERROR: return ERROR_COLOR;
            default:    return INFO_COLOR;
        }
    }
}
