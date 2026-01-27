package com.damagehistory;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

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
        return 5;
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
            keyName = "debugMode",
            name = "Border Debug Mode",
            description = "Show border colors for debugging layout",
            position = 100
    )
    default boolean debugMode() {
        return false;
    }

}
