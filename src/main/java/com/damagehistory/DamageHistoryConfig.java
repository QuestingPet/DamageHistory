package com.damagehistory;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("DamageHistory")
public interface DamageHistoryConfig extends Config
{
	enum TickDisplayMode
	{
		TOTAL_TICKS,
		EXTRA_DELAYED_TICKS;
	}

	@ConfigItem(
		keyName = "tickDisplayMode",
		name = "Tick Display",
		description = "Choose how to display ticks since last attack"
	)
	default TickDisplayMode tickDisplayMode()
	{
		return TickDisplayMode.TOTAL_TICKS;
	}

	@ConfigItem(
		keyName = "debugMode",
		name = "Debug Mode",
		description = "Show border colors for debugging layout"
	)
	default boolean debugMode()
	{
		return false;
	}

	@ConfigItem(
		keyName = "maxHitsToShow",
		name = "Hits to Show (Self)",
		description = "Maximum number of recent hits to display for yourself"
	)
	default int maxHitsToShow()
	{
		return 5;
	}

	@ConfigItem(
		keyName = "maxHitsToShowOthers",
		name = "Hits to Show (Others)",
		description = "Maximum number of recent hits to display for other players"
	)
	default int maxHitsToShowOthers()
	{
		return 5;
	}

	@ConfigItem(
		keyName = "showOverlay",
		name = "Show Overlay",
		description = "Display recent hits overlay on screen"
	)
	default boolean showOverlay()
	{
		return true;
	}

	@ConfigItem(
		keyName = "clearOnPartyChange",
		name = "Clear on Party Change",
		description = "Clear other players' damage history when joining a new party"
	)
	default boolean clearOnPartyChange()
	{
		return true;
	}
}
