package com.damagehistory;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class DamageHistoryPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(DamageHistoryPlugin.class);
		RuneLite.main(args);
	}
}