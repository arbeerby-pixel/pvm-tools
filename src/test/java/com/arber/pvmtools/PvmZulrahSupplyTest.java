package com.arber.pvmtools;

import net.runelite.api.ItemID;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class PvmZulrahSupplyTest
{
	@Test
	public void zulAndraTeleportBreakIsTheOnlyTeleportSupplyTrigger()
	{
		assertTrue(PvmToolsPlugin.isZulAndraTeleportConsumption(ItemID.ZULANDRA_TELEPORT, "break"));
		assertTrue(PvmToolsPlugin.isZulAndraTeleportConsumption(ItemID.ZULANDRA_TELEPORT, "<col=ffffff>Break</col>"));
		assertFalse(PvmToolsPlugin.isZulAndraTeleportConsumption(ItemID.ZULANDRA_TELEPORT, "teleport"));
		assertFalse(PvmToolsPlugin.isZulAndraTeleportConsumption(ItemID.TELEPORT_CRYSTAL, "break"));
		assertFalse(PvmToolsPlugin.isZulAndraTeleportConsumption(ItemID.ZULANDRA_TELEPORT, "drop"));
	}

	@Test
	public void confirmedTeleportQuantityIsRecordedOnceAsRuneSupply()
	{
		PvmConsumableUsageTracker tracker = new PvmConsumableUsageTracker();
		PvmConsumableUsageTracker.ConsumptionConsumer consumer = mock(PvmConsumableUsageTracker.ConsumptionConsumer.class);
		tracker.recordAttempt(ItemID.ZULANDRA_TELEPORT, 1, 100, PvmToolsPlugin.SupplyCostType.RUNE);
		tracker.confirmInventoryChange(101, itemId -> 0, consumer);

		verify(consumer).record(ItemID.ZULANDRA_TELEPORT, 1, PvmToolsPlugin.SupplyCostType.RUNE);
		verifyNoMoreInteractions(consumer);
	}
}
