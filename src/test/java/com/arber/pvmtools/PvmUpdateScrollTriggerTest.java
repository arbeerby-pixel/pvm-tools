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
		assertEquals("1.4.19", bundledVersion);
		assertTrue(PvmToolsPlugin.shouldShowUpdateScroll(bundledVersion, "1.4.18", false));
	}

	@Test
	public void newVersionTriggersUpdateScroll()
	{
		assertTrue(PvmToolsPlugin.shouldShowUpdateScroll("1.4.19", "1.4.18", false));
	}

	@Test
	public void sameVersionDoesNotTriggerTwice()
	{
		assertFalse(PvmToolsPlugin.shouldShowUpdateScroll("1.4.19", "1.4.19", false));
	}

	@Test
	public void explicitOptOutIsRespected()
	{
		assertFalse(PvmToolsPlugin.shouldShowUpdateScroll("1.4.19", "1.4.18", true));
	}
}
