package com.arber.pvmtools;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
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
}
