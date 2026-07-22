package com.arber.pvmtools;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class PvmToolsPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(PvmToolsPlugin.class);
		RuneLite.main(args);
	}
}
