package com.arber.pvmtools;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Locale;
import org.junit.Test;

import net.runelite.api.gameval.NpcID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
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
	public void abhorrentSpectreHintOnlyNamesItsProtectiveGear()
	{
		SuperiorSlayerHint hint = PvmToolsPlugin.getSuperiorSlayerHint(NpcID.SUPERIOR_ABBERANT_SPECTRE);

		assertNotNull(hint);
		assertEquals("Wear nose protection.", hint.getTip());
	}

	@Test
	public void noSuperiorHintAdvisesAPrayer() throws Exception
	{
		int hints = 0;
		for (Field field : NpcID.class.getFields())
		{
			if (!Modifier.isStatic(field.getModifiers()) || field.getType() != int.class)
			{
				continue;
			}
			SuperiorSlayerHint hint = PvmToolsPlugin.getSuperiorSlayerHint(field.getInt(null));
			if (hint == null)
			{
				continue;
			}
			hints++;
			assertTrue(field.getName(), PvmToolsPlugin.isSuperiorSlayerMonster(field.getInt(null)));
			assertFalse(field.getName() + ": " + hint.getTip(),
				hint.getTip().toLowerCase(Locale.ENGLISH).contains("pray"));
		}
		assertTrue(hints > 10);
	}

	@Test
	public void superiorWithoutAHintStillCountsAsASuperiorSpawn()
	{
		assertNull(PvmToolsPlugin.getSuperiorSlayerHint(NpcID.SUPERIOR_CRAWLING_HAND));
		assertTrue(PvmToolsPlugin.isSuperiorSlayerMonster(NpcID.SUPERIOR_CRAWLING_HAND));
		assertFalse(PvmToolsPlugin.isSuperiorSlayerMonster(NpcID.SLAYER_NECHRYAEL_SPAWN));
	}
}
