package com.arber.pvmtools;

final class PvmTaskSnapshot
{
	static final PvmTaskSnapshot EMPTY = new PvmTaskSnapshot("", "", 0, 0, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L);

	private final String name;
	private final String location;
	private final int amount;
	private final int initialAmount;
	private final long startedMillis;
	private final long elapsedMillis;
	private final long lootValue;
	private final long supplyCostValue;
	private final long combatXp;
	private final long slayerXp;
	private final long potionDoseCount;
	private final long foodCount;
	private final long cannonballCount;
	private final long runeCount;
	private final long ammoCount;
	private final long zulrahScaleCount;

	PvmTaskSnapshot(
		String name,
		String location,
		int amount,
		int initialAmount,
		long startedMillis,
		long elapsedMillis,
		long lootValue,
		long supplyCostValue,
		long combatXp,
		long slayerXp,
		long potionDoseCount,
		long foodCount,
		long cannonballCount)
	{
		this(name, location, amount, initialAmount, startedMillis, elapsedMillis, lootValue,
			supplyCostValue, combatXp, slayerXp, potionDoseCount, foodCount, cannonballCount,
			0L, 0L, 0L);
	}

	PvmTaskSnapshot(
		String name,
		String location,
		int amount,
		int initialAmount,
		long startedMillis,
		long elapsedMillis,
		long lootValue,
		long supplyCostValue,
		long combatXp,
		long slayerXp,
		long potionDoseCount,
		long foodCount,
		long cannonballCount,
		long runeCount,
		long ammoCount,
		long zulrahScaleCount)
	{
		this.name = name == null ? "" : name;
		this.location = location == null ? "" : location;
		this.amount = Math.max(0, amount);
		this.initialAmount = Math.max(0, initialAmount);
		this.startedMillis = Math.max(0L, startedMillis);
		this.elapsedMillis = Math.max(0L, elapsedMillis);
		this.lootValue = Math.max(0L, lootValue);
		this.supplyCostValue = Math.max(0L, supplyCostValue);
		this.combatXp = Math.max(0L, combatXp);
		this.slayerXp = Math.max(0L, slayerXp);
		this.potionDoseCount = Math.max(0L, potionDoseCount);
		this.foodCount = Math.max(0L, foodCount);
		this.cannonballCount = Math.max(0L, cannonballCount);
		this.runeCount = Math.max(0L, runeCount);
		this.ammoCount = Math.max(0L, ammoCount);
		this.zulrahScaleCount = Math.max(0L, zulrahScaleCount);
	}

	boolean isActive()
	{
		return !name.isBlank();
	}

	String getName()
	{
		return name;
	}

	String getLocation()
	{
		return location;
	}

	int getAmount()
	{
		return amount;
	}

	int getInitialAmount()
	{
		return initialAmount;
	}

	long getStartedMillis()
	{
		return startedMillis;
	}

	long getElapsedMillis()
	{
		return elapsedMillis;
	}

	long getLootValue()
	{
		return lootValue;
	}

	long getSupplyCostValue()
	{
		return supplyCostValue;
	}

	long getNetProfit()
	{
		return lootValue - supplyCostValue;
	}

	long getCombatXp()
	{
		return combatXp;
	}

	long getSlayerXp()
	{
		return slayerXp;
	}

	long getPotionDoseCount()
	{
		return potionDoseCount;
	}

	long getFoodCount()
	{
		return foodCount;
	}

	long getCannonballCount()
	{
		return cannonballCount;
	}

	long getRuneCount()
	{
		return runeCount;
	}

	long getAmmoCount()
	{
		return ammoCount;
	}

	long getZulrahScaleCount()
	{
		return zulrahScaleCount;
	}

	int getKilled()
	{
		return initialAmount > 0 ? Math.max(0, initialAmount - amount) : 0;
	}
}
