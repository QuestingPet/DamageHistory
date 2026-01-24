package com.damagehistory;

import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
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

        hitsContainer.setLayout(new BoxLayout(hitsContainer, BoxLayout.Y_AXIS));
        hitsContainer.setBackground(ColorScheme.DARK_GRAY_COLOR);

        layoutPanel.add(hitsContainer);
    }

    public void addHit(String weaponName, int hit, String npcName, int weaponId) {
        HitRecord record = new HitRecord(weaponName, hit, npcName, weaponId);
        hitRecords.add(0, record); // Add to beginning for most recent first

        // Keep only last 50 hits
        if (hitRecords.size() > 50) {
            hitRecords.remove(hitRecords.size() - 1);
        }

        refreshPanel();
    }

    private void refreshPanel() {
        hitsContainer.removeAll();

        for (HitRecord record : hitRecords) {
            JPanel hitPanel = createHitPanel(record);
            hitsContainer.add(hitPanel);
        }

        hitsContainer.revalidate();
        hitsContainer.repaint();
    }

    private JPanel createHitPanel(HitRecord record) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, ColorScheme.DARK_GRAY_COLOR),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        panel.setPreferredSize(new Dimension(0, 36));

        JLabel iconLabel = new JLabel();
        itemManager.getImage(record.getWeaponId()).addTo(iconLabel);
        panel.add(iconLabel, BorderLayout.WEST);

        JLabel hitLabel = new JLabel(String.format("Hit %d on %s",
                record.getHit(), record.getNpcName()));
        hitLabel.setForeground(Color.WHITE);
        hitLabel.setBorder(new EmptyBorder(0, 8, 0, 0));

        panel.add(hitLabel, BorderLayout.CENTER);
        return panel;
    }
}