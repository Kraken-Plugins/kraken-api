package com.kraken.api.sim.colosim;

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

        JPanel rootLeft = new JPanel(new BorderLayout(8, 8));
        rootLeft.setOpaque(false);
        rootLeft.setPreferredSize(new Dimension(340, 860));

        JPanel topActions = new JPanel(new GridLayout(2, 2, 8, 8));
        AppTheme.styleCard(topActions);

        JComboBox<SimulationPanel.Tool> toolBox = new JComboBox<SimulationPanel.Tool>(SimulationPanel.Tool.values());
        toolBox.addActionListener(e -> panel.setTool((SimulationPanel.Tool) toolBox.getSelectedItem()));

        JButton stepButton = new JButton("Step");
        JButton autoButton = new JButton("Play");
        JButton resetButton = new JButton("Reset");
        JButton clearButton = new JButton("Clear");
        AppTheme.styleButton(stepButton);
        AppTheme.styleButton(autoButton);
        AppTheme.styleButton(resetButton);
        AppTheme.styleButton(clearButton);

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

        JPanel statusPanel = new JPanel(new BorderLayout(6, 6));
        AppTheme.styleCard(statusPanel);
        JLabel tickLabel = new JLabel("Tick: 0");
        tickLabel.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 18));
        statusPanel.add(tickLabel, BorderLayout.NORTH);

        JTextArea infoArea = new JTextArea(20, 34);
        infoArea.setEditable(false);
        infoArea.setLineWrap(true);
        infoArea.setWrapStyleWord(true);
        infoArea.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        JScrollPane infoScroll = new JScrollPane(infoArea);
        infoScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        infoScroll.getVerticalScrollBar().setUnitIncrement(14);
        statusPanel.add(infoScroll, BorderLayout.CENTER);

        JScrollPane timelineScroll = new JScrollPane(timelinePanel);
        timelineScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        timelineScroll.getVerticalScrollBar().setUnitIncrement(14);
        timelineScroll.setPreferredSize(new Dimension(240, panel.getPreferredSize().height));

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
            } else {
                timer.start();
                autoButton.setText("Pause");
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

        JPanel leftStack = new JPanel();
        leftStack.setOpaque(false);
        leftStack.setLayout(new GridLayout(0, 1, 8, 8));
        leftStack.add(topActions);
        leftStack.add(setupPanel);
        leftStack.add(statusPanel);
        rootLeft.add(leftStack, BorderLayout.NORTH);

        panel.setPlacementNpcType((NpcType) npcTypeBox.getSelectedItem());
        panel.setPlacementManticoreExtra((String) mantiExtraBox.getSelectedItem());
        panel.setStateChangedCallback(() -> refreshUi(simulation, panel, timelinePanel, infoArea, tickLabel));

        JPanel centerWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 12));
        centerWrap.setOpaque(false);
        centerWrap.add(panel);
        centerWrap.add(timelineScroll);

        frame.add(rootLeft, BorderLayout.WEST);
        frame.add(centerWrap, BorderLayout.CENTER);

        bindKeys(frame, simulation, panel, timelinePanel, infoArea, tickLabel);
        refreshUi(simulation, panel, timelinePanel, infoArea, tickLabel);

        frame.pack();
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
        sb.append("Player: (").append(player.x).append(", ").append(player.y).append(")\n");
        sb.append("Mobs: ").append(simulation.getMobs().size()).append('\n');
        sb.append('\n');

        for (int i = 0; i < simulation.getMobs().size(); i++) {
            Mob mob = simulation.getMobs().get(i);
            sb.append("#").append(i).append(" ").append(simulation.getNpcName(mob.type))
                    .append(" | pos=(").append(mob.x).append(",").append(mob.y).append(")")
                    .append(" | cd=").append(mob.cooldown);
            if (mob.extra != null) {
                sb.append(" | extra=").append(mob.extra);
            }
            sb.append('\n');
        }

        int selectedMobIndex = panel.getSelectedMobIndex();
        if (selectedMobIndex >= 0 && selectedMobIndex < simulation.getMobs().size()) {
            Mob mob = simulation.getMobs().get(selectedMobIndex);
            sb.append('\n');
            sb.append("Selected: #").append(selectedMobIndex).append(" ").append(simulation.getNpcName(mob.type)).append('\n');
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
                sb.append(" - #").append(i).append(" ").append(simulation.getNpcName(mob.type)).append(" attacked");
                if (mob.type == Simulation.MANTICORE) {
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
