package com.damagehistory.panel;

import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public final class UIUtils {
    
    public static Color getDamageColor(int damage) {
        if (damage >= UIConstants.HIGH_DAMAGE_THRESHOLD) return UIConstants.HIGH_DAMAGE_COLOR;
        if (damage >= UIConstants.MEDIUM_DAMAGE_THRESHOLD) return UIConstants.MEDIUM_DAMAGE_COLOR;
        return UIConstants.LOW_DAMAGE_COLOR;
    }

    public static Color getTickDelayColor(int ticksSince, int attackSpeed) {
        if (ticksSince <= attackSpeed) return UIConstants.GOOD_TIMING_COLOR;
        if (ticksSince <= attackSpeed + UIConstants.TICK_DELAY_TOLERANCE) return UIConstants.OKAY_TIMING_COLOR;
        return UIConstants.BAD_TIMING_COLOR;
    }
    
    public static JLabel createStyledLabel(String text, int alignment, Font font, Color color, Dimension size) {
        JLabel label = new JLabel(text, alignment);
        if (font != null) label.setFont(font);
        if (color != null) label.setForeground(color);
        if (size != null) {
            label.setPreferredSize(size);
            label.setMinimumSize(size);
        }
        return label;
    }
    
    public static JPanel createHitPanelBase(boolean isRecent) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, UIConstants.PANEL_HEIGHT));
        panel.setPreferredSize(new Dimension(0, UIConstants.PANEL_HEIGHT));
        
        Color borderColor = isRecent ? ColorScheme.BRAND_ORANGE : ColorScheme.DARK_GRAY_COLOR;
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, borderColor),
                BorderFactory.createEmptyBorder(4, UIConstants.BORDER_PADDING, 4, UIConstants.BORDER_PADDING)
        ));
        
        return panel;
    }
    
    public static void addDebugBorder(JComponent component, Color color, boolean debugMode) {
        if (debugMode) {
            component.setBorder(BorderFactory.createLineBorder(color));
        }
    }
    
    public static void addDebugBorderWithPadding(JComponent component, Color color, boolean debugMode, int leftPadding) {
        if (debugMode) {
            component.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(color),
                    new EmptyBorder(0, leftPadding, 0, 0)
            ));
        } else {
            component.setBorder(new EmptyBorder(0, leftPadding, 0, 0));
        }
    }
    
    public static Color applyAlpha(Color color, float alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), (int)(255 * alpha));
    }
    
    private UIUtils() {
        // Utility class
    }
}