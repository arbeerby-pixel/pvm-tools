package com.arber.pvmtools;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PvmDirectCurrencyLootTest
{
	@Test
	public void tracksOnlyCurrencyConfirmedByServerLoot()
	{
		assertEquals(2_800, PvmToolsPlugin.confirmedDirectCurrencyLootGain(2_800, 2_800, false));
	}

	@Test
	public void ignoresGroundItemPickupToPreventDoubleCounting()
	{
		assertEquals(0, PvmToolsPlugin.confirmedDirectCurrencyLootGain(2_800, 2_800, true));
	}

	@Test
	public void ignoresUnrelatedInventoryGain()
	{
		assertEquals(0, PvmToolsPlugin.confirmedDirectCurrencyLootGain(2_800, 0, false));
	}

	@Test
	public void excludesExtraCoinsFromOtherSourcesInTheSameInventoryChange()
	{
		assertEquals(2_800, PvmToolsPlugin.confirmedDirectCurrencyLootGain(72_800, 2_800, false));
	}
}
