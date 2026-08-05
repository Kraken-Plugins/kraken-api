package plugins.api.ui;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.DynamicGridLayout;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import plugins.api.TestResultManager;
import plugins.api.suite.RegisteredTest;
import plugins.api.suite.SuiteOptions;
import plugins.api.suite.SuiteProgress;
import plugins.api.suite.TestGroup;
import plugins.api.suite.TestRegistry;
import plugins.api.suite.TestRunner;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Sidebar panel for running the in-client test suite and reading its results.
 *
 * <p>Thirty-odd tests do not fit in a corner overlay, and the overlay cannot be clicked. This gives
 * the run a control surface: start, stop, re-run what failed, and see why anything was skipped.</p>
 *
 * <p>State is polled on a Swing {@link Timer} rather than pushed from the runner. Pushing would mean
 * updates arriving from the runner's worker thread and the client thread, each needing
 * {@code invokeLater}, and a fast run would flood the event queue with redundant repaints. Polling
 * keeps every read on the event dispatch thread, in one place, at a bounded rate — and because
 * {@code PluginPanel} is {@code Activatable}, the timer only runs while the panel is actually
 * visible, so a hidden panel costs nothing.</p>
 */
@Slf4j
public class ApiTestPanel extends PluginPanel {

    /** How often the panel re-reads run state. Four times a second reads as live without churn. */
    private static final int REFRESH_INTERVAL_MS = 250;

    private final TestRegistry registry;
    private final TestRunner runner;
    private final TestResultManager results;
    private final Supplier<SuiteOptions> optionsSupplier;

    private final JPanel listPanel = new JPanel(new DynamicGridLayout(0, 1, 0, 2));
    private final Map<String, TestRowPanel> rows = new LinkedHashMap<>();

    private final JComboBox<String> groupFilter = new JComboBox<>();
    private final JButton runAllButton = new JButton("Run All");
    private final JButton stopButton = new JButton("Stop");
    private final JButton rerunFailedButton = new JButton("Re-run Failed");
    private final JButton clearButton = new JButton("Clear");

    private final JLabel progressLabel = new JLabel();
    private final JLabel countsLabel = new JLabel();
    private final JLabel phaseLabel = new JLabel();

    private final Timer refreshTimer = new Timer(REFRESH_INTERVAL_MS, event -> refresh());

    private int renderedTestCount = -1;

    /**
     * Builds the panel.
     *
     * @param registry the test catalogue
     * @param runner the runner to drive
     * @param results where run state is read from
     * @param optionsSupplier supplies run options from current plugin settings
     */
    public ApiTestPanel(TestRegistry registry, TestRunner runner, TestResultManager results,
                        Supplier<SuiteOptions> optionsSupplier) {
        // false: the no-arg constructor wraps the whole panel in a scroll pane, which would scroll the
        // header and footer away once there are thirty rows.
        super(false);

        this.registry = registry;
        this.runner = runner;
        this.results = results;
        this.optionsSupplier = optionsSupplier;

        setLayout(new BorderLayout());
        setBackground(ColorScheme.DARK_GRAY_COLOR);
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        add(buildHeader(), BorderLayout.NORTH);
        add(buildList(), BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);

        refresh();
    }

    @Override
    public void onActivate() {
        refresh();
        refreshTimer.start();
    }

    @Override
    public void onDeactivate() {
        refreshTimer.stop();
    }

