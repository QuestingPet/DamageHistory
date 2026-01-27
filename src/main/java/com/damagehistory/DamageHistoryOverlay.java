package com.damagehistory;

import com.damagehistory.panel.PlayerHitRecord;
import com.damagehistory.panel.TickInfo;
import com.damagehistory.panel.UIUtils;
import net.runelite.api.Client;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.util.AsyncBufferedImage;
import net.runelite.client.util.ImageUtil;

import javax.inject.Inject;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DamageHistoryOverlay extends Overlay {
    private static final BufferedImage FIST_IMAGE = ImageUtil.loadImageResource(DamageHistoryOverlay.class, "fist.png");
    private static final int ICON_SIZE = 16;
    private static final int ROW_HEIGHT = 20;
    private static final int PADDING = 4;
    private static final Color SPECIAL_ATTACK_OUTLINE_COLOR = new Color(55, 160, 186);
    
    private final Client client;
    private final DamageHistoryConfig config;
    private final ItemManager itemManager;
    private final Map<String, List<PlayerHitRecord>> hitsByPlayer = new HashMap<>();
    private final Map<String, BufferedImage> weaponImageCache = new HashMap<>();

    @Inject
    public DamageHistoryOverlay(Client client, DamageHistoryConfig config, ItemManager itemManager) {
        this.client = client;
        this.config = config;
        this.itemManager = itemManager;
        setPosition(OverlayPosition.TOP_LEFT);
        setPriority(OverlayPriority.LOW);
        setResizable(true);
    }

    public void addHit(PlayerHitRecord record) {
        hitsByPlayer.computeIfAbsent(record.getPlayerName(), k -> new ArrayList<>()).add(0, record);
        
        List<PlayerHitRecord> playerHits = hitsByPlayer.get(record.getPlayerName());
        if (playerHits.size() > 5) {
            playerHits.remove(playerHits.size() - 1);
        }
        
        String cacheKey = record.getWeaponId() + "_" + record.isSpecialAttack();
        if (!weaponImageCache.containsKey(cacheKey)) {
            BufferedImage weaponImage;
            if (record.getWeaponId() == -1) {
                weaponImage = FIST_IMAGE;
            } else {
                AsyncBufferedImage asyncImage = itemManager.getImage(record.getWeaponId());
                weaponImage = asyncImage;
            }
            
            if (record.isSpecialAttack() && weaponImage != null) {
                weaponImage = addOutline(weaponImage, SPECIAL_ATTACK_OUTLINE_COLOR);
            }
            
            weaponImageCache.put(cacheKey, weaponImage);
        }
    }

    public void clearHistory() {
        hitsByPlayer.clear();
        weaponImageCache.clear();
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        if (hitsByPlayer.isEmpty()) {
            return null;
        }

        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        int y = PADDING;
        int maxWidth = 200;
        
        // Title
        graphics.setColor(Color.WHITE);
        graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
        graphics.drawString("Recent Hits", PADDING, y + 10);
        y += 16;
        
        // Calculate content height
        int contentHeight = y;
        for (Map.Entry<String, List<PlayerHitRecord>> entry : hitsByPlayer.entrySet()) {
            contentHeight += 12 + ROW_HEIGHT + 2; // player name + hit row + spacing
        }
        contentHeight += PADDING;
        
        // Background
        graphics.setColor(ColorScheme.DARK_GRAY_COLOR);
        graphics.fillRect(0, 0, maxWidth, contentHeight);
        graphics.setColor(ColorScheme.DARKER_GRAY_COLOR);
        graphics.drawRect(0, 0, maxWidth - 1, contentHeight - 1);
        
        // Reset y for actual rendering
        y = PADDING + 16;
        
        // Render each player's most recent hit
        for (Map.Entry<String, List<PlayerHitRecord>> entry : hitsByPlayer.entrySet()) {
            String playerName = entry.getKey();
            List<PlayerHitRecord> hits = entry.getValue();
            if (!hits.isEmpty()) {
                PlayerHitRecord mostRecent = hits.get(0);
                y = renderPlayerSection(graphics, playerName, mostRecent, hits, y, maxWidth);
            }
        }
        
        return new Dimension(maxWidth, contentHeight);
    }
    
    private int renderPlayerSection(Graphics2D graphics, String playerName, PlayerHitRecord record, List<PlayerHitRecord> allHits, int y, int maxWidth) {
        // Player name header
        graphics.setColor(Color.WHITE);
        graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 9));
        graphics.drawString(playerName, PADDING, y + 8);
        y += 12;
        
        // Hit record row background
        graphics.setColor(ColorScheme.DARKER_GRAY_COLOR);
        graphics.fillRect(PADDING, y, maxWidth - PADDING * 2, ROW_HEIGHT);
        
        int currentX = PADDING + 2;
        int centerY = y + ROW_HEIGHT / 2;
        
        // Weapon icon
        String cacheKey = record.getWeaponId() + "_" + record.isSpecialAttack();
        BufferedImage weaponImage = weaponImageCache.get(cacheKey);
        if (weaponImage != null) {
            graphics.drawImage(weaponImage, currentX, centerY - ICON_SIZE / 2, ICON_SIZE, ICON_SIZE, null);
        }
        currentX += ICON_SIZE + 4;
        
        // Damage amount
        graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
        graphics.setColor(getDamageColor(record.getHit()));
        graphics.drawString(String.valueOf(record.getHit()), currentX, centerY + 3);
        currentX += 25;
        
        // NPC name
        graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 9));
        graphics.setColor(Color.WHITE);
        String npcName = record.getNpcName();
        if (npcName.length() > 12) {
            npcName = npcName.substring(0, 12) + "...";
        }
        graphics.drawString(npcName, currentX, centerY + 3);
        
        // Tick delay (right aligned)
        TickInfo tickInfo = calculateTickInfo(record, allHits);
        if (!tickInfo.text.isEmpty()) {
            graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 9));
            graphics.setColor(tickInfo.color);
            int tickWidth = graphics.getFontMetrics().stringWidth(tickInfo.text);
            graphics.drawString(tickInfo.text, maxWidth - PADDING - tickWidth - 2, centerY + 3);
        }
        
        return y + ROW_HEIGHT + 2;
    }
    
    private Color getDamageColor(int damage) {
        if (damage >= 30) return Color.GREEN;
        if (damage >= 15) return Color.YELLOW;
        return Color.RED;
    }
    
    private Color getTickDelayColor(int ticksSince, int attackSpeed) {
        if (ticksSince <= attackSpeed) return Color.GREEN;
        if (ticksSince <= attackSpeed + 2) return Color.YELLOW;
        return Color.RED;
    }
    
    private TickInfo calculateTickInfo(PlayerHitRecord record, List<PlayerHitRecord> allHits) {
        if (allHits.size() < 2) {
            return new TickInfo("", Color.WHITE);
        }
        
        PlayerHitRecord previousHit = allHits.get(1);
        int ticksSince = record.getTickCount() - previousHit.getTickCount();
        int previousAttackSpeed = previousHit.getAttackSpeed();
        
        String tickText;
        if (config.tickDisplayMode() == DamageHistoryConfig.TickDisplayMode.EXTRA_DELAYED_TICKS) {
            int extraTicks = ticksSince - previousAttackSpeed;
            tickText = String.format("+%dt", extraTicks);
        } else {
            tickText = String.format("+%dt", ticksSince);
        }
        
        Color tickColor = getTickDelayColor(ticksSince, previousAttackSpeed);
        return new TickInfo(tickText, tickColor);
    }
    
    private BufferedImage addOutline(BufferedImage image, Color outlineColor) {
        if (image == null) return null;
        
        BufferedImage outlined = new BufferedImage(image.getWidth() + 2, image.getHeight() + 2, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = outlined.createGraphics();
        
        // Draw outline
        g2d.setColor(outlineColor);
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                if (x != 0 || y != 0) {
                    g2d.drawImage(image, x + 1, y + 1, null);
                }
            }
        }
        
        // Draw original image on top
        g2d.drawImage(image, 1, 1, null);
        g2d.dispose();
        
        return outlined;
    }
}