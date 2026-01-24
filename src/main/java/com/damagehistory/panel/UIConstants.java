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
    
    // Damage thresholds
    public static final int HIGH_DAMAGE_THRESHOLD = 30;
    public static final int MEDIUM_DAMAGE_THRESHOLD = 15;
    
    // Tick delay tolerance
    public static final int TICK_DELAY_TOLERANCE = 2;
    
    // Colors
    public static final Color HIGH_DAMAGE_COLOR = Color.GREEN;
    public static final Color MEDIUM_DAMAGE_COLOR = Color.YELLOW;
    public static final Color LOW_DAMAGE_COLOR = Color.RED;
    
    public static final Color GOOD_TIMING_COLOR = Color.GREEN;
    public static final Color OKAY_TIMING_COLOR = Color.YELLOW;
    public static final Color BAD_TIMING_COLOR = Color.RED;
    
    private UIConstants() {
        // Utility class
    }
}