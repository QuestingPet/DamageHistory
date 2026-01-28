package com.damagehistory.panel;

import com.damagehistory.DamageHistoryConfig;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import lombok.Setter;
import net.runelite.api.Client;
import net.runelite.client.game.ItemManager;
import net.runelite.client.party.PartyService;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;

@Singleton
public class DamageHistoryPanel extends PluginPanel {

    @Inject
    private ItemManager itemManager;

    @Inject
    private DamageHistoryConfig config;

    @Inject
    @Setter
    private Client client;

    @Inject
    @Setter
    private PartyService partyService;

    private final JPanel basePanel = new JPanel();
    private final Map<String, PlayerPanel> playerPanels = new HashMap<>();

    public DamageHistoryPanel() {
        super(false);
        setLayout(new BorderLayout());

        basePanel.setBorder(new EmptyBorder(BORDER_OFFSET, BORDER_OFFSET, BORDER_OFFSET, BORDER_OFFSET));
        basePanel.setLayout(new net.runelite.client.ui.DynamicGridLayout(0, 1, 0, 5));

        final JPanel topPanel = new JPanel();
        topPanel.setBorder(new EmptyBorder(BORDER_OFFSET, 2, BORDER_OFFSET, 2));
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));

        JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        
        JLabel titleLabel = new JLabel("Damage History");
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setBorder(new EmptyBorder(0, 8, 0, 0));
        buttonPanel.add(titleLabel, BorderLayout.WEST);
        
        JButton clearButton = new JButton("Clear History");
        clearButton.addActionListener(e -> clearHistory());
        buttonPanel.add(clearButton, BorderLayout.EAST);
        buttonPanel.setBorder(new EmptyBorder(0, 0, 8, 0));
        topPanel.add(buttonPanel);

        add(topPanel, BorderLayout.NORTH);

        // Wrap content to anchor to top and prevent expansion
        final JPanel northPanel = new JPanel(new BorderLayout());
        northPanel.add(basePanel, BorderLayout.NORTH);
        final JScrollPane scrollPane = new JScrollPane(northPanel);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        add(scrollPane, BorderLayout.CENTER);

        checkEmpty();
        basePanel.revalidate();
        basePanel.repaint();
    }

    public PlayerHitRecord getLatestHitForPlayer(String playerName) {
        PlayerPanel playerPanel = playerPanels.get(playerName);
        return playerPanel != null ? playerPanel.getLatestHit() : null;
    }

    public PlayerPanel getPlayerPanel(String playerName) {
        return playerPanels.get(playerName);
    }

    public void addHit(PlayerHitRecord record) {
        // Remove empty message if it exists
        if (playerPanels.isEmpty()) {
            basePanel.removeAll();
        }

        PlayerPanel playerPanel = playerPanels.get(record.getPlayerName());
        if (playerPanel == null) {
            playerPanel = new PlayerPanel(record.getPlayerName(), itemManager, config, client, this);
            playerPanels.put(record.getPlayerName(), playerPanel);

            // Add local player at the top, others at the end
            if (client.getLocalPlayer() != null && client.getLocalPlayer().getName().equals(record.getPlayerName())) {
                basePanel.add(playerPanel, 0);
            } else {
                basePanel.add(playerPanel);
            }
            basePanel.revalidate();
        }

        playerPanel.addHit(record);
    }

    public void removePlayerPanel(String playerName) {
        PlayerPanel panel = playerPanels.remove(playerName);
        if (panel != null) {
            basePanel.remove(panel);
        }

        checkEmpty();

        basePanel.revalidate();
        basePanel.repaint();
    }

    public void clearOtherPlayers() {
        String localPlayerName = client.getLocalPlayer() != null ? client.getLocalPlayer().getName() : null;
        playerPanels.entrySet().removeIf(entry -> {
            if (!entry.getKey().equals(localPlayerName)) {
                basePanel.remove(entry.getValue());
                return true;
            }
            return false;
        });
        checkEmpty();
        basePanel.revalidate();
        basePanel.repaint();
    }



    public void refreshPanel() {
        for (PlayerPanel playerPanel : playerPanels.values()) {
            playerPanel.refreshPanel();
        }
        basePanel.revalidate();
        basePanel.repaint();
    }

    private void checkEmpty() {
        if (!playerPanels.isEmpty()) {
            return;
        }

        basePanel.removeAll();
        JPanel emptyPanel = new JPanel(new BorderLayout());
        emptyPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);

        JTextPane emptyText = new JTextPane();
        emptyText.setContentType("text/html");
        emptyText.setText("<html><div style='text-align: center;'>" +
                          "The <span style='color: #00FF00;'>Customizable XP Drops</span> plugin is required for populating data.<br><br>" +
                          "If you're not seeing any data here after hitting monsters, " +
                          "please go install it from the Plugin Hub.<br><br>" +
                          "If you don't want the customized xp drops, but still want this plugin's functionality, " +
                          "you can uncheck <span style='color: #FF0000;'>\"Use Customizable XP drops\"</span> from that plugin" +
                          "</div></html>");
        emptyText.setForeground(Color.WHITE);
        emptyText.setBackground(ColorScheme.DARK_GRAY_COLOR);
        emptyText.setEditable(false);
        emptyText.setBorder(new EmptyBorder(5, 5, 5, 5));
        emptyText.setPreferredSize(new Dimension(200, 300));

        emptyPanel.add(emptyText, BorderLayout.CENTER);
        basePanel.add(emptyPanel);
    }

    private void clearHistory() {
        for (PlayerPanel panel : playerPanels.values()) {
            basePanel.remove(panel);
        }
        playerPanels.clear();
        basePanel.removeAll();
        checkEmpty();
        basePanel.revalidate();
        basePanel.repaint();
    }


}