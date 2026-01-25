package com.damagehistory.panel;

import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.image.BufferedImage;

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
        
        int panelHeight = isRecent ? UIConstants.PANEL_HEIGHT + 16 : UIConstants.PANEL_HEIGHT;
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, panelHeight));
        panel.setPreferredSize(new Dimension(0, panelHeight));
        
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
    
    public static BufferedImage addOutline(BufferedImage image, Color outlineColor) {
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage outlined = new BufferedImage(width + 2, height + 2, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = outlined.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Create mask from original image
        BufferedImage mask = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D maskG2d = mask.createGraphics();
        maskG2d.drawImage(image, 0, 0, null);
        maskG2d.setComposite(AlphaComposite.SrcIn);
        maskG2d.setColor(outlineColor);
        maskG2d.fillRect(0, 0, width, height);
        maskG2d.dispose();
        
        // Draw outline by drawing the mask offset in all 8 directions
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                if (x != 0 || y != 0) {
                    g2d.drawImage(mask, x + 1, y + 1, null);
                }
            }
        }
        
        // Draw original image on top
        g2d.drawImage(image, 1, 1, null);
        g2d.dispose();
        
        return outlined;
    }
    
    private UIUtils() {
        // Utility class
    }
}