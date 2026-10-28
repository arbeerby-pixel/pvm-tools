package com.arber.pvmtools;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PvmResourceWarningTest
{
	@Test
	public void hitpointsThresholdUsesBoostedToRealPercentage()
	{
		assertTrue(PvmToolsPlugin.isHitpointsThresholdReached(25, 100, 25));
		assertTrue(PvmToolsPlugin.isHitpointsThresholdReached(20, 99, 25));
		assertFalse(PvmToolsPlugin.isHitpointsThresholdReached(26, 100, 25));
		assertFalse(PvmToolsPlugin.isHitpointsThresholdReached(0, 100, 25));
		assertFalse(PvmToolsPlugin.isHitpointsThresholdReached(25, 100, 0));
	}

	@Test
	public void prayerThresholdUsesInclusivePointBoundary()
	{
		assertTrue(PvmToolsPlugin.isPrayerThresholdReached(10, 10));
		assertTrue(PvmToolsPlugin.isPrayerThresholdReached(0, 10));
		assertFalse(PvmToolsPlugin.isPrayerThresholdReached(11, 10));
		assertFalse(PvmToolsPlugin.isPrayerThresholdReached(10, 0));
	}
}
