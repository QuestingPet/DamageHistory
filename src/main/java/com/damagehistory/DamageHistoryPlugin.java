package com.damagehistory;

import com.google.gson.Gson;
import com.google.inject.Provides;
import javax.inject.Inject;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Hitsplat;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.NpcID;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.PluginMessage;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.NPCManager;
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
    public void onGameStateChanged(GameStateChanged gameStateChanged) {

    }

    @Subscribe
    public void onGameTick(GameTick gameTick) {
        //		log.debug("test");
    }

    @Subscribe
    public void onHitsplatApplied(HitsplatApplied event) {
        log.debug(event.toString());
    }

    @Subscribe
    public void onPluginMessage(PluginMessage pluginMessage) {
        log.debug(pluginMessage.toString());
        if ("customizable-xp-drops".equals(pluginMessage.getNamespace()) &&
            "predicted-hit".equals(pluginMessage.getName())) {

            Map<String, Object> data = pluginMessage.getData();
            Object json = data.get("value");
            log.debug(json.toString());
            PredictedHit predictedHit = gson.fromJson(json.toString(), PredictedHit.class);
            String weaponName = itemManager.getItemComposition(predictedHit.getEquippedWeaponId()).getMembersName();
            int hit = predictedHit.getHit();
            String npcName = client.getNpcDefinition(predictedHit.getNpcId()).getName();
            int attackSpeed = itemManager.getItemStats(predictedHit.getEquippedWeaponId()).getEquipment().getAspeed();
            log.debug("{} hit {} on {}", weaponName, hit, npcName);
            
            if (panel != null) {
                SwingUtilities.invokeLater(() -> panel.addHit(weaponName, hit, npcName, predictedHit.getEquippedWeaponId(), client.getTickCount(), attackSpeed));
            }
        }
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged configChanged) {
        if ("DamageHistory".equals(configChanged.getGroup()) && panel != null) {
            SwingUtilities.invokeLater(() -> panel.refreshPanel());
        }
    }

    @Provides
    DamageHistoryConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(DamageHistoryConfig.class);
    }
}
