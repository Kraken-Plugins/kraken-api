package plugins.api.ui;

import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import plugins.api.TestResultManager;
import plugins.api.suite.RegisteredTest;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionListener;

/**
 * One row in the test panel: name, status, duration, and a button to run just that test.
 *
 * <p>Rows are created once and updated in place. Rebuilding the list on every refresh would mean
 * thirty-odd component trees discarded four times a second, and would lose the expanded state of any
 * row whose failure reason you were reading.</p>
 */
class TestRowPanel extends JPanel {

    private final RegisteredTest test;

    private final JLabel nameLabel = new JLabel();
    private final JLabel durationLabel = new JLabel();
    private final JLabel detailLabel = new JLabel();
    private final JButton runButton = new JButton("Run");

    private TestResultManager.TestStatus renderedStatus;
    private String renderedDetail;
    private long renderedDuration = -1;

    /**
     * Builds a row.
     *
     * @param test the test this row represents
     * @param onRun invoked when the row's run button is pressed
     */
    TestRowPanel(RegisteredTest test, ActionListener onRun) {
        this.test = test;

        setLayout(new BorderLayout(4, 0));
        setBackground(ColorScheme.DARKER_GRAY_COLOR);
        setBorder(BorderFactory.createEmptyBorder(3, 6, 3, 6));

        nameLabel.setText(test.getDisplayName());
        nameLabel.setFont(FontManager.getRunescapeSmallFont());
        nameLabel.setForeground(ColorScheme.TEXT_COLOR);
        nameLabel.setToolTipText(test.getId());

        durationLabel.setFont(FontManager.getRunescapeSmallFont());
        durationLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);

        runButton.setFont(FontManager.getRunescapeSmallFont());
        runButton.setFocusPainted(false);
        runButton.setMargin(new java.awt.Insets(0, 4, 0, 4));
        runButton.setToolTipText("Run " + test.getDisplayName() + " on its own");
        runButton.addActionListener(onRun);

        detailLabel.setFont(FontManager.getRunescapeSmallFont());
        detailLabel.setVisible(false);
        detailLabel.setBorder(BorderFactory.createEmptyBorder(1, 6, 2, 0));

        JPanel trailing = new JPanel(new BorderLayout(4, 0));
        trailing.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        trailing.add(durationLabel, BorderLayout.CENTER);
        trailing.add(runButton, BorderLayout.EAST);

        add(nameLabel, BorderLayout.CENTER);
        add(trailing, BorderLayout.EAST);
        add(detailLabel, BorderLayout.SOUTH);
    }

    /**
     * Applies a result to this row, doing nothing when nothing visible has changed.
     *
     * @param result the latest result for this test
     * @param runInProgress whether a run is active, which disables per-test run buttons
     */
    void update(TestResultManager.TestResult result, boolean runInProgress) {
        runButton.setEnabled(!runInProgress);

        TestResultManager.TestStatus status = result.getStatus();
        String detail = detailFor(result);
        long duration = result.getExecutionTimeMs();

        if (status == renderedStatus && java.util.Objects.equals(detail, renderedDetail)
                && duration == renderedDuration) {
            return;
        }

        renderedStatus = status;
        renderedDetail = detail;
        renderedDuration = duration;

        nameLabel.setForeground(colourFor(status));
        durationLabel.setText(duration > 0 ? formatDuration(duration) : "");

        if (detail == null) {
            detailLabel.setVisible(false);
        } else {
            // Wrapped in HTML so a long reason spans lines instead of being clipped at the panel edge.
            detailLabel.setText("<html><body style='width:150px'>" + escape(detail) + "</body></html>");
            detailLabel.setForeground(status == TestResultManager.TestStatus.FAILED
                    ? ColorScheme.PROGRESS_ERROR_COLOR : ColorScheme.BRAND_ORANGE);
            detailLabel.setVisible(true);
        }

        revalidate();
        repaint();
    }

    /**
     * The registered test this row shows.
     *
     * @return the test
     */
    RegisteredTest getTest() {
        return test;
    }

    /**
     * Chooses which message, if any, to show beneath the row.
     *
     * <p>A skip reason is always shown: it is the difference between "your bank is missing an item"
     * and "the API broke", and hiding it behind a debug toggle is what made the old overlay so hard
     * to act on.</p>
     *
     * @param result the result to describe
     * @return the message, or null when there is nothing worth saying
     */
    private String detailFor(TestResultManager.TestResult result) {
        if (result.getStatus() != TestResultManager.TestStatus.SKIPPED
                && result.getStatus() != TestResultManager.TestStatus.FAILED) {
            return null;
        }
        return result.getMessage();
    }

    /**
     * Maps a status to its row colour.
     *
     * @param status the status to colour
     * @return the colour for the test name
     */
    private Color colourFor(TestResultManager.TestStatus status) {
        switch (status) {
            case PASSED:
                return ColorScheme.PROGRESS_COMPLETE_COLOR;
            case FAILED:
                return ColorScheme.PROGRESS_ERROR_COLOR;
            case RUNNING:
                return ColorScheme.PROGRESS_INPROGRESS_COLOR;
            case SKIPPED:
                // Not red on purpose: a skip means the environment was not ready, not that the API
                // regressed, and colouring them the same makes a run impossible to read at a glance.
                return ColorScheme.BRAND_ORANGE;
            case CANCELLED:
                return ColorScheme.LIGHT_GRAY_COLOR;
            default:
                return ColorScheme.TEXT_COLOR;
        }
    }

    /**
     * Formats a duration compactly.
     *
     * @param millis how long the test took
     * @return a short duration string
     */
    private String formatDuration(long millis) {
        return millis < 1000 ? millis + "ms" : String.format("%.1fs", millis / 1000.0);
    }

    /**
     * Escapes the few characters that would otherwise be read as markup by the HTML label.
     *
     * @param text the raw message
     * @return the escaped message
     */
    private String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
    }
}
