package com.undyingretributiontimer;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class UndyingRetributionTimerPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(UndyingRetributionTimerPlugin.class);
		RuneLite.main(args);
	}
}