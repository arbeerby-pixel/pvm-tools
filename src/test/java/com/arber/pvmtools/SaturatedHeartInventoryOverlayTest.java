package com.arber.pvmtools;

import net.runelite.api.GameState;
import net.runelite.api.ItemID;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SaturatedHeartInventoryOverlayTest
{
	@Test
	public void highlightsReadySaturatedHeartWhenLoggedIn()
	{
		assertTrue(SaturatedHeartInventoryOverlay.shouldHighlight(
			ItemID.SATURATED_HEART, true, GameState.LOGGED_IN, 0, 0));
	}

	@Test
	public void suppressesHighlightWhileBoostOrCooldownRemains()
	{
		assertFalse(SaturatedHeartInventoryOverlay.shouldHighlight(
			ItemID.SATURATED_HEART, true, GameState.LOGGED_IN, 1, 0));
		assertFalse(SaturatedHeartInventoryOverlay.shouldHighlight(
			ItemID.SATURATED_HEART, true, GameState.LOGGED_IN, 0, 1));
	}

	@Test
	public void suppressesDisabledWrongItemAndLoggedOutStates()
	{
		assertFalse(SaturatedHeartInventoryOverlay.shouldHighlight(
			ItemID.SATURATED_HEART, false, GameState.LOGGED_IN, 0, 0));
		assertFalse(SaturatedHeartInventoryOverlay.shouldHighlight(
			ItemID.IMBUED_HEART, true, GameState.LOGGED_IN, 0, 0));
		assertFalse(SaturatedHeartInventoryOverlay.shouldHighlight(
			ItemID.SATURATED_HEART, true, GameState.LOGIN_SCREEN, 0, 0));
	}
}
