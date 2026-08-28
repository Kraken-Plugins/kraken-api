package colosim;

import colosim.model.Mob;
import colosim.model.Tile;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

public class ColoSimApp {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            AppTheme.install();
            new ColoSimApp().start();
        });
    }

    private void start() {
        final Simulation simulation = new Simulation();
        final SimulationPanel panel = new SimulationPanel(simulation);
        final TimelinePanel timelinePanel = new TimelinePanel(simulation);

        JFrame frame = new JFrame("OSRS Colosseum Java Simulator");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout(12, 12));
        frame.getContentPane().setBackground(AppTheme.BG);

        // --- Left Sidebar (Tools) ---
        JPanel rootLeft = new JPanel(new BorderLayout(8, 8));
        rootLeft.setOpaque(false);
        rootLeft.setPreferredSize(new Dimension(300, 860));

        JPanel topActions = new JPanel(new GridLayout(1, 4, 6, 6));
        AppTheme.styleCard(topActions);

        JComboBox<SimulationPanel.Tool> toolBox = new JComboBox<SimulationPanel.Tool>(SimulationPanel.Tool.values());
        toolBox.addActionListener(e -> panel.setTool((SimulationPanel.Tool) toolBox.getSelectedItem()));

        JButton stepButton = new JButton("Step");
        JButton autoButton = new JButton("Play");
        JButton resetButton = new JButton("Reset");
        JButton clearButton = new JButton("Clear");
        final Color stepColor = new Color(26, 115, 232);
        final Color playColor = new Color(34, 163, 74);
        final Color playActiveColor = new Color(24, 127, 58);
        final Color resetColor = new Color(239, 132, 32);
        final Color clearColor = new Color(220, 53, 69);
        AppTheme.styleActionButton(stepButton, stepColor);
        AppTheme.styleActionButton(autoButton, playColor);
        AppTheme.styleActionButton(resetButton, resetColor);
        AppTheme.styleActionButton(clearButton, clearColor);

        topActions.add(stepButton);
        topActions.add(autoButton);
        topActions.add(resetButton);
        topActions.add(clearButton);

        JPanel setupPanel = new JPanel(new GridLayout(0, 1, 6, 6));
        AppTheme.styleCard(setupPanel);

        List<NpcType> npcTypes = new ArrayList<NpcType>();
        for (NpcType value : NpcType.values()) {
            if (value != NpcType.PLAYER) {
                npcTypes.add(value);
            }
        }
        JComboBox<NpcType> npcTypeBox = new JComboBox<NpcType>(new DefaultComboBoxModel<NpcType>(npcTypes.toArray(new NpcType[0])));
        npcTypeBox.addActionListener(e -> panel.setPlacementNpcType((NpcType) npcTypeBox.getSelectedItem()));

        String[] mantiExtras = {"u", "ur", "um", "r", "m", "Mrm", "Mmr", "rMm", "mMr", "uMrm", "uMmr", "urMm", "umMr"};
        JComboBox<String> mantiExtraBox = new JComboBox<String>(mantiExtras);
        mantiExtraBox.addActionListener(e -> panel.setPlacementManticoreExtra((String) mantiExtraBox.getSelectedItem()));

        JCheckBox fromWaveStart = new JCheckBox("From Wave Start");
        fromWaveStart.addActionListener(e -> simulation.setFromWaveStart(fromWaveStart.isSelected()));
        JCheckBox mantimayhem3 = new JCheckBox("Mantimayhem 3");
        mantimayhem3.addActionListener(e -> simulation.setMantimayhem3(mantimayhem3.isSelected()));
        JCheckBox showPlayerLos = new JCheckBox("Show Player LoS", true);
        showPlayerLos.addActionListener(e -> panel.setShowPlayerLos(showPlayerLos.isSelected()));
        JCheckBox showVenator = new JCheckBox("Show Venator Bounce", true);
        showVenator.addActionListener(e -> panel.setShowVenatorBounce(showVenator.isSelected()));

        setupPanel.add(new JLabel("Tool"));
        setupPanel.add(toolBox);
        setupPanel.add(new JLabel("NPC Type"));
        setupPanel.add(npcTypeBox);
        setupPanel.add(new JLabel("Manticore Extra"));
        setupPanel.add(mantiExtraBox);
        setupPanel.add(new JSeparator());
        setupPanel.add(fromWaveStart);
        setupPanel.add(mantimayhem3);
        setupPanel.add(showPlayerLos);
        setupPanel.add(showVenator);

        JPanel leftStack = new JPanel(new GridBagLayout());
        leftStack.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTH;
        gbc.insets = new Insets(0, 0, 8, 0);

        leftStack.add(topActions, gbc);

        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 0, 0);
        leftStack.add(setupPanel, gbc);

        gbc.gridy = 2;
        gbc.weighty = 1.0;
        JPanel spacer = new JPanel();
        spacer.setOpaque(false);
        leftStack.add(spacer, gbc);

        rootLeft.add(leftStack, BorderLayout.CENTER);

        // --- Status / Console Panel ---
        JPanel statusPanel = new JPanel(new BorderLayout(6, 6));
        statusPanel.setOpaque(true);
        statusPanel.setBackground(new Color(27, 33, 39));
        statusPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(53, 62, 74), 1, true),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        statusPanel.setPreferredSize(new Dimension(0, 220));
        JLabel tickLabel = new JLabel("Tick: 0");
        tickLabel.setForeground(new Color(205, 216, 229));
        tickLabel.setFont(new Font("Consolas", Font.BOLD, 14));
        statusPanel.add(tickLabel, BorderLayout.NORTH);

        JTextArea infoArea = new JTextArea(10, 120);
        infoArea.setEditable(false);
        infoArea.setLineWrap(false);
        infoArea.setWrapStyleWord(false);
        infoArea.setBackground(new Color(27, 33, 39));
        infoArea.setForeground(new Color(210, 219, 230));
        infoArea.setCaretColor(new Color(210, 219, 230));
        infoArea.setFont(new Font("Consolas", Font.PLAIN, 13));
        JScrollPane infoScroll = new JScrollPane(infoArea);
        infoScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        infoScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        infoScroll.setBorder(BorderFactory.createEmptyBorder());
        infoScroll.getViewport().setBackground(new Color(27, 33, 39));
        infoScroll.getVerticalScrollBar().setUnitIncrement(14);
        statusPanel.add(infoScroll, BorderLayout.CENTER);

        JScrollPane timelineScroll = new JScrollPane(timelinePanel);
        timelineScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        timelineScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        timelineScroll.setPreferredSize(new Dimension(270, panel.getPreferredSize().height));
        timelineScroll.setBorder(BorderFactory.createEmptyBorder());
        AppTheme.styleCleanScrollBars(timelineScroll);

        Timer timer = new Timer(600, e -> {
            simulation.step();
            refreshUi(simulation, panel, timelinePanel, infoArea, tickLabel);
        });

        stepButton.addActionListener(e -> {
            simulation.step();
            refreshUi(simulation, panel, timelinePanel, infoArea, tickLabel);
        });

        autoButton.addActionListener(e -> {
            if (timer.isRunning()) {
                timer.stop();
                autoButton.setText("Play");
                autoButton.setBackground(playColor);
            } else {
                timer.start();
                autoButton.setText("Pause");
                autoButton.setBackground(playActiveColor);
            }
        });

        resetButton.addActionListener(e -> {
            simulation.reset();
            refreshUi(simulation, panel, timelinePanel, infoArea, tickLabel);
        });

        clearButton.addActionListener(e -> {
            simulation.clear();
            refreshUi(simulation, panel, timelinePanel, infoArea, tickLabel);
        });

        panel.setPlacementNpcType((NpcType) npcTypeBox.getSelectedItem());
        panel.setPlacementManticoreExtra((String) mantiExtraBox.getSelectedItem());
        panel.setStateChangedCallback(() -> refreshUi(simulation, panel, timelinePanel, infoArea, tickLabel));

        // --- Center Layout ---
        JScrollPane boardScroll = new JScrollPane(panel);
        boardScroll.setBorder(BorderFactory.createEmptyBorder());
        boardScroll.getViewport().setBackground(AppTheme.BG);
        // If your AppTheme method supports it, you can style these scrollbars too!
        // AppTheme.styleCleanScrollBars(boardScroll);

        // 2. Set strict minimum sizes so they never disappear
        boardScroll.setMinimumSize(new Dimension(400, 400));
        timelineScroll.setMinimumSize(new Dimension(200, 400));

        // 3. Use a JSplitPane instead of GridBagLayout
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, boardScroll, timelineScroll);
        splitPane.setBorder(BorderFactory.createEmptyBorder());
        splitPane.setOpaque(false);
        splitPane.setDividerSize(5); // Clean, thin draggable divider
        splitPane.setContinuousLayout(true);
        splitPane.setResizeWeight(1.0); // 1.0 means the left side (board) gets the extra space when maximizing

        // --- Main Frame Composition ---
        frame.add(rootLeft, BorderLayout.WEST);
        frame.add(splitPane, BorderLayout.CENTER); // Add the splitPane here instead of centerWrap
        frame.add(statusPanel, BorderLayout.SOUTH);

        // Pushing the status panel to the JFrame's SOUTH region allows it to
        // stretch horizontally across the entire application width
        frame.add(statusPanel, BorderLayout.SOUTH);

        bindKeys(frame, simulation, panel, timelinePanel, infoArea, tickLabel);
        refreshUi(simulation, panel, timelinePanel, infoArea, tickLabel);

        frame.pack();
        frame.setMinimumSize(new Dimension(1500, 900));
        frame.setSize(new Dimension(
                Math.max(frame.getWidth(), 1680),
                Math.max(frame.getHeight(), 980)
        ));
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private void bindKeys(JFrame frame, Simulation simulation, SimulationPanel panel, TimelinePanel timelinePanel, JTextArea infoArea, JLabel tickLabel) {
        frame.getRootPane().getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), "step");
        frame.getRootPane().getActionMap().put("step", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                simulation.step();
                refreshUi(simulation, panel, timelinePanel, infoArea, tickLabel);
            }
        });
        frame.getRootPane().getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "reset");
        frame.getRootPane().getActionMap().put("reset", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                simulation.reset();
                refreshUi(simulation, panel, timelinePanel, infoArea, tickLabel);
            }
        });
        frame.getRootPane().getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, InputEvent.SHIFT_DOWN_MASK), "clear");
        frame.getRootPane().getActionMap().put("clear", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                simulation.clear();
                refreshUi(simulation, panel, timelinePanel, infoArea, tickLabel);
            }
        });
    }

    private void refreshUi(Simulation simulation, SimulationPanel panel, TimelinePanel timelinePanel, JTextArea infoArea, JLabel tickLabel) {
        panel.repaint();
        timelinePanel.repaint();
        refreshInfo(simulation, panel, infoArea, tickLabel);
    }

    private void refreshInfo(Simulation simulation, SimulationPanel panel, JTextArea infoArea, JLabel tickLabel) {
        StringBuilder sb = new StringBuilder();
        Tile player = simulation.getPlayer();

        tickLabel.setText("Tick: " + simulation.getTickCount());
        sb.append("Player: (").append(player.getX()).append(", ").append(player.getY()).append(")\n");
        sb.append("Mobs: ").append(simulation.getMobs().size()).append('\n');
        sb.append('\n');

        for (int i = 0; i < simulation.getMobs().size(); i++) {
            Mob mob = simulation.getMobs().get(i);
            sb.append("#").append(i).append(" ").append(simulation.getNpcName(mob.getType()))
                    .append(" | pos=(").append(mob.getX()).append(",").append(mob.getY()).append(")")
                    .append(" | cd=").append(mob.getCooldown());
            if (mob.getExtra() != null) {
                sb.append(" | extra=").append(mob.getExtra());
            }
            sb.append('\n');
        }

        int selectedMobIndex = panel.getSelectedMobIndex();
        if (selectedMobIndex >= 0 && selectedMobIndex < simulation.getMobs().size()) {
            Mob mob = simulation.getMobs().get(selectedMobIndex);
            sb.append('\n');
            sb.append("Selected: #").append(selectedMobIndex).append(" ").append(simulation.getNpcName(mob.getType())).append('\n');
            sb.append("Can attack player: ").append(simulation.canAttackPlayer(mob)).append('\n');
        }

        if (!simulation.getTape().isEmpty()) {
            int[] last = simulation.getTape().get(simulation.getTape().size() - 1);
            sb.append('\n');
            sb.append("Last Tick Attacks:\n");
            for (int i = 0; i < last.length && i < simulation.getMobs().size(); i++) {
                int value = last[i];
                int attacked = value & 0xff;
                if (attacked == 0) {
                    continue;
                }
                Mob mob = simulation.getMobs().get(i);
                sb.append(" - #").append(i).append(" ").append(simulation.getNpcName(mob.getType())).append(" attacked");
                if (mob.getType() == Simulation.MANTICORE) {
                    int style = (value >> 8) & 0xff;
                    sb.append(" [").append(simulation.decodeManticoreAttack(style)).append("]");
                }
                sb.append('\n');
            }
        }

        infoArea.setText(sb.toString());
        infoArea.setCaretPosition(0);
    }
}