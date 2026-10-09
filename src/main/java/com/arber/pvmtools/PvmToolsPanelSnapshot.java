package com.arber.pvmtools;

import java.util.List;
import java.util.Set;

/** A detached view captured on the client thread for the Swing panel. */
final class PvmToolsPanelSnapshot
{
	private final PvmToolsStatsPeriod period;
	private final PvmToolsStats stats;
	private final PvmTaskSnapshot currentTask;
	private final List<PvmTaskHistoryEntry> taskHistory;
	private final String cannonEstimate;
	private final Set<String> excludedLootItems;

	PvmToolsPanelSnapshot(
		PvmToolsStatsPeriod period,
		PvmToolsStats stats,
		PvmTaskSnapshot currentTask,
		List<PvmTaskHistoryEntry> taskHistory,
		String cannonEstimate,
		Set<String> excludedLootItems)
	{
		this.period = period;
		this.stats = stats;
		this.currentTask = currentTask;
		this.taskHistory = List.copyOf(taskHistory);
		this.cannonEstimate = cannonEstimate;
		this.excludedLootItems = Set.copyOf(excludedLootItems);
	}

	static PvmToolsPanelSnapshot empty(PvmToolsStatsPeriod period)
	{
		return new PvmToolsPanelSnapshot(period, new PvmToolsStats(""), PvmTaskSnapshot.EMPTY,
			List.of(), "No cannon", Set.of());
	}

	PvmToolsStatsPeriod getPeriod()
	{
		return period;
	}

	PvmToolsStats getStats()
	{
		return stats;
	}

	PvmTaskSnapshot getCurrentTask()
	{
		return currentTask;
	}

	List<PvmTaskHistoryEntry> getTaskHistory()
	{
		return taskHistory;
	}

	String getCannonEstimate()
	{
		return cannonEstimate;
	}

	boolean isLootExcluded(String sourceName, int itemId)
	{
		return excludedLootItems.contains(PvmToolsPlugin.combatLootExclusionKey(sourceName, itemId));
	}

	long getCountedLootValue(PvmLootSourceStat source)
	{
		long value = 0L;
		for (PvmDropStat drop : source.getDrops())
		{
			if (!isLootExcluded(source.getName(), drop.getItemId()))
			{
				value += drop.getValue();
			}
		}
		return value;
	}
}
