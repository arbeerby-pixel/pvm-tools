package com.arber.pvmtools;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class PvmTaskPersonalBest
{
	enum Metric
	{
		TIME,
		PROFIT,
		XP
	}

	private final List<PvmTaskHistoryEntry> entries;
	private final PvmTaskHistoryEntry fastestEntry;
	private final PvmTaskHistoryEntry bestProfitEntry;
	private final PvmTaskHistoryEntry mostXpEntry;
	private final long averageElapsedMillis;
	private final long averageProfit;
	private final long averageXp;

	private PvmTaskPersonalBest(List<PvmTaskHistoryEntry> entries)
	{
		this.entries = entries;

		PvmTaskHistoryEntry fastest = null;
		PvmTaskHistoryEntry bestProfit = null;
		PvmTaskHistoryEntry mostXp = null;
		long totalElapsedMillis = 0L;
		long totalProfit = 0L;
		long totalXp = 0L;

		// History is newest first. Walking backwards preserves the older record on ties.
		for (int index = entries.size() - 1; index >= 0; index--)
		{
			PvmTaskHistoryEntry entry = entries.get(index);
			PvmTaskSnapshot task = entry.getTask();
			long taskXp = totalXp(task);

			if (task.getElapsedMillis() > 0L
				&& (fastest == null || task.getElapsedMillis() < fastest.getTask().getElapsedMillis()))
			{
				fastest = entry;
			}
			if (bestProfit == null || task.getNetProfit() > bestProfit.getTask().getNetProfit())
			{
				bestProfit = entry;
			}
			if (mostXp == null || taskXp > totalXp(mostXp.getTask()))
			{
				mostXp = entry;
			}

			totalElapsedMillis += task.getElapsedMillis();
			totalProfit += task.getNetProfit();
			totalXp += taskXp;
		}

		fastestEntry = fastest;
		bestProfitEntry = bestProfit;
		mostXpEntry = mostXp;
		int count = entries.size();
		averageElapsedMillis = count == 0 ? 0L : Math.round(totalElapsedMillis / (double) count);
		averageProfit = count == 0 ? 0L : Math.round(totalProfit / (double) count);
		averageXp = count == 0 ? 0L : Math.round(totalXp / (double) count);
	}

	static Map<String, PvmTaskPersonalBest> index(List<PvmTaskHistoryEntry> history)
	{
		Map<String, List<PvmTaskHistoryEntry>> groupedEntries = new LinkedHashMap<>();
		for (PvmTaskHistoryEntry entry : history)
		{
			groupedEntries
				.computeIfAbsent(taskKey(entry.getTask().getName()), ignored -> new ArrayList<>())
				.add(entry);
		}

		Map<String, PvmTaskPersonalBest> records = new LinkedHashMap<>();
		for (Map.Entry<String, List<PvmTaskHistoryEntry>> groupedEntry : groupedEntries.entrySet())
		{
			records.put(groupedEntry.getKey(), new PvmTaskPersonalBest(groupedEntry.getValue()));
		}
		return records;
	}

	static PvmTaskPersonalBest forTask(List<PvmTaskHistoryEntry> history, String taskName)
	{
		String requestedTask = taskKey(taskName);
		List<PvmTaskHistoryEntry> matchingEntries = new ArrayList<>();
		for (PvmTaskHistoryEntry entry : history)
		{
			if (taskKey(entry.getTask().getName()).equals(requestedTask))
			{
				matchingEntries.add(entry);
			}
		}
		return matchingEntries.isEmpty() ? null : new PvmTaskPersonalBest(matchingEntries);
	}

	int getTaskCount()
	{
		return entries.size();
	}

	long getFastestMillis()
	{
		return fastestEntry == null ? 0L : fastestEntry.getTask().getElapsedMillis();
	}

	long getBestProfit()
	{
		return bestProfitEntry == null ? 0L : bestProfitEntry.getTask().getNetProfit();
	}

	long getMostXp()
	{
		return mostXpEntry == null ? 0L : totalXp(mostXpEntry.getTask());
	}

	long getAverageElapsedMillis()
	{
		return averageElapsedMillis;
	}

	long getAverageProfit()
	{
		return averageProfit;
	}

	long getAverageXp()
	{
		return averageXp;
	}

	boolean isFirstRecord(PvmTaskHistoryEntry entry)
	{
		return entries.size() == 1 && entries.get(0) == entry;
	}

	EnumSet<Metric> getCurrentRecords(PvmTaskHistoryEntry entry)
	{
		EnumSet<Metric> records = EnumSet.noneOf(Metric.class);
		if (entry == fastestEntry)
		{
			records.add(Metric.TIME);
		}
		if (entry == bestProfitEntry)
		{
			records.add(Metric.PROFIT);
		}
		if (entry == mostXpEntry)
		{
			records.add(Metric.XP);
		}
		return records;
	}

	EnumSet<Metric> getNewRecords(PvmTaskHistoryEntry entry)
	{
		EnumSet<Metric> records = EnumSet.noneOf(Metric.class);
		if (entries.size() <= 1 || entries.get(0) != entry)
		{
			return records;
		}

		PvmTaskPersonalBest previousRecords =
			new PvmTaskPersonalBest(new ArrayList<>(entries.subList(1, entries.size())));
		PvmTaskSnapshot task = entry.getTask();
		if (task.getElapsedMillis() > 0L
			&& (previousRecords.getFastestMillis() == 0L
				|| task.getElapsedMillis() < previousRecords.getFastestMillis()))
		{
			records.add(Metric.TIME);
		}
		if (task.getNetProfit() > previousRecords.getBestProfit())
		{
			records.add(Metric.PROFIT);
		}
		if (totalXp(task) > previousRecords.getMostXp())
		{
			records.add(Metric.XP);
		}
		return records;
	}

	private static String taskKey(String taskName)
	{
		return taskName == null ? "" : taskName.trim().toLowerCase(Locale.ENGLISH);
	}

	private static long totalXp(PvmTaskSnapshot task)
	{
		return task.getCombatXp() + task.getSlayerXp();
	}
}
