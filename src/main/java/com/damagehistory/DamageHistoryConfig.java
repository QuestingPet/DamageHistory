package com.damagehistory;

import java.awt.*;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup("DamageHistory")
public interface DamageHistoryConfig extends Config {

    enum TickDisplayMode {
        EXTRA_DELAYED_TICKS,
        TOTAL_TICKS;
    }

    @ConfigItem(
            keyName = "tickDisplayMode",
            name = "Tick Display",
            description = "Choose how to display ticks since last attack",
            position = 0
    )
    default TickDisplayMode tickDisplayMode() {
        return TickDisplayMode.EXTRA_DELAYED_TICKS;
    }

    @ConfigItem(
            keyName = "maxHitsToShow",
            name = "Hits to Show (Self)",
            description = "Maximum number of recent hits to display for yourself",
            position = 1
    )
    default int maxHitsToShowSelf() {
        return 10;
    }

    @ConfigItem(
            keyName = "maxHitsToShowOthers",
            name = "Hits to Show (Others)",
            description = "Maximum number of recent hits to display for other players",
            position = 2
    )
    default int maxHitsToShowOthers() {
        return 5;
    }

    @ConfigItem(
            keyName = "clearOnPartyChange",
            name = "Clear on Party Change",
            description = "Clear other players' damage history when joining a new party",
            position = 3
    )
    default boolean clearOnPartyChange() {
        return true;
    }

    @ConfigItem(
            keyName = "expandPanelsByDefault",
            name = "Expand Panels by Default",
            description = "Whether player panels should be expanded by default when a new player is added",
            position = 4
    )
    default boolean expandPanelsByDefault() {
        return true;
    }

    @ConfigItem(
            keyName = "sendDamageHistoryOverParty",
            name = "Send Damage History over party",
            description = "Send damage history information over party",
            position = 5
    )
    default boolean sendDamageHistoryOverParty() {
        return true;
    }

    @ConfigSection(
            name = "Thresholds",
            description = "Configure damage and timing thresholds",
            closedByDefault = true,
            position = 10
    )
    String thresholds = "thresholds";

    @ConfigItem(
            keyName = "tickDelayTolerance",
            name = "Tick Delay Tolerance",
            description = "Number of extra ticks allowed before timing is considered bad",
            position = 11,
            section = thresholds
    )
    default int tickDelayTolerance() {
        return 2;
    }

    @ConfigItem(
            keyName = "mediumDamageThreshold",
            name = "Medium Damage Threshold",
            description = "Minimum damage for medium damage color",
            position = 12,
            section = thresholds
    )
    default int mediumDamageThreshold() {
        return 15;
    }

    @ConfigItem(
            keyName = "highDamageThreshold",
            name = "High Damage Threshold",
            description = "Minimum damage for high damage color",
            position = 13,
            section = thresholds
    )
    default int highDamageThreshold() {
        return 30;
    }

    @ConfigSection(
            name = "Colors",
            description = "Configure damage and timing colors",
            closedByDefault = true,
            position = 20
    )
    String colors = "colors";

    @ConfigItem(
            keyName = "badTimingColor",
            name = "Bad Timing Color",
            description = "Color for attacks with bad timing",
            position = 21,
            section = colors
    )
    default Color badTimingColor() {
        return Color.RED;
    }

    @ConfigItem(
            keyName = "okayTimingColor",
            name = "Okay Timing Color",
            description = "Color for attacks with okay timing",
            position = 22,
            section = colors
    )
    default Color okayTimingColor() {
        return Color.YELLOW;
    }

    @ConfigItem(
            keyName = "goodTimingColor",
            name = "Good Timing Color",
            description = "Color for attacks with good timing",
            position = 23,
            section = colors
    )
    default Color goodTimingColor() {
        return Color.GREEN;
    }

    @ConfigItem(
            keyName = "lowDamageColor",
            name = "Low Damage Color",
            description = "Color for low damage hits",
            position = 24,
            section = colors
    )
    default Color lowDamageColor() {
        return Color.RED;
    }

    @ConfigItem(
            keyName = "mediumDamageColor",
            name = "Medium Damage Color",
            description = "Color for medium damage hits",
            position = 25,
            section = colors
    )
    default Color mediumDamageColor() {
        return Color.YELLOW;
    }

    @ConfigItem(
            keyName = "highDamageColor",
            name = "High Damage Color",
            description = "Color for high damage hits",
            position = 26,
            section = colors
    )
    default Color highDamageColor() {
        return Color.GREEN;
    }

}
