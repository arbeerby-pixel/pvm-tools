package com.arber.pvmtools;

import net.runelite.api.ItemID;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SaturatedHeartInventoryOverlayTest
{
	@Test
	public void highlightsOnlySaturatedHeartWhenEnabled()
	{
		assertTrue(SaturatedHeartInventoryOverlay.shouldHighlight(ItemID.SATURATED_HEART, true));
		assertFalse(SaturatedHeartInventoryOverlay.shouldHighlight(ItemID.SATURATED_HEART, false));
		assertFalse(SaturatedHeartInventoryOverlay.shouldHighlight(ItemID.IMBUED_HEART, true));
	}
}
