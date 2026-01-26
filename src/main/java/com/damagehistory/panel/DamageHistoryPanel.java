package com.damagehistory.panel;

import com.damagehistory.DamageHistoryConfig;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;

@Singleton
public class DamageHistoryPanel extends PluginPanel {
    @Inject
    private ItemManager itemManager;

    @Inject
    private DamageHistoryConfig config;

    private final JPanel playersContainer = new JPanel();
    private final Map<String, PlayerPanel> playerPanels = new HashMap<>();
    private int testRecordCounter = 0;

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

        playersContainer.setLayout(new BoxLayout(playersContainer, BoxLayout.Y_AXIS));
        playersContainer.setBackground(ColorScheme.DARK_GRAY_COLOR);

        JScrollPane scrollPane = new JScrollPane(playersContainer);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        
        layoutPanel.add(scrollPane);
    }

    public void addHit(int hit, String npcName, int weaponId, int tickCount, int attackSpeed, boolean specialAttack) {
        addHitForPlayer("You", hit, npcName, weaponId, tickCount, attackSpeed, specialAttack);
    }
    
    public void addHitForPlayer(String playerName, int hit, String npcName, int weaponId, int tickCount, int attackSpeed, boolean specialAttack) {
        PlayerPanel playerPanel = playerPanels.get(playerName);
        if (playerPanel == null) {
            playerPanel = new PlayerPanel(playerName, itemManager, config);
            playerPanels.put(playerName, playerPanel);
            playersContainer.add(playerPanel);
            playersContainer.revalidate();
        }
        
        PlayerHitRecord record = new PlayerHitRecord(playerName, hit, npcName, weaponId, tickCount, attackSpeed, specialAttack);
        playerPanel.addHit(record);
    }

    public void refreshPanel() {
        for (PlayerPanel playerPanel : playerPanels.values()) {
            playerPanel.refreshPanel();
        }
        playersContainer.revalidate();
        playersContainer.repaint();
    }

    private void clearHistory() {
        for (PlayerPanel panel : playerPanels.values()) {
            playersContainer.remove(panel);
        }
        playerPanels.clear();
        playersContainer.removeAll();
        testRecordCounter = 0;
        addTestPlayers();
        playersContainer.revalidate();
        playersContainer.repaint();
    }
    
    private void addTestRecord() {
        String[] players = {"You", "Player1", "Player2", "Player3"};
        String[] npcs = {"Goblin", "Cow", "Rat", "Spider", "Something that is very long"};

        // Cycle through players in order
        String player = players[testRecordCounter % players.length];
        testRecordCounter++;
        
        int hit = (int)(Math.random() * 50);
        String npc = npcs[(int)(Math.random() * npcs.length)];
        int weaponId = 4151; // Whip ID
        int tickCount = (int)(Math.random() * 1000);
        int attackSpeed = 4;
        boolean specialAttack = Math.random() < 0.3;
        
        addHitForPlayer(player, hit, npc, weaponId, tickCount, attackSpeed, specialAttack);
    }
    
    public void addTestPlayers() {
        // Add some hardcoded test data for different players
        addHitForPlayer("You", 25, "Goblin", 4151, 100, 4, false);
        addHitForPlayer("You", 18, "Cow", 4151, 104, 4, false);
        
        addHitForPlayer("Player1", 32, "Dragon", 4587, 200, 5, true);
        addHitForPlayer("Player1", 15, "Goblin", 4587, 205, 5, false);
        addHitForPlayer("Player1", 28, "Spider", 4587, 210, 5, false);
        
//        addHitForPlayer("Player2", 45, "Demon", 4153, 300, 4, true);
//        addHitForPlayer("Player2", 22, "Rat", 4153, 304, 4, false);
        
        addHitForPlayer("Player3", 12, "Chicken", -1, 400, 4, false);
    }


}