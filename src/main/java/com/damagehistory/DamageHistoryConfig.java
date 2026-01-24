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
}
