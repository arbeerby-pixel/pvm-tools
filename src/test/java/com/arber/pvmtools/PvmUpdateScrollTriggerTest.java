package com.arber.pvmtools;

import java.lang.reflect.Method;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PvmUpdateScrollTriggerTest
{
	@Test
	public void bundledReleaseVersionTriggersAgainstPreviousRelease() throws Exception
	{
		Method method = PvmToolsPlugin.class.getDeclaredMethod("getPluginVersion");
		method.setAccessible(true);
		String bundledVersion = (String) method.invoke(new PvmToolsPlugin());
		assertEquals("1.4.8", bundledVersion);
		assertTrue(PvmToolsPlugin.shouldShowUpdateScroll(bundledVersion, "1.4.7", false, false));
	}

	@Test
	public void newVersionTriggersUpdateScroll()
	{
		assertTrue(PvmToolsPlugin.shouldShowUpdateScroll("1.4.8", "1.4.7", false, false));
	}

	@Test
	public void sameVersionDoesNotTriggerTwice()
	{
		assertFalse(PvmToolsPlugin.shouldShowUpdateScroll("1.4.8", "1.4.8", false, false));
	}

	@Test
	public void explicitOptOutIsRespected()
	{
		assertFalse(PvmToolsPlugin.shouldShowUpdateScroll("1.4.8", "1.4.7", true, false));
	}

	@Test
	public void previewOverridesSeenVersionAndOptOut()
	{
		assertTrue(PvmToolsPlugin.shouldShowUpdateScroll("1.4.8", "1.4.8", true, true));
	}
}
