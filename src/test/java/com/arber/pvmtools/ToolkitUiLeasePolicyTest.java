package com.arber.pvmtools;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ToolkitUiLeasePolicyTest
{
	@Test
	public void activeLeasePreventsActivityOwnerFlip()
	{
		assertFalse(ToolkitUiLeasePolicy.shouldClaim(
			"PVM", "SKILLING", "ACTIVITY", true, 40_000L, 20_000L));
		assertTrue(ToolkitUiLeasePolicy.shouldClaim(
			"PVM", "SKILLING", "ACTIVITY", true, 20_000L, 20_000L));
	}

	@Test
	public void manualSelectionCannotBeStolenByActivity()
	{
		assertFalse(ToolkitUiLeasePolicy.shouldClaim(
			"PVM", "SKILLING", "MANUAL", false, 0L, 20_000L));
	}

	@Test
	public void ownerRenewsOnlyNearLeaseEnd()
	{
		assertFalse(ToolkitUiLeasePolicy.shouldRenew(
			"PVM", "PVM", "ACTIVITY", 40_001L, 20_000L, 10_000L));
		assertTrue(ToolkitUiLeasePolicy.shouldRenew(
			"PVM", "PVM", "ACTIVITY", 30_000L, 20_000L, 10_000L));
	}
}
