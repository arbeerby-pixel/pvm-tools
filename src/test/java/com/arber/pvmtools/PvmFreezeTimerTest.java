package com.arber.pvmtools;

import net.runelite.api.gameval.SpotanimID;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PvmFreezeTimerTest
{
	@Test
	public void npcFreezeGraphicsMapToStandardSpellDurations()
	{
		assertEquals(8, PvmToolsPlugin.getPvmFreezeDurationTicks(SpotanimID.BIND_IMPACT));
		assertEquals(8, PvmToolsPlugin.getPvmFreezeDurationTicks(SpotanimID.ICE_RUSH_IMPACT));
		assertEquals(16, PvmToolsPlugin.getPvmFreezeDurationTicks(SpotanimID.SNARE_IMPACT));
		assertEquals(16, PvmToolsPlugin.getPvmFreezeDurationTicks(SpotanimID.ICE_BURST_IMPACT));
		assertEquals(24, PvmToolsPlugin.getPvmFreezeDurationTicks(SpotanimID.ENTANGLE_IMPACT));
		assertEquals(24, PvmToolsPlugin.getPvmFreezeDurationTicks(SpotanimID.ICE_BLITZ_IMPACT));
		assertEquals(32, PvmToolsPlugin.getPvmFreezeDurationTicks(SpotanimID.ICE_BARRAGE_IMPACT));
		assertEquals(32, PvmToolsPlugin.getPvmFreezeDurationTicks(SpotanimID.ICE_BARRAGE_IMPACT2));
		assertEquals(32, PvmToolsPlugin.getPvmFreezeDurationTicks(SpotanimID.ICE_BARRAGE_IMPACT3));
		assertEquals(0, PvmToolsPlugin.getPvmFreezeDurationTicks(-1));
	}
}
