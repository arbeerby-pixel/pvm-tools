package com.arber.pvmtools;

final class PvmDropStat
{
	private final int itemId;
	private final long quantity;
	private final long value;
	private final long pickupCount;

	PvmDropStat(int itemId, long quantity, long value, long pickupCount)
	{
		this.itemId = itemId;
		this.quantity = Math.max(0L, quantity);
		this.value = Math.max(0L, value);
		this.pickupCount = Math.max(0L, pickupCount);
	}

	int getItemId()
	{
		return itemId;
	}

	long getQuantity()
	{
		return quantity;
	}

	long getValue()
	{
		return value;
	}

	long getPickupCount()
	{
		return pickupCount;
	}
}
