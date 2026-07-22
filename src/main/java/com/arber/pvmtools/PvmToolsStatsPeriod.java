package com.arber.pvmtools;

import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.Locale;

enum PvmToolsStatsPeriod
{
	DAY("Day"),
	WEEK("Week"),
	MONTH("Month"),
	YEAR("Year"),
	ALL_TIME("All-time");

	private final String displayName;

	PvmToolsStatsPeriod(String displayName)
	{
		this.displayName = displayName;
	}

	String getDisplayName()
	{
		return displayName;
	}

	String getCurrentPeriodId(LocalDate date)
	{
		switch (this)
		{
			case DAY:
				return date.toString();
			case WEEK:
				WeekFields weekFields = WeekFields.ISO;
				return date.get(weekFields.weekBasedYear()) + "-W" + date.get(weekFields.weekOfWeekBasedYear());
			case MONTH:
				return date.getYear() + "-" + date.getMonthValue();
			case YEAR:
				return Integer.toString(date.getYear());
			case ALL_TIME:
			default:
				return "all";
		}
	}

	@Override
	public String toString()
	{
		return displayName.toLowerCase(Locale.ENGLISH);
	}
}
