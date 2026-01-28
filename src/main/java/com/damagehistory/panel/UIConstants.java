package com.damagehistory.panel;

import java.awt.*;

public final class UIConstants {
    // Panel dimensions
    public static final int PANEL_HEIGHT = 54;
    public static final int MAX_HIT_RECORDS = 50;
    
    // Padding values
    public static final int COLUMN_PADDING = 4;
    public static final int BORDER_PADDING = 8;
    
    // Alpha values
    public static final float RECENT_HIT_ALPHA = 1.0f;
    public static final float OLD_HIT_ALPHA = 0.9f;
    
    // Special attack outline color (not configurable)
    public static final Color SPECIAL_ATTACK_OUTLINE_COLOR = new Color(55, 160, 186);
    
    // Icon dimensions
    public static final int ICON_SIZE = 36;
    
    private UIConstants() {
        // Utility class
    }
}