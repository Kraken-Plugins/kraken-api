package plugins.colosseum;

import com.google.inject.Inject;
import com.kraken.api.simulation.colosseum.ColoNpcType;
import com.kraken.api.simulation.colosseum.ColoState;
import com.kraken.api.simulation.colosseum.ColoTick;
import com.kraken.api.simulation.colosseum.live.ColoCapture;
import com.kraken.api.simulation.colosseum.plan.ColoDecision;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;

/**
 * Panel overlay: the decision's reasoning, planner statistics, predicted outcomes and
 * per-manticore charge states - the "why did it do that" view.
 */
public class ColosseumInfoOverlay extends OverlayPanel {
    private static final Color TITLE = new Color(255, 180, 60);
    private static final Color WARN = new Color(255, 90, 80);
    private static final Color OK = new Color(120, 230, 120);

    private final ColosseumPlannerPlugin plugin;
    private final ColosseumPlannerConfig config;

    @Inject
    public ColosseumInfoOverlay(ColosseumPlannerPlugin plugin, ColosseumPlannerConfig config) {
        this.plugin = plugin;
        this.config = config;
        setPosition(OverlayPosition.TOP_LEFT);
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        if (!config.enabled() || !config.showInfoPanel()) {
            return null;
        }
        panelComponent.getChildren().clear();
        panelComponent.getChildren().add(TitleComponent.builder()
                .text("Auto Colosseum")
                .color(TITLE)
                .build());

        if (plugin.getLastError() != null) {
            addLine("Error", plugin.getLastError(), WARN);
            return super.render(graphics);
        }

        ColoCapture capture = plugin.getLastCapture();
        ColoDecision decision = plugin.getLastDecision();
        if (capture == null || decision == null) {
            addLine("State", "waiting for wave", Color.LIGHT_GRAY);
            return super.render(graphics);
        }

        ColoState state = capture.getState();
        addLine("Wave tick", Integer.toString(state.tick()), Color.WHITE);
        addLine("Plan", decision.getReasoning(), Color.WHITE);
        addLine("Score", String.format("%.0f", decision.getScore()), Color.WHITE);
        addLine("Rollouts", decision.getRollouts() + " in " + decision.getElapsedNanos() / 1_000 + "us",
                decision.getElapsedNanos() < 18_000_000 ? OK : WARN);
        addLine("HP now/+h", state.playerHp() + " / " + decision.getPredictedHpAtHorizon(), Color.WHITE);
        addLine("Worst floor", Integer.toString(decision.getPredictedWorstCaseFloor()),
                decision.getPredictedWorstCaseFloor() > 0 ? OK : WARN);
        addLine("Dmg in / kills", decision.getPredictedDamageTaken() + " / " + decision.getPredictedKills(), Color.WHITE);
        addLine("Supplies", "food " + state.foodCount() + " combo " + state.comboCount()
                + " brew " + state.brewDoses() + " rest " + state.restoreDoses(), Color.WHITE);

        for (int slot = 0; slot < capture.getFrame().getNpcSlotCount(); slot++) {
            if (!state.npcActive(slot) || capture.getFrame().type(slot) != ColoNpcType.MANTICORE) {
                continue;
            }
            String patternText;
            switch (state.manticorePattern(slot)) {
                case ColoTick.PATTERN_RANGED_FIRST:
                    patternText = "range-first";
                    break;
                case ColoTick.PATTERN_MAGIC_FIRST:
                    patternText = "mage-first";
                    break;
                case ColoTick.PATTERN_UNKNOWN:
                    patternText = "unknown";
                    break;
                default:
                    patternText = "mm3-" + state.manticorePattern(slot);
                    break;
            }
            String chargeText = state.npcChargingStarted(slot)
                    ? (state.manticoreOrbsRemaining(slot) > 0
                    ? "firing (" + state.manticoreOrbsRemaining(slot) + " orbs left)"
                    : "cd " + Math.max(0, state.npcCooldown(slot)))
                    : "uncharged";
            addLine("Manticore#" + slot, patternText + ", " + chargeText,
                    state.manticorePattern(slot) == ColoTick.PATTERN_UNKNOWN ? WARN : Color.WHITE);
        }

        return super.render(graphics);
    }

    private void addLine(String left, String right, Color color) {
        panelComponent.getChildren().add(LineComponent.builder()
                .left(left)
                .right(right == null ? "" : right)
                .rightColor(color)
                .build());
    }
}
