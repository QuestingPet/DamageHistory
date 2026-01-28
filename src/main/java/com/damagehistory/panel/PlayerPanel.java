package com.damagehistory.panel;

import com.damagehistory.DamageHistoryConfig;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import net.runelite.api.Client;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.AsyncBufferedImage;

public class PlayerPanel extends JPanel {
    private static final BufferedImage FIST_IMAGE = ImageUtil.loadImageResource(PlayerPanel.class, "fist.png");

    @Inject
    private ItemManager itemManager;

    @Inject
    private DamageHistoryConfig config;

    private final String playerName;
    private final JPanel headerPanel;
    private final JPanel hitsContainer;
    private final List<PlayerHitRecord> hitRecords = new ArrayList<>();
    private boolean collapsed = false;
    private final Client client;
    private final DamageHistoryPanel parentPanel;

    public PlayerPanel(String playerName, ItemManager itemManager, DamageHistoryConfig config, Client client, DamageHistoryPanel parentPanel) {
        this.playerName = playerName;
        this.itemManager = itemManager;
        this.config = config;
        this.client = client;
        this.parentPanel = parentPanel;

        setLayout(new BorderLayout());
        setBackground(ColorScheme.DARK_GRAY_COLOR);
        setBorder(new EmptyBorder(2, 2, 12, 2));
        setFocusable(false);

        // Header panel with player name and collapse button
        headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        headerPanel.setBorder(new EmptyBorder(4, 8, 4, 8));

        JLabel nameLabel = new JLabel(playerName);
        nameLabel.setForeground(Color.WHITE);
        nameLabel.setFont(FontManager.getRunescapeBoldFont());

        JLabel collapseLabel = new JLabel("▼");
        collapseLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);

        headerPanel.add(nameLabel, BorderLayout.WEST);
        headerPanel.add(collapseLabel, BorderLayout.EAST);

        // Hits container
        hitsContainer = new JPanel();
        hitsContainer.setLayout(new BoxLayout(hitsContainer, BoxLayout.Y_AXIS));
        hitsContainer.setBackground(ColorScheme.DARK_GRAY_COLOR);
        hitsContainer.setFocusable(false);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        mainPanel.setFocusable(false);
        mainPanel.add(headerPanel, BorderLayout.NORTH);
        mainPanel.add(hitsContainer, BorderLayout.CENTER);

        // Make entire panel clickable for collapse/expand
        addClickListeners(this);
        addClickListeners(mainPanel);
        addClickListeners(hitsContainer);
        addClickListeners(headerPanel);

