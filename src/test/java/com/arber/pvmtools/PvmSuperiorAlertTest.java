package com.arber.pvmtools;

import org.junit.Test;

import net.runelite.api.gameval.NpcID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class PvmSuperiorAlertTest
{
	@Test
	public void requiresSpawnAndPersonalChatMessageCloseTogether()
	{
		assertTrue(PvmToolsPlugin.isSuperiorSpawnConfirmation(100, 100));
		assertTrue(PvmToolsPlugin.isSuperiorSpawnConfirmation(100, 103));
		assertTrue(PvmToolsPlugin.isSuperiorSpawnConfirmation(103, 100));
		assertFalse(PvmToolsPlugin.isSuperiorSpawnConfirmation(100, -1));
		assertFalse(PvmToolsPlugin.isSuperiorSpawnConfirmation(-1, 100));
		assertFalse(PvmToolsPlugin.isSuperiorSpawnConfirmation(100, 104));
	}

	@Test
	public void abhorrentSpectreUsesMagicPrayerHint()
	{
		SuperiorSlayerHint hint = PvmToolsPlugin.getSuperiorSlayerHint(NpcID.SUPERIOR_ABBERANT_SPECTRE);

		assertNotNull(hint);
		assertEquals("Magic", hint.getPrayer());
		assertTrue(hint.getTip().contains("magic prayer"));
	}
}
