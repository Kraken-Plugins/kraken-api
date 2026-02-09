package com.kraken.api.plugins.packetmapper;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;

import javax.inject.Inject;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.FileWriter;

/**
 * UI Panel for controlling the packet mapping tool
 */
@Slf4j
public class PacketMappingPanel extends PluginPanel {

    private final PacketMappingTool mappingTool;
    private final PacketQueueMonitor queueMonitor;
    private final PacketInterceptor interceptor;
    private JButton startMonitoringBtn;
    private JButton stopMonitoringBtn;
    private JButton exportAllBtn;
    private JButton clearMappingsBtn;
    private JTextArea mappingsTextArea;
    private JLabel statusLabel;
    private JComboBox<String> packetSelector;

    @Inject
    public PacketMappingPanel(PacketMappingTool mappingTool, PacketQueueMonitor queueMonitor, PacketInterceptor interceptor) {
        this.mappingTool = mappingTool;
        this.queueMonitor = queueMonitor;
        this.interceptor = interceptor;

        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(10, 10, 10, 10));
        setBackground(ColorScheme.DARK_GRAY_COLOR);

        add(createControlPanel(), BorderLayout.NORTH);
        add(createMappingsPanel(), BorderLayout.CENTER);
        add(createStatusPanel(), BorderLayout.SOUTH);
    }

    private JPanel createControlPanel() {
        JPanel panel = new JPanel(new GridLayout(0, 1, 5, 5));
        panel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        panel.setBorder(new EmptyBorder(0, 0, 10, 0));

        startMonitoringBtn = createStyledButton("Start Monitoring");
        startMonitoringBtn.addActionListener(e -> startMonitoring());

        stopMonitoringBtn = createStyledButton("Stop Monitoring");
        stopMonitoringBtn.setEnabled(false);
        stopMonitoringBtn.addActionListener(e -> stopMonitoring());

        exportAllBtn = createStyledButton("Export All Mappings");
        exportAllBtn.addActionListener(e -> exportAllMappings());

        JButton exportSelectedBtn = createStyledButton("Export Selected");
        exportSelectedBtn.addActionListener(e -> exportSelectedMapping());

        JButton copyToClipboardBtn = createStyledButton("Copy to Clipboard");
        copyToClipboardBtn.addActionListener(e -> copyToClipboard());

        clearMappingsBtn = createStyledButton("Clear Mappings");
        clearMappingsBtn.addActionListener(e -> clearMappings());

        panel.add(startMonitoringBtn);
        panel.add(stopMonitoringBtn);
        panel.add(exportAllBtn);
        panel.add(exportSelectedBtn);
        panel.add(copyToClipboardBtn);
        panel.add(clearMappingsBtn);

        return panel;
    }

    private JPanel createMappingsPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 5));
        panel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        panel.setBorder(new EmptyBorder(10, 0, 10, 0));

        // Packet selector dropdown
        packetSelector = new JComboBox<>();
        packetSelector.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        packetSelector.setForeground(Color.WHITE);
        packetSelector.setFocusable(false);
        packetSelector.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                                                          int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (isSelected) {
                    setBackground(ColorScheme.BRAND_ORANGE);
                } else {
                    setBackground(ColorScheme.DARKER_GRAY_COLOR);
                }
                setForeground(Color.WHITE);
                return this;
            }
        });
        packetSelector.addActionListener(e -> updateMappingDisplay());
        panel.add(packetSelector, BorderLayout.NORTH);

        // Text area for displaying mapping
        mappingsTextArea = new JTextArea();
        mappingsTextArea.setEditable(false);
        mappingsTextArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        mappingsTextArea.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        mappingsTextArea.setForeground(Color.WHITE);
        mappingsTextArea.setBorder(new EmptyBorder(5, 5, 5, 5));

        JScrollPane scrollPane = new JScrollPane(mappingsTextArea);
        scrollPane.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        scrollPane.setBorder(BorderFactory.createLineBorder(ColorScheme.MEDIUM_GRAY_COLOR));
        scrollPane.setPreferredSize(new Dimension(400, 400));
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createStatusPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        panel.setBorder(new EmptyBorder(10, 0, 0, 0));

        statusLabel = new JLabel("Ready");
        statusLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        panel.add(statusLabel, BorderLayout.CENTER);

        return panel;
    }

    // Helper method to create RuneLite-styled buttons
    private JButton createStyledButton(String text) {
        JButton button = new JButton(text);
        button.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(new EmptyBorder(5, 10, 5, 10));

        // Hover effect
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                if (button.isEnabled()) {
                    button.setBackground(ColorScheme.DARKER_GRAY_HOVER_COLOR);
                }
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(ColorScheme.DARKER_GRAY_COLOR);
            }
        });

        return button;
    }

    private void startMonitoring() {
        try {
            //queueMonitor.startMonitoring();
            interceptor.startInterception();
            startMonitoringBtn.setEnabled(false);
            stopMonitoringBtn.setEnabled(true);
            statusLabel.setText("Monitoring active");
            statusLabel.setForeground(ColorScheme.PROGRESS_COMPLETE_COLOR); // Green
        } catch (Exception e) {
            log.error("Failed to start monitoring", e);
            JOptionPane.showMessageDialog(this,
                    "Failed to start monitoring: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void stopMonitoring() {
//        queueMonitor.stopMonitoring();
        try {
            interceptor.stopInterception();
        } catch (Exception e) {
            log.error("Failed to stop monitoring", e);
        }
        startMonitoringBtn.setEnabled(true);
        stopMonitoringBtn.setEnabled(false);
        statusLabel.setText("Monitoring stopped");
        statusLabel.setForeground(ColorScheme.PROGRESS_ERROR_COLOR); // Orange/Red
        updatePacketSelector();
    }

    private void updatePacketSelector() {
        packetSelector.removeAllItems();
        packetSelector.addItem("-- Select a packet --");

        for (String packetName : mappingTool.getMappings().keySet()) {
            packetSelector.addItem(packetName);
        }

        statusLabel.setText("Found " + mappingTool.getMappings().size() + " unique packets");
        statusLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
    }

    private void updateMappingDisplay() {
        String selectedPacket = (String) packetSelector.getSelectedItem();

        if (selectedPacket == null || selectedPacket.startsWith("--")) {
            mappingsTextArea.setText("");
            return;
        }

        String mapping = mappingTool.exportMapping(selectedPacket);
        mappingsTextArea.setText(mapping);
    }

    /**
     * Exports all mappings to a file
     */
    private void exportAllMappings() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setSelectedFile(new File("PacketMappings.java"));
        
        int result = fileChooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            
            try (FileWriter writer = new FileWriter(file)) {
                String allMappings = mappingTool.exportMappings();
                writer.write(allMappings);
                
                JOptionPane.showMessageDialog(this,
                    "Exported " + mappingTool.getMappings().size() + " mappings to " + file.getName(),
                    "Export Successful",
                    JOptionPane.INFORMATION_MESSAGE);
                    
                statusLabel.setText("Exported to: " + file.getAbsolutePath());
                
            } catch (Exception e) {
                log.error("Failed to export mappings", e);
                JOptionPane.showMessageDialog(this,
                    "Failed to export mappings: " + e.getMessage(),
                    "Export Failed",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Exports the selected mapping to a file
     */
    private void exportSelectedMapping() {
        String selectedPacket = (String) packetSelector.getSelectedItem();
        
        if (selectedPacket == null || selectedPacket.startsWith("--")) {
            JOptionPane.showMessageDialog(this,
                "Please select a packet first",
                "No Selection",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setSelectedFile(new File(selectedPacket + "_Mapping.java"));
        
        int result = fileChooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            
            try (FileWriter writer = new FileWriter(file)) {
                String mapping = mappingTool.exportMapping(selectedPacket);
                writer.write(mapping);
                
                JOptionPane.showMessageDialog(this,
                    "Exported mapping for " + selectedPacket,
                    "Export Successful",
                    JOptionPane.INFORMATION_MESSAGE);
                    
            } catch (Exception e) {
                log.error("Failed to export mapping", e);
                JOptionPane.showMessageDialog(this,
                    "Failed to export mapping: " + e.getMessage(),
                    "Export Failed",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Copies current mapping to clipboard
     */
    private void copyToClipboard() {
        String text = mappingsTextArea.getText();
        
        if (text.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "No mapping to copy",
                "Nothing to Copy",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        StringSelection selection = new StringSelection(text);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
        
        statusLabel.setText("Copied to clipboard");
    }

    /**
     * Clears all mappings
     */
    private void clearMappings() {
        int result = JOptionPane.showConfirmDialog(this,
            "Are you sure you want to clear all " + mappingTool.getMappings().size() + " mappings?",
            "Confirm Clear",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);
            
        if (result == JOptionPane.YES_OPTION) {
            mappingTool.clearMappings();
            queueMonitor.clearHistory();
            updatePacketSelector();
            mappingsTextArea.setText("");
            statusLabel.setText("All mappings cleared");
        }
    }
}
