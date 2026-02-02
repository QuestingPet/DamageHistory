package com.damagehistory;

import static com.damagehistory.PredictedHit.AttackStyle.CASTING;
import static com.damagehistory.PredictedHit.AttackStyle.DEFENSIVE_CASTING;
import static com.damagehistory.PredictedHit.AttackStyle.RANGING;
import static net.runelite.api.gameval.VarbitID.AUTOCAST_SET;

import com.damagehistory.panel.DamageHistoryPanel;
import com.damagehistory.panel.PlayerHitRecord;
import com.google.gson.Gson;
import com.google.inject.Provides;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.swing.*;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.NpcID;
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

    private static final String CXD_PLUGIN_NAMESPACE = "customizable-xp-drops";
    private static final String PREDICTED_HIT_MESSAGE = "predicted-hit";
    private static final String CONFIG_GROUP = "DamageHistory";
    private static final int DEFAULT_ATTACK_SPEED = 4;

    private static Set<Integer> NPC_BLOCKLIST = Set.of(
            NpcID.CRYSTAL_HUNLLEF_MELEE,
            NpcID.CRYSTAL_HUNLLEF_RANGED,
            NpcID.CRYSTAL_HUNLLEF_MAGIC,
            NpcID.CRYSTAL_HUNLLEF_MELEE_HM,
            NpcID.CRYSTAL_HUNLLEF_RANGED_HM,
            NpcID.CRYSTAL_HUNLLEF_MAGIC_HM
    );

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
    private boolean isAutoCasting;
    private boolean isManualCasting;

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
        if (!CXD_PLUGIN_NAMESPACE.equals(pluginMessage.getNamespace()) ||
            !PREDICTED_HIT_MESSAGE.equals(pluginMessage.getName())) {
            return;
        }

        Map<String, Object> data = pluginMessage.getData();
        Object json = data.get("value");

        log.debug(json.toString());

        PredictedHit predictedHit = gson.fromJson(json.toString(), PredictedHit.class);
        String playerName = client.getLocalPlayer().getName();
        int currentTick = client.getTickCount();
        processPredictedHit(predictedHit, playerName, currentTick);

        if (config.sendDamageHistoryOverParty() && partyService.isInParty()) {
            partyService.send(new DamageHistoryPartyMessage(predictedHit, playerName, currentTick));
        }
    }

    private void processPredictedHit(PredictedHit predictedHit, String playerName, int serverTick) {
        // Skip PvP hits and invalid NPCs
        if (predictedHit.isOpponentIsPlayer() || predictedHit.getNpcId() == -1) {
            return;
        }

        // Ignore blocked NPCs
        if (NPC_BLOCKLIST.contains(predictedHit.getNpcId())) {
            return;
        }

        int weaponId = predictedHit.getEquippedWeaponId();
        String weaponName = itemManager.getItemComposition(predictedHit.getEquippedWeaponId()).getMembersName();
        int hit = predictedHit.getHit();
        String npcName = client.getNpcDefinition(predictedHit.getNpcId()).getName();

        int attackSpeed = determineAttackSpeed(weaponId, predictedHit.getAttackStyle());
        // Manual casting only lasts one attack max per click
        isManualCasting = false;

        boolean specialAttack = predictedHit.isSpecialAttack();

        log.debug("{} hit {} on {}", weaponName, hit, npcName);

        if (panel != null) {
            // Get the previous hit for this player to calculate tick delay
            PlayerHitRecord previousHit = panel.getLatestHitForPlayer(playerName);

            Integer ticksSincePrevious = null;
            Integer previousAttackSpeed = null;

            if (previousHit != null) {
                ticksSincePrevious = serverTick - previousHit.getTickCount();
                previousAttackSpeed = previousHit.getAttackSpeed();
            }

            PlayerHitRecord record = new PlayerHitRecord(
                    playerName,
                    hit,
                    npcName,
                    weaponId,
                    serverTick,
                    attackSpeed,
                    specialAttack,
                    ticksSincePrevious,
                    previousAttackSpeed
            );
            log.debug(record.toString());
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
                damageHistoryPartyMessage.getPlayerName(),
                damageHistoryPartyMessage.getServerTick())
        );
    }

    @Subscribe
    public void onPartyChanged(PartyChanged partyChanged) {
        if (panel != null && config.clearOnPartyChange()) {
            SwingUtilities.invokeLater(() -> panel.clearOtherPlayers());
        }
    }

    @Subscribe
    public void onMenuOptionClicked(MenuOptionClicked event) {
        if ("Cast".equals(event.getMenuOption()) && event.getMenuAction() == MenuAction.WIDGET_TARGET_ON_NPC) {
            isManualCasting = true;
        } else {
            isManualCasting = false;
        }
    }

    @Subscribe
    public void onVarbitChanged(VarbitChanged varbitChanged) {
        if (varbitChanged.getVarbitId() == AUTOCAST_SET) {
            isAutoCasting = varbitChanged.getValue() == 1;
        }
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged configChanged) {
        if (CONFIG_GROUP.equals(configChanged.getGroup()) && panel != null) {
            log.debug("Config changed: {} = {}", configChanged.getKey(), configChanged.getNewValue());
            SwingUtilities.invokeLater(() -> panel.refreshPanel());
        }
    }

    @Provides
    DamageHistoryConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(DamageHistoryConfig.class);
    }

    private boolean isAutoCastingMage(PredictedHit.AttackStyle attackStyle) {
        boolean hasMageWeapon = attackStyle == CASTING || attackStyle == DEFENSIVE_CASTING;
        return hasMageWeapon && isAutoCasting;
    }

    private int determineAttackSpeed(int weaponId, PredictedHit.AttackStyle attackStyle) {
        if (weaponId == -1) {
            return DEFAULT_ATTACK_SPEED;
        }

        if (isManualCasting) {
            return 5;
        }

        if (isAutoCastingMage(attackStyle)) {
            if (weaponId == ItemID.NIGHTMARE_STAFF_HARMONISED) {
                return 4;
            } else {
                return 5;
            }
        }

        ItemStats itemStats = itemManager.getItemStats(weaponId);
        int attackSpeed = itemStats != null ? itemStats.getEquipment().getAspeed() : DEFAULT_ATTACK_SPEED;

        // Both Accurate and Rapid are reported as RANGING, but assume the player is playing on Rapid, which attacks
        // one tick faster.
        if (attackStyle == RANGING) {
            attackSpeed -= 1;
        }

        return attackSpeed;
    }
}
