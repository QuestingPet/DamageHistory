package com.damagehistory.panel;

import com.damagehistory.DamageHistoryConfig;
import com.damagehistory.DamageHistoryOverlay;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import lombok.Setter;
import net.runelite.client.game.ItemManager;
import net.runelite.client.party.PartyMember;
import net.runelite.client.party.PartyService;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.api.Client;
import javax.swing.ScrollPaneConstants;

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

    @Setter
    private DamageHistoryOverlay overlay;

    private final JPanel basePanel = new JPanel();
    private final Map<String, PlayerPanel> playerPanels = new HashMap<>();
    private int testRecordCounter = 0;
    private int prevTickCount = 0;

    public DamageHistoryPanel() {
        super(false);
        setLayout(new BorderLayout());

        basePanel.setBorder(new EmptyBorder(BORDER_OFFSET, BORDER_OFFSET, BORDER_OFFSET, BORDER_OFFSET));
        basePanel.setLayout(new net.runelite.client.ui.DynamicGridLayout(0, 1, 0, 5));

        final JPanel topPanel = new JPanel();
        topPanel.setBorder(new EmptyBorder(BORDER_OFFSET, 2, BORDER_OFFSET, 2));
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));

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
        topPanel.add(buttonPanel);

        add(topPanel, BorderLayout.NORTH);

        // Wrap content to anchor to top and prevent expansion
        final JPanel northPanel = new JPanel(new BorderLayout());
        northPanel.add(basePanel, BorderLayout.NORTH);
        final JScrollPane scrollPane = new JScrollPane(northPanel);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        add(scrollPane, BorderLayout.CENTER);
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

        // Also update overlay if available
        if (overlay != null) {
            overlay.addHit(record);
        }
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
        if (!checkEmpty()) {
            for (PlayerPanel playerPanel : playerPanels.values()) {
                playerPanel.refreshPanel();
            }
        }

        basePanel.revalidate();
        basePanel.repaint();
    }

    private boolean checkEmpty() {
        if (playerPanels.isEmpty()) {
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
            return true;
        }

        return false;
    }

    private void clearHistory() {
        for (PlayerPanel panel : playerPanels.values()) {
            basePanel.remove(panel);
        }
        playerPanels.clear();
        basePanel.removeAll();
        testRecordCounter = 0;
        checkEmpty();
        basePanel.revalidate();
        basePanel.repaint();

        // Also clear overlay if available
        if (overlay != null) {
            overlay.clearHistory();
        }
    }

    private void addTestRecord() {
        String[] players = partyService.isInParty() ?
            partyService.getMembers().stream().map(PartyMember::getDisplayName).toArray(String[]::new) :
            new String[]{client.getLocalPlayer() != null ? client.getLocalPlayer().getName() : "You", "Player1", "Player2", "Player3"};
        String[] npcs = {"Goblin", "Cow", "Rat", "Spider", "Something that is very long"};
        int[] weaponIds = {27275, 25739, 21003, 25731, 26219, 13652, 27690, 4151, 12926};

        // Cycle through players in order
        String player = players[testRecordCounter % players.length];
        testRecordCounter++;

        int hit = (int)(Math.random() * 50);
        String npc = npcs[(int)(Math.random() * npcs.length)];
        int weaponId = weaponIds[(int)(Math.random() * weaponIds.length)];
        int tickCount = prevTickCount + (int)(Math.random() * 100);
        prevTickCount = tickCount;
        int attackSpeed = 4;
        boolean specialAttack = Math.random() < 0.3;

        PlayerHitRecord record = new PlayerHitRecord(player, hit, npc, weaponId, tickCount, attackSpeed, specialAttack);
        addHit(record);
    }

    public void addTestPlayers() {
        // Add some hardcoded test data for different players
        addHit(new PlayerHitRecord("You", 25, "Goblin", 4151, 100, 4, false));
        addHit(new PlayerHitRecord("You", 18, "Cow", 4151, 104, 4, false));

        addHit(new PlayerHitRecord("Player1", 32, "Dragon", 4587, 200, 5, true));
        addHit(new PlayerHitRecord("Player1", 15, "Goblin", 4587, 205, 5, false));
        addHit(new PlayerHitRecord("Player1", 28, "Spider", 4587, 210, 5, false));

//        addHit(new PlayerHitRecord("Player2", 45, "Demon", 4153, 300, 4, true));
//        addHit(new PlayerHitRecord("Player2", 22, "Rat", 4153, 304, 4, false));

        addHit(new PlayerHitRecord("Player3", 12, "Chicken", -1, 400, 4, false));
    }


}