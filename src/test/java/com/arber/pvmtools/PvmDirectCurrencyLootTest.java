package com.arber.pvmtools;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PvmDirectCurrencyLootTest
{
	@Test
	public void tracksCurrencyAddedImmediatelyAfterNpcDeath()
	{
		assertEquals(2_800, PvmToolsPlugin.directCurrencyLootGain(1_000, 3_800, true, false));
	}

	@Test
	public void ignoresGroundItemPickupToPreventDoubleCounting()
	{
		assertEquals(0, PvmToolsPlugin.directCurrencyLootGain(1_000, 3_800, true, true));
	}

	@Test
	public void ignoresUnrelatedInventoryGain()
	{
		assertEquals(0, PvmToolsPlugin.directCurrencyLootGain(1_000, 3_800, false, false));
	}
}
