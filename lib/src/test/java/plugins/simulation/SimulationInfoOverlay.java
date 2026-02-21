package plugins.simulation;

import com.google.inject.Inject;
import com.kraken.api.simulation.SimulationDecisionAdapter;
import com.kraken.api.simulation.SimulationSnapshot;
import net.runelite.api.Prayer;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import java.awt.*;
import java.util.stream.Collectors;

public class SimulationInfoOverlay extends OverlayPanel {
    private final SimulationPlugin plugin;
    private final SimulationPluginConfig config;

    @Inject
    public SimulationInfoOverlay(SimulationPlugin plugin, SimulationPluginConfig config) {
        this.plugin = plugin;
        this.config = config;
        setPosition(OverlayPosition.TOP_LEFT);
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        if (!config.enabled() || !config.showInfoOverlay()) {
            return null;
        }

        panelComponent.getChildren().clear();
        panelComponent.getChildren().add(TitleComponent.builder()
                .text("Simulation")
                .color(new Color(90, 230, 220))
                .build());

        if (plugin.getLastError() != null && !plugin.getLastError().trim().isEmpty()) {
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Error")
                    .right(plugin.getLastError())
                    .rightColor(Color.RED)
                    .build());
            return super.render(graphics);
        }

        SimulationSnapshot snapshot = plugin.getLastSnapshot();
        if (snapshot != null) {
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Tick")
                    .right(String.valueOf(snapshot.getGameTick()))
                    .rightColor(Color.WHITE)
                    .build());
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Plane")
                    .right(String.valueOf(snapshot.getPlane()))
                    .rightColor(Color.WHITE)
                    .build());
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("NPCs")
                    .right(String.valueOf(snapshot.getNpcs().size()))
                    .rightColor(Color.WHITE)
                    .build());
        }

        panelComponent.getChildren().add(LineComponent.builder()
                .left("Depth/Nodes")
                .right(config.searchDepth() + "/" + config.maxSearchNodes())
                .rightColor(Color.YELLOW)
                .build());
        panelComponent.getChildren().add(LineComponent.builder()
                .left("Search us")
                .right(String.valueOf(plugin.getLastSearchMicros()))
                .rightColor(Color.YELLOW)
                .build());

        if (plugin.getLastDecisionResult() != null) {
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Best Action")
                    .right(String.valueOf(plugin.getLastDecisionResult().getBestAction()))
                    .rightColor(new Color(100, 255, 130))
                    .build());
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Score")
                    .right(String.format("%.2f", plugin.getLastDecisionResult().getBestScore()))
                    .rightColor(Color.WHITE)
                    .build());
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Explored")
                    .right(String.valueOf(plugin.getLastDecisionResult().getExploredNodes()))
                    .rightColor(Color.WHITE)
                    .build());
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Threats (root->best)")
                    .right(plugin.getRootThreatCount() + "->" + plugin.getBestThreatCount())
                    .rightColor(plugin.getBestThreatCount() <= plugin.getRootThreatCount() ? Color.GREEN : Color.ORANGE)
                    .build());
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Attack Threats")
                    .right(plugin.getRootAttackThreatCount() + "->" + plugin.getBestAttackThreatCount())
                    .rightColor(plugin.getBestAttackThreatCount() <= plugin.getRootAttackThreatCount() ? Color.GREEN : Color.ORANGE)
                    .build());
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Unprotected")
                    .right(plugin.getRootUnprotectedThreatCount() + "->" + plugin.getBestUnprotectedThreatCount())
                    .rightColor(plugin.getBestUnprotectedThreatCount() <= plugin.getRootUnprotectedThreatCount() ? Color.GREEN : Color.ORANGE)
                    .build());
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Prayer (root->best)")
                    .right(formatPrayer(plugin.getRootRecommendedPrayer()) + "->" + formatPrayer(plugin.getBestRecommendedPrayer()))
                    .rightColor(Color.WHITE)
                    .build());
        }

        SimulationDecisionAdapter.ExecutableAction executableAction = plugin.getLastExecutableAction();
        if (executableAction != null) {
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Move")
                    .right(executableAction.hasMovement() ? String.valueOf(executableAction.getMovementDestination()) : "None")
                    .rightColor(Color.CYAN)
                    .build());
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Interact")
                    .right(executableAction.hasInteraction()
                            ? executableAction.getInteractionAction() + " idx=" + executableAction.getTargetNpcIndex()
                            : "None")
                    .rightColor(Color.CYAN)
                    .build());
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Action Type")
                    .right(String.valueOf(executableAction.getSimulationAction().getType()))
                    .rightColor(Color.CYAN)
                    .build());
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Steps")
                    .right(executableAction.getSteps().stream()
                            .map(step -> step.getType().name())
                            .collect(Collectors.joining(",")))
                    .rightColor(Color.CYAN)
                    .build());
        }

        return super.render(graphics);
    }

    private String formatPrayer(Prayer prayer) {
        return prayer == null ? "None" : prayer.name();
    }
}
