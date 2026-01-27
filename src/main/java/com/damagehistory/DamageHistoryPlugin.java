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
import net.runelite.api.events.HitsplatApplied;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.PartyChanged;
import net.runelite.client.events.PluginMessage;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStats;
import net.runelite.client.party.PartyMember;
import net.runelite.client.party.PartyService;
import net.runelite.client.party.WSClient;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ImageUtil;

@Slf4j
@PluginDescriptor(
        name = "Damage History"
)
public class DamageHistoryPlugin extends Plugin {

    private static final String PLUGIN_NAMESPACE = "customizable-xp-drops";
    private static final String PREDICTED_HIT_MESSAGE = "predicted-hit";
    private static final String CONFIG_GROUP = "DamageHistory";
    private static final int DEFAULT_ATTACK_SPEED = 4;

    @Inject
    private Client client;

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

    @Inject
    private OverlayManager overlayManager;

    private DamageHistoryPanel panel;
    private NavigationButton navButton;
    private final BufferedImage icon = ImageUtil.loadImageResource(DamageHistoryPlugin.class, "panel-icon.png");
    private int playerCounter = 0;

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

        // Initialize test data after panel is fully constructed
//        SwingUtilities.invokeLater(() -> panel.addTestPlayers());

        log.debug("Damage History started!");
        debugReinit();
    }

    @Override
    protected void shutDown() throws Exception {
        clientToolbar.removeNavigation(navButton);
        panel = null;
        log.debug("Damage History stopped!");
    }

    private void debugReinit() {
//        panel = new DamageHistoryPanel();
        panel.setClient(client);
        panel.setPartyService(partyService);
    }

    @Subscribe
    public void onPluginMessage(PluginMessage pluginMessage) {
        if (!PLUGIN_NAMESPACE.equals(pluginMessage.getNamespace()) ||
            !PREDICTED_HIT_MESSAGE.equals(pluginMessage.getName())) {
            return;
        }

        log.debug("in party: {}", partyService.isInParty());
        try {
            processPredictedHit(pluginMessage);
        } catch (Exception e) {
            log.error("Error processing predicted hit", e);
        }
    }

    private void processPredictedHit(PluginMessage pluginMessage) {
        Map<String, Object> data = pluginMessage.getData();
        Object json = data.get("value");
        log.debug(json.toString());

        PredictedHit predictedHit = gson.fromJson(json.toString(), PredictedHit.class);

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
//        log.debug(itemStats.toString());
        boolean specialAttack = predictedHit.isSpecialAttack();

        log.debug("{} hit {} on {}", weaponName, hit, npcName);

        String[] players = partyService.isInParty() ? 
            partyService.getMembers().stream().map(PartyMember::getDisplayName).toArray(String[]::new) :
            new String[]{ client.getLocalPlayer().getName(), "Player1", "Player2" };
        String player = players[playerCounter % players.length];
        playerCounter++;

        if (panel != null) {
            // Get the previous hit for this player to calculate tick delay
            PlayerHitRecord previousHit = panel.getLatestHitForPlayer(player);
            
            Integer ticksSincePrevious = null;
            Integer previousAttackSpeed = null;
            
            if (previousHit != null) {
                ticksSincePrevious = client.getTickCount() - previousHit.getTickCount();
                previousAttackSpeed = previousHit.getAttackSpeed();
            }
            
            PlayerHitRecord record = new PlayerHitRecord(
                    player,
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
    public void onHitsplatApplied(HitsplatApplied hitsplatApplied) {
        // log the hitsplat amount on actor
//        log.debug("Hitsplat: {} - {}", hitsplatApplied.getActor().getName(), hitsplatApplied.getHitsplat().getAmount());
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
