package com.arber.pvmtools;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PvmUpdateScrollTriggerTest
{
	@Test
	public void newVersionTriggersUpdateScroll()
	{
		assertTrue(PvmToolsPlugin.shouldShowUpdateScroll("1.4.5", "1.4.4", false, false));
	}

	@Test
	public void sameVersionDoesNotTriggerTwice()
	{
		assertFalse(PvmToolsPlugin.shouldShowUpdateScroll("1.4.5", "1.4.5", false, false));
	}

	@Test
	public void explicitOptOutIsRespected()
	{
		assertFalse(PvmToolsPlugin.shouldShowUpdateScroll("1.4.5", "1.4.4", true, false));
	}

	@Test
	public void previewOverridesSeenVersionAndOptOut()
	{
		assertTrue(PvmToolsPlugin.shouldShowUpdateScroll("1.4.5", "1.4.5", true, true));
	}
}
