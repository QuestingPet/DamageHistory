package com.damagehistory;

import com.damagehistory.panel.DamageHistoryPanel;
import com.damagehistory.panel.PlayerHitRecord;
import com.google.gson.Gson;
import com.google.inject.Provides;
import java.awt.image.BufferedImage;
import java.util.Map;
import javax.inject.Inject;
import javax.swing.*;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.PartyChanged;
import net.runelite.client.events.PluginMessage;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStats;
import net.runelite.client.party.PartyService;
import net.runelite.client.party.WSClient;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

@Slf4j
@PluginDescriptor(
        name = "Damage History"
)
public class DamageHistoryPlugin extends Plugin {

    private static final String CXP_PLUGIN_NAMESPACE = "customizable-xp-drops";
    private static final String PREDICTED_HIT_MESSAGE = "predicted-hit";
    private static final String CONFIG_GROUP = "DamageHistory";
    private static final int DEFAULT_ATTACK_SPEED = 4;

    @Inject
    private Client client;

    @Inject
    private ClientThread clientThread;

    @Inject
    private DamageHistoryConfig config;

    @Inject
    private EventBus eventBus;

    @Inject
    private Gson gson;

    @Inject
    private ItemManager itemManager;

    @Inject
    private ClientToolbar clientToolbar;

    @Inject
    private PartyService partyService;

    @Inject
    private WSClient wsClient;

    private DamageHistoryPanel panel;
    private NavigationButton navButton;
    private final BufferedImage icon = ImageUtil.loadImageResource(DamageHistoryPlugin.class, "panel-icon.png");

    @Override
    protected void startUp() throws Exception {
        panel = injector.getInstance(DamageHistoryPanel.class);

        navButton = NavigationButton.builder()
                .tooltip("Damage History")
                .icon(icon)
                .priority(5)
                .panel(panel)
                .build();

        clientToolbar.addNavigation(navButton);

        try {
            wsClient.registerMessage(DamageHistoryPartyMessage.class);
        } catch (Exception e) {
            log.error("Failed to register party message", e);
        }

        log.debug("Damage History started!");
    }

    @Override
    protected void shutDown() throws Exception {
        clientToolbar.removeNavigation(navButton);
        panel = null;
        log.debug("Damage History stopped!");
    }

    @Subscribe
    public void onPluginMessage(PluginMessage pluginMessage) {
        if (!CXP_PLUGIN_NAMESPACE.equals(pluginMessage.getNamespace()) ||
            !PREDICTED_HIT_MESSAGE.equals(pluginMessage.getName())) {
            return;
        }

        Map<String, Object> data = pluginMessage.getData();
        Object json = data.get("value");

        PredictedHit predictedHit = gson.fromJson(json.toString(), PredictedHit.class);
        String playerName = client.getLocalPlayer().getName();
        processPredictedHit(predictedHit, "Greg M");

        if (config.sendDamageHistoryOverParty()) {
            partyService.send(new DamageHistoryPartyMessage(predictedHit, playerName));
        }
    }

    private void processPredictedHit(PredictedHit predictedHit, String playerName) {
        // Skip PvP hits and invalid NPCs
        if (predictedHit.isOpponentIsPlayer() || predictedHit.getNpcId() == -1) {
            return;
        }

        String weaponName = itemManager.getItemComposition(predictedHit.getEquippedWeaponId()).getMembersName();
        int hit = predictedHit.getHit();
        String npcName = client.getNpcDefinition(predictedHit.getNpcId()).getName();
        int weaponId = predictedHit.getEquippedWeaponId();
        ItemStats itemStats = weaponId == -1 ? null : itemManager.getItemStats(weaponId);
        int attackSpeed = itemStats != null ? itemStats.getEquipment().getAspeed() : DEFAULT_ATTACK_SPEED;

        // Both Accurate and Rapid are reported as RANGING, but assume the player is playing on Rapid, which
        // attacks one tick faster.
        if (predictedHit.getAttackStyle() == PredictedHit.AttackStyle.RANGING) {
            attackSpeed -= 1;
        }
        boolean specialAttack = predictedHit.isSpecialAttack();

        log.debug("{} hit {} on {}", weaponName, hit, npcName);

        if (panel != null) {
            // Get the previous hit for this player to calculate tick delay
            PlayerHitRecord previousHit = panel.getLatestHitForPlayer(playerName);

            Integer ticksSincePrevious = null;
            Integer previousAttackSpeed = null;

            if (previousHit != null) {
                ticksSincePrevious = client.getTickCount() - previousHit.getTickCount();
                previousAttackSpeed = previousHit.getAttackSpeed();
            }

            PlayerHitRecord record = new PlayerHitRecord(
                    playerName,
                    hit,
                    npcName,
                    weaponId,
                    client.getTickCount(),
                    attackSpeed,
                    specialAttack,
                    ticksSincePrevious,
                    previousAttackSpeed
            );
            SwingUtilities.invokeLater(() -> {
                panel.addHit(record);
            });
        }
    }

    @Subscribe
    public void onDamageHistoryPartyMessage(DamageHistoryPartyMessage damageHistoryPartyMessage) {
        // Ignore incoming party messages from local player, since those are already handled.
        if (client.getLocalPlayer() != null && client.getLocalPlayer().getName() != null &&
            client.getLocalPlayer().getName().equals(damageHistoryPartyMessage.getPlayerName())) {
            return;
        }

        clientThread.invoke(() -> processPredictedHit(
                damageHistoryPartyMessage.getPredictedHit(),
                damageHistoryPartyMessage.getPlayerName())
        );
    }

    @Subscribe
    public void onPartyChanged(PartyChanged partyChanged) {
        if (panel != null && config.clearOnPartyChange()) {
            SwingUtilities.invokeLater(() -> panel.clearOtherPlayers());
        }
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged configChanged) {
        if (CONFIG_GROUP.equals(configChanged.getGroup()) && panel != null) {
            SwingUtilities.invokeLater(() -> panel.refreshPanel());
        }
    }

    @Provides
    DamageHistoryConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(DamageHistoryConfig.class);
    }
}
