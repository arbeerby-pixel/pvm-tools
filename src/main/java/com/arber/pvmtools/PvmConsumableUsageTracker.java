package com.arber.pvmtools;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.IntUnaryOperator;

/**
 * Confirms food and potion usage from an inventory decrease instead of treating
 * every menu click as a successful consumption.
 */
final class PvmConsumableUsageTracker
{
	private static final int CONFIRMATION_WINDOW_TICKS = 3;

	private final Map<Integer, PendingConsumption> pendingConsumptions = new HashMap<>();

	void recordAttempt(
		int itemId,
		int inventoryQuantity,
		int tick,
		PvmToolsPlugin.SupplyCostType type)
	{
		if (itemId <= 0 || inventoryQuantity <= 0 || type == null)
		{
			return;
		}

		pendingConsumptions.put(itemId, new PendingConsumption(inventoryQuantity, tick, type));
	}

	void confirmInventoryChange(
		int tick,
		IntUnaryOperator quantityLookup,
		ConsumptionConsumer consumer)
	{
		Iterator<Map.Entry<Integer, PendingConsumption>> iterator = pendingConsumptions.entrySet().iterator();
		while (iterator.hasNext())
		{
			Map.Entry<Integer, PendingConsumption> entry = iterator.next();
			PendingConsumption pending = entry.getValue();
			if (isExpired(tick, pending.tick))
			{
				iterator.remove();
				continue;
			}

			int currentQuantity = Math.max(0, quantityLookup.applyAsInt(entry.getKey()));
			if (currentQuantity < pending.inventoryQuantity)
			{
				consumer.record(entry.getKey(), 1, pending.type);
				iterator.remove();
			}
		}
	}

	void expire(int tick)
	{
		pendingConsumptions.values().removeIf(pending -> isExpired(tick, pending.tick));
	}

	void reset()
	{
		pendingConsumptions.clear();
	}

	private boolean isExpired(int currentTick, int attemptTick)
	{
		int age = currentTick - attemptTick;
		return age < 0 || age > CONFIRMATION_WINDOW_TICKS;
	}

	@FunctionalInterface
	interface ConsumptionConsumer
	{
		void record(int itemId, int quantity, PvmToolsPlugin.SupplyCostType type);
	}

	private static final class PendingConsumption
	{
		private final int inventoryQuantity;
		private final int tick;
		private final PvmToolsPlugin.SupplyCostType type;

		private PendingConsumption(
			int inventoryQuantity,
			int tick,
			PvmToolsPlugin.SupplyCostType type)
		{
			this.inventoryQuantity = inventoryQuantity;
			this.tick = tick;
			this.type = type;
		}
	}
}
