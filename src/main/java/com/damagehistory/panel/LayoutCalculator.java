package com.damagehistory.panel;

import java.awt.*;
import java.util.List;
import net.runelite.client.ui.FontManager;

public final class LayoutCalculator {

    public static class ColumnWidths {

        public final int damageWidth;
        public final int npcWidth;
        public final int tickWidth;

        public ColumnWidths(int damageWidth, int npcWidth, int tickWidth) {
            this.damageWidth = damageWidth;
            this.npcWidth = npcWidth;
            this.tickWidth = tickWidth;
        }
    }

    public static ColumnWidths calculateColumnWidths(List<HitRecord> hitRecords, Component component) {
        FontMetrics damageFM = component.getFontMetrics(FontManager.getRunescapeBoldFont());
        FontMetrics regularFM = component.getFontMetrics(FontManager.getRunescapeFont());

        int maxDamageWidth = 0;
        int maxNpcWidth = 0;
        int maxTickWidth = 0;

        for (int i = 0; i < hitRecords.size(); i++) {
            HitRecord record = hitRecords.get(i);

            // Calculate damage width
            int damageWidth = damageFM.stringWidth(String.valueOf(record.getHit()));
            maxDamageWidth = Math.max(maxDamageWidth, damageWidth);

            // Calculate NPC name width
            int npcWidth = regularFM.stringWidth(record.getNpcName());
            maxNpcWidth = Math.max(maxNpcWidth, npcWidth);

            // Calculate tick info width
            if (i < hitRecords.size() - 1) {
                int ticksSince = record.getTickCount() - hitRecords.get(i + 1).getTickCount();
                String tickInfo = String.format(" +%dt", ticksSince);
                int tickWidth = regularFM.stringWidth(tickInfo);
                maxTickWidth = Math.max(maxTickWidth, tickWidth);
            }
        }

        // Add padding
        return new ColumnWidths(
                maxDamageWidth + UIConstants.COLUMN_PADDING,
                maxNpcWidth + UIConstants.COLUMN_PADDING,
                maxTickWidth + UIConstants.COLUMN_PADDING
        );
    }

    private LayoutCalculator() {
        // Utility class
    }
}