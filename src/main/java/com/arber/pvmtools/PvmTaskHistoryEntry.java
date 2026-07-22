package com.arber.pvmtools;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

final class PvmTaskHistoryEntry
{
	private static final String FIELD_SEPARATOR = "|";
	private static final String FIELD_SEPARATOR_PATTERN = "\\|";
	private static final int SERIALIZED_FIELD_COUNT = 14;

	private final PvmTaskSnapshot task;
	private final long finishedMillis;

	PvmTaskHistoryEntry(PvmTaskSnapshot task, long finishedMillis)
	{
		this.task = task == null ? PvmTaskSnapshot.EMPTY : task;
		this.finishedMillis = Math.max(0L, finishedMillis);
	}

	PvmTaskSnapshot getTask()
	{
		return task;
	}

	long getFinishedMillis()
	{
		return finishedMillis;
	}

	boolean canMergeInterruptedTask(PvmTaskHistoryEntry other)
	{
		if (other == null
			|| !task.getName().equalsIgnoreCase(other.task.getName())
			|| !locationsMatch(task.getLocation(), other.task.getLocation()))
		{
			return false;
		}

		int thisInitial = task.getInitialAmount();
		int otherInitial = other.task.getInitialAmount();
		if (thisInitial > 0 && otherInitial > 0 && thisInitial != otherInitial)
		{
			return false;
		}

		return task.getAmount() > 5 || other.task.getAmount() > 5;
	}

	PvmTaskHistoryEntry merge(PvmTaskHistoryEntry other)
	{
		if (other == null)
		{
			return this;
		}

		PvmTaskSnapshot otherTask = other.task;
		String location = task.getLocation().isBlank() ? otherTask.getLocation() : task.getLocation();
		PvmTaskSnapshot mergedTask = new PvmTaskSnapshot(
			task.getName(),
			location,
			Math.min(task.getAmount(), otherTask.getAmount()),
			Math.max(task.getInitialAmount(), otherTask.getInitialAmount()),
			minPositive(task.getStartedMillis(), otherTask.getStartedMillis()),
			task.getElapsedMillis() + otherTask.getElapsedMillis(),
			task.getLootValue() + otherTask.getLootValue(),
			task.getSupplyCostValue() + otherTask.getSupplyCostValue(),
			task.getCombatXp() + otherTask.getCombatXp(),
			task.getSlayerXp() + otherTask.getSlayerXp(),
			task.getPotionDoseCount() + otherTask.getPotionDoseCount(),
			task.getFoodCount() + otherTask.getFoodCount(),
			task.getCannonballCount() + otherTask.getCannonballCount());
		return new PvmTaskHistoryEntry(mergedTask, Math.max(finishedMillis, other.finishedMillis));
	}

	String serialize()
	{
		return encode(task.getName())
			+ FIELD_SEPARATOR + encode(task.getLocation())
			+ FIELD_SEPARATOR + task.getAmount()
			+ FIELD_SEPARATOR + task.getInitialAmount()
			+ FIELD_SEPARATOR + task.getStartedMillis()
			+ FIELD_SEPARATOR + task.getElapsedMillis()
			+ FIELD_SEPARATOR + task.getLootValue()
			+ FIELD_SEPARATOR + task.getSupplyCostValue()
			+ FIELD_SEPARATOR + task.getCombatXp()
			+ FIELD_SEPARATOR + task.getSlayerXp()
			+ FIELD_SEPARATOR + task.getPotionDoseCount()
			+ FIELD_SEPARATOR + task.getFoodCount()
			+ FIELD_SEPARATOR + task.getCannonballCount()
			+ FIELD_SEPARATOR + finishedMillis;
	}

	static PvmTaskHistoryEntry deserialize(String serialized)
	{
		if (serialized == null || serialized.isBlank())
		{
			return null;
		}

		String[] fields = serialized.split(FIELD_SEPARATOR_PATTERN, -1);
		if (fields.length != SERIALIZED_FIELD_COUNT)
		{
			return null;
		}

		try
		{
			PvmTaskSnapshot task = new PvmTaskSnapshot(
				decode(fields[0]),
				decode(fields[1]),
				Integer.parseInt(fields[2]),
				Integer.parseInt(fields[3]),
				Long.parseLong(fields[4]),
				Long.parseLong(fields[5]),
				Long.parseLong(fields[6]),
				Long.parseLong(fields[7]),
				Long.parseLong(fields[8]),
				Long.parseLong(fields[9]),
				Long.parseLong(fields[10]),
				Long.parseLong(fields[11]),
				Long.parseLong(fields[12]));
			return task.isActive() ? new PvmTaskHistoryEntry(task, Long.parseLong(fields[13])) : null;
		}
		catch (IllegalArgumentException ex)
		{
			return null;
		}
	}

	private static String encode(String value)
	{
		return Base64.getUrlEncoder().withoutPadding().encodeToString(
			(value == null ? "" : value).getBytes(StandardCharsets.UTF_8));
	}

	private static String decode(String value)
	{
		return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
	}

	private static boolean locationsMatch(String first, String second)
	{
		return first.isBlank() || second.isBlank() || first.equalsIgnoreCase(second);
	}

	private static long minPositive(long first, long second)
	{
		if (first <= 0L)
		{
			return Math.max(0L, second);
		}
		if (second <= 0L)
		{
			return first;
		}
		return Math.min(first, second);
	}
}
