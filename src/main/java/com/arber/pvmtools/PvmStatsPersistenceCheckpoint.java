package com.arber.pvmtools;

import java.util.EnumMap;
import java.util.EnumSet;

final class PvmStatsPersistenceCheckpoint
{
	private final EnumSet<PvmToolsStatsPeriod> dirtyPeriods = EnumSet.noneOf(PvmToolsStatsPeriod.class);
	private final EnumMap<PvmToolsStatsPeriod, Long> lastPersistMillis = new EnumMap<>(PvmToolsStatsPeriod.class);

	void markDirty(PvmToolsStatsPeriod period)
	{
		if (period != null)
		{
			dirtyPeriods.add(period);
		}
	}

	boolean isDirty(PvmToolsStatsPeriod period)
	{
		return dirtyPeriods.contains(period);
	}

	boolean isDue(PvmToolsStatsPeriod period, long nowMillis, long intervalMillis, boolean force)
	{
		if (!isDirty(period) || force)
		{
			return isDirty(period);
		}

		long lastMillis = lastPersistMillis.getOrDefault(period, 0L);
		long interval = Math.max(0L, intervalMillis);
		return nowMillis >= lastMillis && nowMillis - lastMillis >= interval;
	}

	void persisted(PvmToolsStatsPeriod period, long nowMillis)
	{
		if (period == null)
		{
			return;
		}

		dirtyPeriods.remove(period);
		lastPersistMillis.put(period, Math.max(0L, nowMillis));
	}
}
