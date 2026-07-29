package com.arber.pvmtools;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Map;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PvmTaskPersonalBestTest
{
	@Test
	public void recordsAndAveragesAreCalculatedPerMonster()
	{
		PvmTaskHistoryEntry newest = entry(
			"Dust devils", 1_500_000L, 1_000_000L, 40_000L, 4_000L);
		PvmTaskHistoryEntry older = entry(
			"Dust devils", 1_800_000L, 900_000L, 50_000L, 3_000L);
		PvmTaskHistoryEntry oldest = entry(
			"Dust devils", 2_400_000L, 1_200_000L, 30_000L, 2_000L);
		PvmTaskHistoryEntry otherMonster = entry(
			"Abyssal demons", 3_000_000L, 2_000_000L, 60_000L, 1_000L);

		Map<String, PvmTaskPersonalBest> records = PvmTaskPersonalBest.index(
			Arrays.asList(newest, older, oldest, otherMonster));
		PvmTaskPersonalBest dustDevils = records.get("dust devils");

		assertEquals(2, records.size());
		assertEquals(3, dustDevils.getTaskCount());
		assertEquals(1_500_000L, dustDevils.getFastestMillis());
		assertEquals(1_200_000L, dustDevils.getBestProfit());
		assertEquals(50_000L, dustDevils.getMostXp());
		assertEquals(1_900_000L, dustDevils.getAverageElapsedMillis());
		assertEquals(1_033_333L, dustDevils.getAverageProfit());
		assertEquals(40_000L, dustDevils.getAverageXp());
		assertEquals(
			EnumSet.of(PvmTaskPersonalBest.Metric.TIME),
			dustDevils.getNewRecords(newest));
		assertEquals(
			EnumSet.of(PvmTaskPersonalBest.Metric.PROFIT),
			dustDevils.getCurrentRecords(oldest));
		assertEquals(
			EnumSet.of(PvmTaskPersonalBest.Metric.XP),
			dustDevils.getCurrentRecords(older));
		assertTrue(records.get("abyssal demons").isFirstRecord(otherMonster));
	}

	@Test
	public void equalResultDoesNotReplaceOrBreakPreviousRecord()
	{
		PvmTaskHistoryEntry newest = entry(
			"Nechryaels", 1_000_000L, 500_000L, 25_000L, 2_000L);
		PvmTaskHistoryEntry oldest = entry(
			"Nechryaels", 1_000_000L, 500_000L, 25_000L, 1_000L);

		PvmTaskPersonalBest records = PvmTaskPersonalBest.forTask(
			Arrays.asList(newest, oldest),
			"NECHRYAELS");

		assertTrue(records.getNewRecords(newest).isEmpty());
		assertTrue(records.getCurrentRecords(newest).isEmpty());
		assertEquals(
			EnumSet.allOf(PvmTaskPersonalBest.Metric.class),
			records.getCurrentRecords(oldest));
	}

	private PvmTaskHistoryEntry entry(
		String taskName,
		long elapsedMillis,
		long netProfit,
		long totalXp,
		long finishedMillis)
	{
		long supplyCost = 100_000L;
		PvmTaskSnapshot task = new PvmTaskSnapshot(
			taskName,
			"",
			0,
			200,
			1_000L,
			elapsedMillis,
			netProfit + supplyCost,
			supplyCost,
			totalXp,
			0L,
			0L,
			0L,
			0L);
		return new PvmTaskHistoryEntry(task, finishedMillis);
	}
}
