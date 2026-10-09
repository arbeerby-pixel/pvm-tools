package com.arber.pvmtools;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PvmPanelRefreshCheckpointTest
{
	@Test
	public void throttledLastUpdateRemainsPendingUntilNextTick()
	{
		PvmPanelRefreshCheckpoint checkpoint = new PvmPanelRefreshCheckpoint();
		checkpoint.request();
		assertTrue(checkpoint.takeIfDue(1_000L, 500L, false));
		checkpoint.request();
		assertFalse(checkpoint.takeIfDue(1_100L, 500L, false));
		assertFalse(checkpoint.takeIfDue(1_499L, 500L, false));
		// No further activity/request is needed to deliver the last change.
		assertTrue(checkpoint.takeIfDue(1_600L, 500L, false));
		assertFalse(checkpoint.takeIfDue(2_200L, 500L, false));
	}

	@Test
	public void userResetCanForceRefreshWithinThrottleWindow()
	{
		PvmPanelRefreshCheckpoint checkpoint = new PvmPanelRefreshCheckpoint();
		checkpoint.request();
		assertTrue(checkpoint.takeIfDue(1_000L, 500L, false));
		checkpoint.request();
		assertTrue(checkpoint.takeIfDue(1_001L, 500L, true));
		assertFalse(checkpoint.takeIfDue(1_002L, 500L, true));
	}

	@Test
	public void clockAdjustmentDoesNotFreezePanelRefresh()
	{
		PvmPanelRefreshCheckpoint checkpoint = new PvmPanelRefreshCheckpoint();
		checkpoint.request();
		assertTrue(checkpoint.takeIfDue(10_000L, 500L, false));
		checkpoint.request();
		assertTrue(checkpoint.takeIfDue(1_000L, 500L, false));
	}
}
