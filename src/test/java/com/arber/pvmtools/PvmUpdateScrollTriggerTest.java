package com.arber.pvmtools;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PvmUpdateScrollTriggerTest
{
	@Test
	public void newVersionTriggersUpdateScroll()
	{
		assertTrue(PvmToolsPlugin.shouldShowUpdateScroll("1.4.3", "1.4.2", false, false));
	}

	@Test
	public void sameVersionDoesNotTriggerTwice()
	{
		assertFalse(PvmToolsPlugin.shouldShowUpdateScroll("1.4.3", "1.4.3", false, false));
	}

	@Test
	public void explicitOptOutIsRespected()
	{
		assertFalse(PvmToolsPlugin.shouldShowUpdateScroll("1.4.3", "1.4.2", true, false));
	}

	@Test
	public void previewOverridesSeenVersionAndOptOut()
	{
		assertTrue(PvmToolsPlugin.shouldShowUpdateScroll("1.4.3", "1.4.3", true, true));
	}
}
