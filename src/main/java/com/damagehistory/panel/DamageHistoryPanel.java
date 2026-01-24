package com.damagehistory.panel;

import com.damagehistory.DamageHistoryConfig;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.AsyncBufferedImage;

@Singleton
public class DamageHistoryPanel extends PluginPanel {
    @Inject
    private ItemManager itemManager;

    @Inject
    private DamageHistoryConfig config;

    private final JPanel hitsContainer = new JPanel();
    private final List<HitRecord> hitRecords = new ArrayList<>();

    public DamageHistoryPanel() {
        setBorder(new EmptyBorder(6, 6, 6, 6));
        setBackground(ColorScheme.DARK_GRAY_COLOR);
        setLayout(new BorderLayout());

        final JPanel layoutPanel = new JPanel();
        layoutPanel.setLayout(new BoxLayout(layoutPanel, BoxLayout.Y_AXIS));
        add(layoutPanel, BorderLayout.NORTH);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> refreshPanel());
        buttonPanel.add(refreshButton);
        JButton testButton = new JButton("+");
        testButton.addActionListener(e -> addTestRecord());
        buttonPanel.add(testButton);
        JButton clearButton = new JButton("Clear History");
        clearButton.addActionListener(e -> clearHistory());
        buttonPanel.add(clearButton);
        buttonPanel.setBorder(new EmptyBorder(0, 0, 8, 0));
        layoutPanel.add(buttonPanel);

        hitsContainer.setLayout(new BoxLayout(hitsContainer, BoxLayout.Y_AXIS));
        hitsContainer.setBackground(ColorScheme.DARK_GRAY_COLOR);

        layoutPanel.add(hitsContainer);
    }

    public void addHit(int hit, String npcName, int weaponId, int tickCount, int attackSpeed, boolean specialAttack) {
        HitRecord record = new HitRecord(hit, npcName, weaponId, tickCount, attackSpeed, specialAttack);
        hitRecords.add(0, record);

        if (hitRecords.size() > UIConstants.MAX_HIT_RECORDS) {
            hitRecords.remove(hitRecords.size() - 1);
        }

        refreshPanel();
    }

    public void refreshPanel() {
        hitsContainer.removeAll();

        if (hitRecords.isEmpty()) {
            hitsContainer.revalidate();
            hitsContainer.repaint();
            return;
        }

        LayoutCalculator.ColumnWidths widths = LayoutCalculator.calculateColumnWidths(hitRecords, this);

        for (int i = 0; i < hitRecords.size(); i++) {
            HitRecord record = hitRecords.get(i);
            JPanel hitPanel = createHitPanel(record, i, widths);
            hitsContainer.add(hitPanel);
        }

        hitsContainer.revalidate();
        hitsContainer.repaint();
    }

    private void clearHistory() {
        hitRecords.clear();
        refreshPanel();
    }
    
    private void addTestRecord() {
        String[] npcs = {"Goblin", "Cow", "Rat", "Spider"};

        int hit = (int)(Math.random() * 50);
        String npc = npcs[(int)(Math.random() * npcs.length)];
        int weaponId = 4151; // Whip ID
        int tickCount = (int)(Math.random() * 1000);
        int attackSpeed = 4;
        boolean specialAttack = Math.random() < 0.3;
        
        addHit(hit, npc, weaponId, tickCount, attackSpeed, specialAttack);
    }

    private JPanel createHitPanel(HitRecord record, int index, LayoutCalculator.ColumnWidths widths) {
        boolean isRecent = index == 0;
        float alpha = isRecent ? UIConstants.RECENT_HIT_ALPHA : UIConstants.OLD_HIT_ALPHA;
        
        JPanel panel = UIUtils.createHitPanelBase(isRecent);

        JLabel iconLabel = new JLabel();
        AsyncBufferedImage weaponImage = itemManager.getImage(record.getWeaponId());
        weaponImage.onLoaded(() -> {
            if (record.isSpecialAttack()) {
                BufferedImage outlinedImage = UIUtils.addOutline(weaponImage, UIConstants.SPECIAL_ATTACK_OUTLINE_COLOR);
                iconLabel.setIcon(new ImageIcon(outlinedImage));
            } else {
                iconLabel.setIcon(new ImageIcon(weaponImage));
            }
        });
        UIUtils.addDebugBorder(iconLabel, Color.RED, config.debugMode());
        panel.add(iconLabel, BorderLayout.WEST);

        Dimension damageSize = new Dimension(widths.damageWidth, 0);
        JLabel damageLabel = UIUtils.createStyledLabel(
            String.valueOf(record.getHit()),
            SwingConstants.CENTER,
            FontManager.getRunescapeBoldFont(),
            UIUtils.getDamageColor(record.getHit()),
            damageSize
        );
        UIUtils.addDebugBorder(damageLabel, Color.BLUE, config.debugMode());

        Dimension npcSize = new Dimension(widths.npcWidth, 0);
        JLabel npcLabel = UIUtils.createStyledLabel(
            "<html>" + record.getNpcName() + "</html>",
            SwingConstants.LEFT,
            null,
            UIUtils.applyAlpha(Color.WHITE, alpha),
            npcSize
        );
        UIUtils.addDebugBorderWithPadding(npcLabel, Color.GREEN, config.debugMode(), 8);

        TickInfo tickInfo = calculateTickInfo(record, index);
        Dimension tickSize = new Dimension(widths.tickWidth, 0);
        JLabel tickLabel = UIUtils.createStyledLabel(
            tickInfo.text,
            SwingConstants.CENTER,
            null,
            UIUtils.applyAlpha(tickInfo.color, alpha),
            tickSize
        );
        UIUtils.addDebugBorder(tickLabel, Color.YELLOW, config.debugMode());

        JPanel textPanel = new JPanel(new BorderLayout());
        textPanel.setBackground(panel.getBackground());
        textPanel.add(damageLabel, BorderLayout.WEST);
        textPanel.add(npcLabel, BorderLayout.CENTER);
        textPanel.add(tickLabel, BorderLayout.EAST);
        textPanel.setBorder(new EmptyBorder(0, 0, 0, 0));
        
        panel.add(textPanel, BorderLayout.CENTER);
        return panel;
    }
    
    private TickInfo calculateTickInfo(HitRecord record, int index) {
        if (index >= hitRecords.size() - 1) {
            return new TickInfo("", Color.WHITE);
        }
        
        int ticksSince = record.getTickCount() - hitRecords.get(index + 1).getTickCount();
        int previousAttackSpeed = hitRecords.get(index + 1).getAttackSpeed();
        
        String tickText;
        if (config.tickDisplayMode() == DamageHistoryConfig.TickDisplayMode.EXTRA_DELAYED_TICKS) {
            int extraTicks = ticksSince - previousAttackSpeed;
            tickText = String.format(" +%dt", extraTicks);
        } else {
            tickText = String.format(" +%dt", ticksSince);
        }
        
        Color tickColor = UIUtils.getTickDelayColor(ticksSince, previousAttackSpeed);
        return new TickInfo(tickText, tickColor);
    }
}