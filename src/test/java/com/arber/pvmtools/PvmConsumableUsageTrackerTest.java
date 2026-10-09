package com.arber.pvmtools;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PvmConsumableUsageTrackerTest
{
	private static final int SHARK = 385;

	@Test
	public void spamClicksWithoutConsumptionDoNotAddCost()
	{
		PvmConsumableUsageTracker tracker = new PvmConsumableUsageTracker();
		AtomicInteger recorded = new AtomicInteger();
		Map<Integer, Integer> inventory = inventoryWith(10);

		for (int click = 0; click < 20; click++)
		{
			tracker.recordAttempt(SHARK, 10, 100, PvmToolsPlugin.SupplyCostType.FOOD);
		}
		tracker.confirmInventoryChange(100, itemId -> inventory.getOrDefault(itemId, 0),
			(itemId, quantity, type) -> recorded.addAndGet(quantity));

		assertEquals(0, recorded.get());
	}

	@Test
	public void manyClicksAndOneInventoryDecreaseRecordOneConsumption()
	{
		PvmConsumableUsageTracker tracker = new PvmConsumableUsageTracker();
		AtomicInteger recorded = new AtomicInteger();

		for (int click = 0; click < 20; click++)
		{
			tracker.recordAttempt(SHARK, 10, 100, PvmToolsPlugin.SupplyCostType.FOOD);
		}
		Map<Integer, Integer> inventory = inventoryWith(9);
		tracker.confirmInventoryChange(101, itemId -> inventory.getOrDefault(itemId, 0),
			(itemId, quantity, type) -> recorded.addAndGet(quantity));

		assertEquals(1, recorded.get());
	}

	@Test
	public void unrelatedInventoryChangesDoNotConfirmConsumption()
	{
		PvmConsumableUsageTracker tracker = new PvmConsumableUsageTracker();
		AtomicInteger recorded = new AtomicInteger();
		tracker.recordAttempt(SHARK, 10, 100, PvmToolsPlugin.SupplyCostType.FOOD);

		Map<Integer, Integer> inventory = inventoryWith(10);
		inventory.put(995, 1_000);
		tracker.confirmInventoryChange(101, itemId -> inventory.getOrDefault(itemId, 0),
			(itemId, quantity, type) -> recorded.addAndGet(quantity));

		assertEquals(0, recorded.get());
	}

	@Test
	public void expiredAttemptsCannotConfirmLaterInventoryRemoval()
	{
		PvmConsumableUsageTracker tracker = new PvmConsumableUsageTracker();
		AtomicInteger recorded = new AtomicInteger();
		tracker.recordAttempt(SHARK, 10, 100, PvmToolsPlugin.SupplyCostType.FOOD);
		tracker.expire(104);

		Map<Integer, Integer> inventory = inventoryWith(9);
		tracker.confirmInventoryChange(104, itemId -> inventory.getOrDefault(itemId, 0),
			(itemId, quantity, type) -> recorded.addAndGet(quantity));

		assertEquals(0, recorded.get());
	}

	private Map<Integer, Integer> inventoryWith(int sharks)
	{
		Map<Integer, Integer> inventory = new HashMap<>();
		inventory.put(SHARK, sharks);
		return inventory;
	}
}