    /**
     * Builds the sticky header: title, controls, and the group filter.
     *
     * @return the header panel
     */
    private JPanel buildHeader() {
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBackground(ColorScheme.DARK_GRAY_COLOR);

        JLabel title = new JLabel("API Tests");
        title.setFont(FontManager.getRunescapeBoldFont());
        title.setForeground(ColorScheme.BRAND_ORANGE);
        title.setAlignmentX(LEFT_ALIGNMENT);
        header.add(title);
        header.add(Box.createVerticalStrut(6));

        JPanel buttons = new JPanel(new GridLayout(2, 2, 4, 4));
        buttons.setBackground(ColorScheme.DARK_GRAY_COLOR);
        buttons.add(runAllButton);
        buttons.add(stopButton);
        buttons.add(rerunFailedButton);
        buttons.add(clearButton);
        buttons.setAlignmentX(LEFT_ALIGNMENT);
        header.add(buttons);
        header.add(Box.createVerticalStrut(6));

        runAllButton.setToolTipText("Run every test, ordered to minimise walking. Start at the hub bank.");
        stopButton.setToolTipText("Cancel the run in progress");
        rerunFailedButton.setToolTipText("Re-run only the tests that failed");
        clearButton.setToolTipText("Clear all recorded results");

        // Button handlers run on the event dispatch thread and must return immediately: the runner
        // hands the work to its own worker, so nothing here blocks the UI.
        runAllButton.addActionListener(event -> runSelectedGroup());
        stopButton.addActionListener(event -> runner.cancel());
        rerunFailedButton.addActionListener(event -> rerunFailed());
        clearButton.addActionListener(event -> {
            results.clearAllResults();
            refresh();
        });

        groupFilter.addItem("All tests");
        for (TestGroup group : TestGroup.values()) {
            groupFilter.addItem(group.getDisplayName());
        }
        groupFilter.setAlignmentX(LEFT_ALIGNMENT);
        groupFilter.setToolTipText("Limit Run All, and the list below, to one category");
        groupFilter.addActionListener(event -> {
            applyFilter();
            refresh();
        });
        header.add(groupFilter);
        header.add(Box.createVerticalStrut(6));

        return header;
    }

    /**
     * Builds the scrolling list of test rows.
     *
     * @return the scroll pane wrapping the list
     */
    private JScrollPane buildList() {
        listPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(ColorScheme.DARK_GRAY_COLOR);
        wrapper.add(listPanel, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(wrapper,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBackground(ColorScheme.DARK_GRAY_COLOR);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setPreferredSize(new Dimension(PANEL_WIDTH, 380));
        return scroll;
    }

    /**
     * Builds the sticky footer showing run position, counts and the current phase.
     *
     * @return the footer panel
     */
    private JPanel buildFooter() {
        JPanel footer = new JPanel(new GridLayout(3, 1));
        footer.setBackground(ColorScheme.DARK_GRAY_COLOR);
        footer.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));

        for (JLabel label : new JLabel[]{progressLabel, countsLabel, phaseLabel}) {
            label.setFont(FontManager.getRunescapeSmallFont());
            label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
            footer.add(label);
        }

        return footer;
    }

    /**
     * Creates a row per registered test, grouped under category headings.
     *
     * <p>Deferred until the registry has been initialised, which happens in the plugin's
     * {@code startUp()}, and rebuilt only if the catalogue size changes.</p>
     */
    private void rebuildRows() {
        listPanel.removeAll();
        rows.clear();

        TestGroup previousGroup = null;
        for (RegisteredTest test : registry.all()) {
            if (test.getGroup() != previousGroup) {
                listPanel.add(buildCategoryHeader(test.getGroup()));
                previousGroup = test.getGroup();
            }

            TestRowPanel row = new TestRowPanel(test,
                    event -> runner.runSingle(test, optionsSupplier.get()));
            rows.put(test.getId(), row);
            listPanel.add(row);
        }

        renderedTestCount = rows.size();
        applyFilter();
        listPanel.revalidate();
        listPanel.repaint();
    }

    /**
     * Builds a category heading label.
     *
     * @param group the category being introduced
     * @return the heading label
     */
    private JLabel buildCategoryHeader(TestGroup group) {
        JLabel header = new JLabel(group.getDisplayName().toUpperCase());
        header.setFont(FontManager.getRunescapeSmallFont());
        header.setForeground(ColorScheme.BRAND_ORANGE);
        header.setBorder(BorderFactory.createEmptyBorder(6, 2, 2, 0));
        return header;
    }