        add(mainPanel, BorderLayout.CENTER);
    }

    private void addClickListeners(JComponent component) {
        component.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (hitRecords.size() > 1) {
                    toggleCollapse();
                    updateHeader();
                }
            }
        });
    }

    public PlayerHitRecord getLatestHit() {
        return hitRecords.isEmpty() ? null : hitRecords.get(0);
    }

    public void addHit(PlayerHitRecord record) {
        hitRecords.add(0, record);

        if (hitRecords.size() > UIConstants.MAX_HIT_RECORDS) {
            hitRecords.remove(hitRecords.size() - 1);
        }

        updateHeader();
        refreshPanel();
    }

    private void toggleCollapse() {
        collapsed = !collapsed;
        refreshPanel();
    }

    private void updateHeader() {
        headerPanel.removeAll();

        JLabel nameLabel = new JLabel(playerName);
        nameLabel.setForeground(Color.WHITE);
        nameLabel.setFont(FontManager.getRunescapeBoldFont());

        headerPanel.add(nameLabel, BorderLayout.WEST);

        // Right side panel for clear and collapse buttons
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        rightPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);

        // Clear button (always visible if there are hits)
        if (!hitRecords.isEmpty()) {
            JLabel clearLabel = new JLabel("✕");
            clearLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
            clearLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            clearLabel.setToolTipText("Clear all hits");
            clearLabel.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    clearAllHits();
                }
                @Override
                public void mouseEntered(java.awt.event.MouseEvent e) {
                    clearLabel.setForeground(Color.WHITE);
                }
                @Override
                public void mouseExited(java.awt.event.MouseEvent e) {
                    clearLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
                }
            });
            rightPanel.add(clearLabel);
        }

        // Collapse button (only show if there are multiple records)
        if (hitRecords.size() > 1) {
            JLabel collapseLabel = new JLabel(collapsed ? "▶" : "▼");
            collapseLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
            rightPanel.add(collapseLabel);
        }

        if (rightPanel.getComponentCount() > 0) {
            headerPanel.add(rightPanel, BorderLayout.EAST);
        }

        headerPanel.revalidate();
        headerPanel.repaint();
    }

    public void refreshPanel() {
        hitsContainer.removeAll();

        LayoutCalculator.ColumnWidths widths = LayoutCalculator.calculateColumnWidths(
            hitRecords.stream().map(r -> new HitRecord(r.getHit(), r.getNpcName(), r.getWeaponId(),
                r.getTickCount(), r.getAttackSpeed(), r.isSpecialAttack())).collect(Collectors.toList()),
            this
        );

        // Show only the configured number of recent hits
        boolean isLocalPlayer = client.getLocalPlayer() != null && client.getLocalPlayer().getName().equals(playerName);
        int maxHits = isLocalPlayer ? config.maxHitsToShowSelf() : config.maxHitsToShowOthers();
        int hitsToShow = Math.min(hitRecords.size(), maxHits);

        // Show latest hit first (always visible)
        PlayerHitRecord latestRecord = hitRecords.get(0);
        JPanel latestHitPanel = createHitPanel(latestRecord, 0, widths);
        addClickListeners(latestHitPanel);
        hitsContainer.add(latestHitPanel);

        // Show remaining hits if not collapsed
        if (!collapsed && hitsToShow > 1) {
            for (int i = 1; i < hitsToShow; i++) {
                PlayerHitRecord record = hitRecords.get(i);
                JPanel hitPanel = createHitPanel(record, i, widths);
                addClickListeners(hitPanel);
                hitsContainer.add(hitPanel);
            }
        }

        hitsContainer.revalidate();
        hitsContainer.repaint();

        // Force parent containers to revalidate for proper height adjustment
        Container parent = getParent();
        while (parent != null) {
            parent.revalidate();
            parent = parent.getParent();
        }
    }

    private JPanel createHitPanel(PlayerHitRecord record, int index, LayoutCalculator.ColumnWidths widths) {
        boolean isRecent = index == 0;
        float alpha = isRecent ? UIConstants.RECENT_HIT_ALPHA : UIConstants.OLD_HIT_ALPHA;

        JPanel panel = UIUtils.createHitPanelBase(false);

        JLabel iconLabel = new JLabel();
        iconLabel.setPreferredSize(new Dimension(UIConstants.ICON_SIZE, UIConstants.ICON_SIZE));
        iconLabel.setMinimumSize(new Dimension(UIConstants.ICON_SIZE, UIConstants.ICON_SIZE));
        iconLabel.setMaximumSize(new Dimension(UIConstants.ICON_SIZE, UIConstants.ICON_SIZE));

        if (record.getWeaponId() == -1) {
            setIconWithOutline(iconLabel, FIST_IMAGE, record.isSpecialAttack());
        } else {
            AsyncBufferedImage weaponImage = itemManager.getImage(record.getWeaponId());
            weaponImage.onLoaded(() -> setIconWithOutline(iconLabel, weaponImage, record.isSpecialAttack()));
        }
        UIUtils.addDebugBorder(iconLabel, Color.RED, config.debugMode());
        panel.add(iconLabel, BorderLayout.WEST);

        Dimension damageSize = new Dimension(widths.damageWidth, 0);
        JLabel damageLabel = UIUtils.createStyledLabel(
            String.valueOf(record.getHit()),
            SwingConstants.CENTER,
            FontManager.getRunescapeBoldFont(),
            UIUtils.getDamageColor(record.getHit(), config),
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

    private TickInfo calculateTickInfo(PlayerHitRecord record, int index) {
        Integer ticksSince = null;
        Integer previousAttackSpeed = null;

        // Use stored tick delay information if available
        if (record.getTicksSincePrevious() != null && record.getPreviousAttackSpeed() != null) {
            ticksSince = record.getTicksSincePrevious();
            previousAttackSpeed = record.getPreviousAttackSpeed();
        }
        // Fallback to old calculation method for records without stored info
        else if (index < hitRecords.size() - 1) {
            ticksSince = record.getTickCount() - hitRecords.get(index + 1).getTickCount();
            previousAttackSpeed = hitRecords.get(index + 1).getAttackSpeed();
        }

        if (ticksSince == null || previousAttackSpeed == null) {
            return new TickInfo("", Color.WHITE);
        }

        String tickText = config.tickDisplayMode() == DamageHistoryConfig.TickDisplayMode.EXTRA_DELAYED_TICKS
            ? String.format(" +%dt", ticksSince - previousAttackSpeed)
            : String.format(" +%dt", ticksSince);

        Color tickColor = UIUtils.getTickDelayColor(ticksSince, previousAttackSpeed, config);
        return new TickInfo(tickText, tickColor);
    }

    private void setIconWithOutline(JLabel iconLabel, BufferedImage image, boolean specialAttack) {
        if (specialAttack) {
            BufferedImage outlinedImage = UIUtils.addOutline(image, UIConstants.SPECIAL_ATTACK_OUTLINE_COLOR);
            iconLabel.setIcon(new ImageIcon(outlinedImage));
        } else {
            iconLabel.setIcon(new ImageIcon(image));
        }
    }

    private void clearAllHits() {
        if (parentPanel != null) {
            parentPanel.removePlayerPanel(playerName);
        }
    }

    public String getPlayerName() {
        return playerName;
    }
}