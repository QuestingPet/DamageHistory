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
import net.runelite.client.game.ItemManager;
import net.runelite.client.party.PartyMember;
import net.runelite.client.party.PartyService;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.api.Client;

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

    public void addHit(PlayerHitRecord record) {
        PlayerPanel playerPanel = playerPanels.get(record.getPlayerName());
        if (playerPanel == null) {
            playerPanel = new PlayerPanel(record.getPlayerName(), itemManager, config);
            playerPanels.put(record.getPlayerName(), playerPanel);
            
            // Add local player at the top, others at the end
            if (client.getLocalPlayer() != null && client.getLocalPlayer().getName().equals(record.getPlayerName())) {
                playersContainer.add(playerPanel, 0);
            } else {
                playersContainer.add(playerPanel);
            }
            playersContainer.revalidate();
        }
        
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
//        addTestPlayers();
        playersContainer.revalidate();
        playersContainer.repaint();
    }
    
    private void addTestRecord() {
        String[] players = partyService.isInParty() ?
            partyService.getMembers().stream().map(PartyMember::getDisplayName).toArray(String[]::new) :
            new String[]{client.getLocalPlayer() != null ? client.getLocalPlayer().getName() : "You", "Player1", "Player2", "Player3"};
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