    /**
     * Re-reads run state and updates every row and the footer.
     *
     * <p>Always called on the event dispatch thread, either from the timer or from a button handler.
     * Rows early-return when nothing they display has changed, so a steady state tick touches no
     * components.</p>
     *
     * <p>The row check compares against {@link TestRegistry#size()} rather than the length of
     * {@code all()}, which would allocate a defensive copy of the whole catalogue on every tick. The
     * sentinel {@code -1} start value makes the first call rebuild without needing a separate
     * condition, and it still picks up the catalogue appearing later, since the panel is constructed
     * before the registry is populated on a fresh profile.</p>
     */
    private void refresh() {
        if (renderedTestCount != registry.size()) {
            rebuildRows();
        }

        boolean running = runner.isRunning();

        for (TestResultManager.TestResult result : results.resultsInOrder()) {
            TestRowPanel row = rows.get(result.getTestId());
            if (row != null) {
                row.update(result, running);
            }
        }

        runAllButton.setEnabled(!running);
        rerunFailedButton.setEnabled(!running);
        clearButton.setEnabled(!running);
        stopButton.setEnabled(running);

        updateFooter(results.getProgress(), running);
    }

    /**
     * Updates the footer labels.
     *
     * @param progress the current run snapshot
     * @param running whether a run is active
     */
    private void updateFooter(SuiteProgress progress, boolean running) {
        if (running) {
            progressLabel.setText(progress.describePosition() + "   " + formatElapsed(progress.getElapsed()));
            progressLabel.setForeground(ColorScheme.PROGRESS_INPROGRESS_COLOR);
        } else {
            progressLabel.setText(results.getOverallStatus());
            progressLabel.setForeground(progress.getFailed() > 0
                    ? ColorScheme.PROGRESS_ERROR_COLOR : ColorScheme.LIGHT_GRAY_COLOR);
        }

        countsLabel.setText(String.format("%d passed   %d failed   %d skipped",
                progress.getPassed(), progress.getFailed(), progress.getSkipped()));

        String phase = progress.getCurrentPhase();
        phaseLabel.setText(running && phase != null ? phase : " ");
        phaseLabel.setToolTipText(phase);
    }

    /**
     * Starts a run of whatever the group filter currently selects.
     */
    private void runSelectedGroup() {
        TestGroup selected = selectedGroup();
        if (selected == null) {
            runner.runAll(optionsSupplier.get());
        } else {
            runner.runGroup(selected, optionsSupplier.get());
        }
        refresh();
    }

    /**
     * Re-runs only the tests that failed.
     *
     * <p>Skips are excluded deliberately: a skipped test needs the environment fixing, and re-running
     * it unchanged would simply skip again.</p>
     */
    private void rerunFailed() {
        List<RegisteredTest> failed = new ArrayList<>();

        for (TestResultManager.TestResult result : results.resultsInOrder()) {
            if (result.isFailure()) {
                registry.byId(result.getTestId()).ifPresent(failed::add);
            }
        }

        if (failed.isEmpty()) {
            log.info("Nothing to re-run: no failures recorded");
            return;
        }

        runner.runSelection(failed, optionsSupplier.get().toBuilder().includeDestructive(true).build());
        refresh();
    }

    /**
     * Shows or hides rows to match the group filter.
     */
    private void applyFilter() {
        TestGroup selected = selectedGroup();

        for (TestRowPanel row : rows.values()) {
            row.setVisible(selected == null || row.getTest().getGroup() == selected);
        }

        listPanel.revalidate();
        listPanel.repaint();
    }

    /**
     * The category currently chosen in the filter.
     *
     * @return the selected group, or null when "All tests" is chosen
     */
    private TestGroup selectedGroup() {
        int index = groupFilter.getSelectedIndex();
        return index <= 0 ? null : TestGroup.values()[index - 1];
    }

    /**
     * Formats a run duration.
     *
     * @param elapsed how long the run has been going
     * @return a mm:ss string
     */
    private String formatElapsed(Duration elapsed) {
        long seconds = elapsed == null ? 0 : elapsed.getSeconds();
        return String.format("%d:%02d", seconds / 60, seconds % 60);
    }
}
