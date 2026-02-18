package com.kraken.api.sim.colosim;

import com.kraken.api.sim.colosim.model.Mob;
import com.kraken.api.sim.colosim.model.NpcType;
import com.kraken.api.sim.colosim.model.Tile;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

public class ColoSimApp {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ColoSimApp().start());
    }

    private void start() {
        final Simulation simulation = new Simulation();
        final SimulationPanel panel = new SimulationPanel(simulation);

        JFrame frame = new JFrame("OSRS Colosseum Java Simulator");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout(8, 8));

        JPanel controls = new JPanel();
        controls.setLayout(new BoxLayout(controls, BoxLayout.Y_AXIS));
        controls.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        controls.setPreferredSize(new Dimension(260, 800));

        JComboBox<SimulationPanel.Tool> toolBox = new JComboBox<SimulationPanel.Tool>(SimulationPanel.Tool.values());
        toolBox.addActionListener(e -> panel.setTool((SimulationPanel.Tool) toolBox.getSelectedItem()));

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

        JLabel tickLabel = new JLabel("Tick: 0");

        JTextArea infoArea = new JTextArea(18, 30);
        infoArea.setEditable(false);
        infoArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane infoScroll = new JScrollPane(infoArea);

        Timer timer = new Timer(600, e -> {
            simulation.step();
            panel.repaint();
            refreshInfo(simulation, panel, infoArea, tickLabel);
        });

        JButton stepButton = new JButton("Step");
        stepButton.addActionListener(e -> {
            simulation.step();
            panel.repaint();
            refreshInfo(simulation, panel, infoArea, tickLabel);
        });

        JButton autoButton = new JButton("Play");
        autoButton.addActionListener(e -> {
            if (timer.isRunning()) {
                timer.stop();
                autoButton.setText("Play");
            } else {
                timer.start();
                autoButton.setText("Pause");
            }
        });

        JButton resetButton = new JButton("Reset");
        resetButton.addActionListener(e -> {
            simulation.reset();
            panel.repaint();
            refreshInfo(simulation, panel, infoArea, tickLabel);
        });

        JButton clearButton = new JButton("Clear");
        clearButton.addActionListener(e -> {
            simulation.clear();
            panel.repaint();
            refreshInfo(simulation, panel, infoArea, tickLabel);
        });

        controls.add(new JLabel("Tool"));
        controls.add(toolBox);
        controls.add(new JLabel("NPC Type"));
        controls.add(npcTypeBox);
        controls.add(new JLabel("Manticore Extra"));
        controls.add(mantiExtraBox);
        controls.add(fromWaveStart);
        controls.add(mantimayhem3);
        controls.add(showPlayerLos);
        controls.add(showVenator);
        controls.add(new JLabel(" "));
        controls.add(stepButton);
        controls.add(autoButton);
        controls.add(resetButton);
        controls.add(clearButton);
        controls.add(new JLabel(" "));
        controls.add(tickLabel);
        controls.add(new JLabel(" "));
        controls.add(infoScroll);

        panel.setPlacementNpcType((NpcType) npcTypeBox.getSelectedItem());
        panel.setPlacementManticoreExtra((String) mantiExtraBox.getSelectedItem());
        panel.setStateChangedCallback(() -> refreshInfo(simulation, panel, infoArea, tickLabel));

        frame.add(controls, BorderLayout.WEST);
        frame.add(panel, BorderLayout.CENTER);

        bindKeys(frame, simulation, panel, infoArea, tickLabel);
        refreshInfo(simulation, panel, infoArea, tickLabel);

        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private void bindKeys(JFrame frame, Simulation simulation, SimulationPanel panel, JTextArea infoArea, JLabel tickLabel) {
        frame.getRootPane().getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), "step");
        frame.getRootPane().getActionMap().put("step", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                simulation.step();
                panel.repaint();
                refreshInfo(simulation, panel, infoArea, tickLabel);
            }
        });
        frame.getRootPane().getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "reset");
        frame.getRootPane().getActionMap().put("reset", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                simulation.reset();
                panel.repaint();
                refreshInfo(simulation, panel, infoArea, tickLabel);
            }
        });
        frame.getRootPane().getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, InputEvent.SHIFT_DOWN_MASK), "clear");
        frame.getRootPane().getActionMap().put("clear", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                simulation.clear();
                panel.repaint();
                refreshInfo(simulation, panel, infoArea, tickLabel);
            }
        });
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
            sb.append(String.format(
                    "#%d %-24s pos=(%2d,%2d) cd=%3d extra=%-6s%n",
                    i,
                    simulation.getNpcName(mob.getType()),
                    mob.getX(),
                    mob.getY(),
                    mob.getCooldown(),
                    mob.getExtra() == null ? "-" : mob.getExtra()
            ));
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
