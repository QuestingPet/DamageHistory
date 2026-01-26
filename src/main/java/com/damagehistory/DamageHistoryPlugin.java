package com.damagehistory;

import com.damagehistory.panel.DamageHistoryPanel;
import com.google.gson.Gson;
import com.google.inject.Provides;
import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.PluginMessage;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

import javax.swing.SwingUtilities;
import java.awt.image.BufferedImage;
import java.util.Map;

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
        if (!PLUGIN_NAMESPACE.equals(pluginMessage.getNamespace()) ||
            !PREDICTED_HIT_MESSAGE.equals(pluginMessage.getName())) {
            return;
        }

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
        var itemStats = itemManager.getItemStats(predictedHit.getEquippedWeaponId());
        int attackSpeed = itemStats != null ? itemStats.getEquipment().getAspeed() : DEFAULT_ATTACK_SPEED;
        log.debug(itemStats.toString());
        boolean specialAttack = predictedHit.isSpecialAttack();

        log.debug("{} hit {} on {}", weaponName, hit, npcName);
        
        if (panel != null) {
            SwingUtilities.invokeLater(() -> 
                panel.addHit(hit, npcName, predictedHit.getEquippedWeaponId(), client.getTickCount(), attackSpeed, specialAttack)
            );
        }
    }

    @Subscribe
    public void onHitsplatApplied(HitsplatApplied hitsplatApplied) {
        // log the hitsplat amount on actor
        log.debug("Hitsplat: {} - {}", hitsplatApplied.getActor().getName(), hitsplatApplied.getHitsplat().getAmount());
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
