package plugins.api.overlay;

import com.google.inject.Inject;
import com.kraken.api.Context;
import com.kraken.api.query.player.WildernessInfo;
import net.runelite.api.Client;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;
import plugins.api.ApiTestConfig;
import plugins.api.TestResultManager;
import plugins.api.suite.SuiteProgress;

import java.awt.*;
import java.time.Duration;

public class InfoPanelOverlay extends OverlayPanel {

    private final Client client;
    private final ApiTestConfig config;
    private final TestResultManager testResultManager;
    private final Context ctx;

    @Inject
    public InfoPanelOverlay(Client client, ApiTestConfig config, TestResultManager testResultManager, Context ctx) {
        this.client = client;
        this.config = config;
        this.ctx = ctx;
        this.testResultManager = testResultManager;
        setPosition(OverlayPosition.TOP_RIGHT);
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        try {
            panelComponent.getChildren().clear();

            // Add title
            panelComponent.getChildren().add(TitleComponent.builder()
                    .text("Test Info")
                    .color(Color.CYAN)
                    .build());

            // Add test results if manager is available
            if (!testResultManager.resultsInOrder().isEmpty()) {
                addTestResults();
            }

            // Add debug info if enabled
            if (config.showDebugInfo()) {
                addDebugInfo();
            }

        } catch (Exception e) {
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Error:")
                    .right(e.getMessage())
                    .rightColor(Color.RED)
                    .build());
        }

        return super.render(graphics);
    }

    private void addTestResults() {
        try {
            SuiteProgress progress = testResultManager.getProgress();

            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Test Status:")
                    .right(testResultManager.getOverallStatus())
                    .rightColor(getOverallStatusColor())
                    .build());

            // While a run is going the useful information is where it is up to and what it is doing.
            // A long walk between locations is otherwise indistinguishable from a hang.
            if (progress.isRunning()) {
                panelComponent.getChildren().add(LineComponent.builder()
                        .left("Progress:")
                        .right(progress.describePosition() + "  " + formatElapsed(progress.getElapsed()))
                        .rightColor(Color.YELLOW)
                        .build());

                if (progress.getCurrentPhase() != null) {
                    panelComponent.getChildren().add(LineComponent.builder()
                            .left("  " + truncate(progress.getCurrentPhase(), 34))
                            .build());
                }
            }

            if (progress.getPassed() + progress.getFailed() + progress.getSkipped() > 0) {
                panelComponent.getChildren().add(LineComponent.builder()
                        .left("Results:")
                        .right(String.format("%dP %dF %dS", progress.getPassed(), progress.getFailed(),
                                progress.getSkipped()))
                        .rightColor(progress.getFailed() > 0 ? Color.RED : Color.WHITE)
                        .build());
            }

            // The per-test breakdown lives in the sidebar panel, which is scrollable and clickable.
            // Only failures are worth putting over the game canvas: while a run is going you are
            // watching the player walk, and a thirty row list would bury the one thing that matters.
            for (TestResultManager.TestResult result : testResultManager.resultsInOrder()) {
                if (!result.isFailure()) {
                    continue;
                }

                panelComponent.getChildren().add(LineComponent.builder()
                        .left(result.getTestName())
                        .right("Failed")
                        .rightColor(Color.RED)
                        .build());
            }

        } catch (Exception e) {
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Test Results Error:")
                    .right(e.getMessage())
                    .rightColor(Color.RED)
                    .build());
        }
    }

    /**
     * Shortens a message to fit the overlay.
     *
     * @param text the text to shorten
     * @param maximum the longest acceptable length
     * @return the text, truncated with an ellipsis when too long
     */
    private String truncate(String text, int maximum) {
        return text.length() <= maximum ? text : text.substring(0, maximum - 3) + "...";
    }

    /**
     * Formats a run duration as minutes and seconds.
     *
     * @param elapsed how long the run has been going
     * @return a mm:ss string
     */
    private String formatElapsed(Duration elapsed) {
        long seconds = elapsed == null ? 0 : elapsed.getSeconds();
        return String.format("%d:%02d", seconds / 60, seconds % 60);
    }

    private Color getOverallStatusColor() {
        if (testResultManager.isRunning()) {
            return Color.YELLOW;
        }

        int failed = testResultManager.count(TestResultManager.TestStatus.FAILED);
        int passed = testResultManager.count(TestResultManager.TestStatus.PASSED);

        if (failed > 0) {
            return Color.RED;
        }

        return passed > 0 ? Color.GREEN : Color.WHITE;
    }


    private void addDebugInfo() {
        try {
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("--- Debug ---")
                    .build());

            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Game State:")
                    .right(client.getGameState().toString())
                    .rightColor(Color.YELLOW)
                    .build());

            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Logged In:")
                    .right(client.getLocalPlayer() != null ? "Yes" : "No")
                    .rightColor(client.getLocalPlayer() != null ? Color.GREEN : Color.RED)
                    .build());

            if (client.getLocalPlayer() != null) {
                panelComponent.getChildren().add(LineComponent.builder()
                        .left("Player Name:")
                        .right(client.getLocalPlayer().getName())
                        .rightColor(Color.WHITE)
                        .build());
            }

            WildernessInfo info = ctx.players().local().getWildernessInfo();
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Wilderness Level:")
                    .right(String.format("%d (%d-%d)", info.getLevel(), info.getMinAttackableCombatLevel(), info.getMaxAttackableCombatLevel()))
                    .rightColor(ctx.players().local().getWildernessInfo().getLevel() > 0 ? Color.GREEN : Color.RED)
                    .build());

        } catch (Exception e) {
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Debug Error:")
                    .right(e.getMessage())
                    .rightColor(Color.RED)
                    .build());
        }
    }
}