package com.arber.pvmtools;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class PvmLootSourceStat
{
	private final String name;
	private final int combatLevel;
	private final long kills;
	private final long firstLootMillis;
	private final long lastLootMillis;
	private final long supplyCostValue;
	private final List<PvmDropStat> drops;

	PvmLootSourceStat(
		String name,
		int combatLevel,
		long kills,
		long firstLootMillis,
		long lastLootMillis,
		long supplyCostValue,
		List<PvmDropStat> drops)
	{
		this.name = name == null ? "Unknown" : name;
		this.combatLevel = Math.max(0, combatLevel);
		this.kills = Math.max(0L, kills);
		this.firstLootMillis = Math.max(0L, firstLootMillis);
		this.lastLootMillis = Math.max(0L, lastLootMillis);
		this.supplyCostValue = Math.max(0L, supplyCostValue);
		this.drops = Collections.unmodifiableList(new ArrayList<>(drops));
	}

	String getName()
	{
		return name;
	}

	int getCombatLevel()
	{
		return combatLevel;
	}

	long getKills()
	{
		return kills;
	}

	long getFirstLootMillis()
	{
		return firstLootMillis;
	}

	long getLastLootMillis()
	{
		return lastLootMillis;
	}

	long getSupplyCostValue()
	{
		return supplyCostValue;
	}

	List<PvmDropStat> getDrops()
	{
		return drops;
	}

	long getTotalValue()
	{
		long total = 0L;
		for (PvmDropStat drop : drops)
		{
			total += drop.getValue();
		}
		return total;
	}
}
