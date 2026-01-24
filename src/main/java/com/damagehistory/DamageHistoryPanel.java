package com.damagehistory;

import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.FontManager;
import net.runelite.client.game.ItemManager;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@Singleton
public class DamageHistoryPanel extends PluginPanel {
    @Inject
    private ItemManager itemManager;

    private final JPanel hitsContainer = new JPanel();
    private final List<HitRecord> hitRecords = new ArrayList<>();

    public DamageHistoryPanel() {
        setBorder(new EmptyBorder(6, 6, 6, 6));
        setBackground(ColorScheme.DARK_GRAY_COLOR);
        setLayout(new BorderLayout());

        // Add the outer panel for wrapping everything else inside
        final JPanel layoutPanel = new JPanel();
        layoutPanel.setLayout(new BoxLayout(layoutPanel, BoxLayout.Y_AXIS));
        add(layoutPanel, BorderLayout.NORTH);

        // Add clear button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        JButton clearButton = new JButton("Clear History");
        clearButton.addActionListener(e -> clearHistory());
        buttonPanel.add(clearButton);
        buttonPanel.setBorder(new EmptyBorder(0, 0, 8, 0));
        layoutPanel.add(buttonPanel);

        hitsContainer.setLayout(new BoxLayout(hitsContainer, BoxLayout.Y_AXIS));
        hitsContainer.setBackground(ColorScheme.DARK_GRAY_COLOR);

        layoutPanel.add(hitsContainer);
    }

    public void addHit(String weaponName, int hit, String npcName, int weaponId, int tickCount, int attackSpeed) {
        HitRecord record = new HitRecord(weaponName, hit, npcName, weaponId, tickCount, attackSpeed);
        hitRecords.add(0, record); // Add to beginning for most recent first

        // Keep only last 50 hits
        if (hitRecords.size() > 50) {
            hitRecords.remove(hitRecords.size() - 1);
        }

        refreshPanel();
    }

    private void refreshPanel() {
        hitsContainer.removeAll();

        for (int i = 0; i < hitRecords.size(); i++) {
            HitRecord record = hitRecords.get(i);
            JPanel hitPanel = createHitPanel(record, i);
            hitsContainer.add(hitPanel);
        }

        hitsContainer.revalidate();
        hitsContainer.repaint();
    }

    private void clearHistory() {
        hitRecords.clear();
        refreshPanel();
    }

    private Color getDamageColor(int damage) {
        if (damage >= 30) return Color.GREEN;
        if (damage >= 15) return Color.YELLOW;
        return Color.RED;
    }

    private Color getTickDelayColor(int ticksSince, int attackSpeed) {
        if (ticksSince <= attackSpeed) return Color.GREEN;
        if (ticksSince <= attackSpeed + 2) return Color.YELLOW;
        return Color.RED;
    }

    private JPanel createHitPanel(HitRecord record, int index) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        // Highlight most recent hit
        if (index == 0) {
            panel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, ColorScheme.BRAND_ORANGE),
                    BorderFactory.createEmptyBorder(4, 8, 4, 8)
            ));
        } else {
            panel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, ColorScheme.DARK_GRAY_COLOR),
                    BorderFactory.createEmptyBorder(4, 8, 4, 8)
            ));
        }
        if (index == 0) {
            panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
            panel.setPreferredSize(new Dimension(0, 50));
        } else {
            panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
            panel.setPreferredSize(new Dimension(0, 42));
        }

        // Fade effect for older entries
        float alpha = index == 0 ? 1.0f : 0.9f;

        JLabel iconLabel = new JLabel();
        itemManager.getImage(record.getWeaponId()).addTo(iconLabel);
        panel.add(iconLabel, BorderLayout.WEST);

        // Create damage label with fixed width for alignment
        JLabel damageLabel = new JLabel(String.valueOf(record.getHit()), SwingConstants.CENTER);
        damageLabel.setForeground(getDamageColor(record.getHit()));
        damageLabel.setFont(FontManager.getRunescapeBoldFont());
        damageLabel.setPreferredSize(new Dimension(24, damageLabel.getPreferredSize().height));
        damageLabel.setMinimumSize(new Dimension(24, damageLabel.getPreferredSize().height));
        
        // Calculate ticks since last hit
        String tickInfo = "";
        Color tickColor = Color.WHITE;
        if (index < hitRecords.size() - 1) {
            int ticksSince = record.getTickCount() - hitRecords.get(index + 1).getTickCount();
            tickInfo = String.format(" (+%dt)", ticksSince);
            tickColor = getTickDelayColor(ticksSince, hitRecords.get(index + 1).getAttackSpeed());
        }
        
        JLabel hitLabel = new JLabel("<html>" + record.getNpcName() + "</html>");
        hitLabel.setForeground(new Color(255, 255, 255, (int)(255 * alpha)));
        hitLabel.setBorder(new EmptyBorder(0, 8, 0, 0));

        JLabel tickLabel = new JLabel(tickInfo);
        tickLabel.setForeground(new Color(tickColor.getRed(), tickColor.getGreen(), tickColor.getBlue(), (int)(255 * alpha)));

        JPanel textPanel = new JPanel(new BorderLayout());
        textPanel.setBackground(panel.getBackground());
        textPanel.add(damageLabel, BorderLayout.WEST);
        textPanel.add(hitLabel, BorderLayout.CENTER);
        textPanel.add(tickLabel, BorderLayout.EAST);
        textPanel.setBorder(new EmptyBorder(0, 8, 0, 0));
        
        panel.add(textPanel, BorderLayout.CENTER);
        return panel;
    }
}