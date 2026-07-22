package com.arber.pvmtools;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class PvmTaskHistoryEntryTest
{
	@Test
	public void taskHistorySurvivesSerialization()
	{
		PvmTaskSnapshot task = new PvmTaskSnapshot(
			"Abyssal demons",
			"Catacombs of Kourend",
			0,
			186,
			1_700_000_000_000L,
			3_723_000L,
			1_850_000L,
			425_000L,
			118_500L,
			28_000L,
			17L,
			6L,
			820L);
		PvmTaskHistoryEntry original = new PvmTaskHistoryEntry(task, 1_700_003_723_000L);

		PvmTaskHistoryEntry restored = PvmTaskHistoryEntry.deserialize(original.serialize());

		assertNotNull(restored);
		assertEquals(1_700_003_723_000L, restored.getFinishedMillis());
		assertEquals("Abyssal demons", restored.getTask().getName());
		assertEquals("Catacombs of Kourend", restored.getTask().getLocation());
		assertEquals(186, restored.getTask().getKilled());
		assertEquals(3_723_000L, restored.getTask().getElapsedMillis());
		assertEquals(1_850_000L, restored.getTask().getLootValue());
		assertEquals(425_000L, restored.getTask().getSupplyCostValue());
		assertEquals(1_425_000L, restored.getTask().getNetProfit());
		assertEquals(118_500L, restored.getTask().getCombatXp());
		assertEquals(28_000L, restored.getTask().getSlayerXp());
		assertEquals(17L, restored.getTask().getPotionDoseCount());
		assertEquals(6L, restored.getTask().getFoodCount());
		assertEquals(820L, restored.getTask().getCannonballCount());
	}

	@Test
	public void corruptHistoryEntryIsIgnored()
	{
		assertNull(PvmTaskHistoryEntry.deserialize("not-a-valid-entry"));
		assertNull(PvmTaskHistoryEntry.deserialize(""));
	}

	@Test
	public void interruptedPartsOfTheSameTaskAreMerged()
	{
		PvmTaskHistoryEntry beforePause = new PvmTaskHistoryEntry(
			new PvmTaskSnapshot(
				"Dust devils", "Catacombs", 120, 200, 1_000L, 60_000L,
				500_000L, 100_000L, 20_000L, 5_000L, 2L, 1L, 100L),
			2_000L);
		PvmTaskHistoryEntry afterPause = new PvmTaskHistoryEntry(
			new PvmTaskSnapshot(
				"Dust devils", "Catacombs", 0, 200, 3_000L, 120_000L,
				700_000L, 150_000L, 30_000L, 10_000L, 3L, 2L, 200L),
			4_000L);

		assertEquals(true, beforePause.canMergeInterruptedTask(afterPause));
		PvmTaskHistoryEntry merged = beforePause.merge(afterPause);

		assertEquals(0, merged.getTask().getAmount());
		assertEquals(200, merged.getTask().getKilled());
		assertEquals(180_000L, merged.getTask().getElapsedMillis());
		assertEquals(1_200_000L, merged.getTask().getLootValue());
		assertEquals(250_000L, merged.getTask().getSupplyCostValue());
		assertEquals(50_000L, merged.getTask().getCombatXp());
		assertEquals(15_000L, merged.getTask().getSlayerXp());
		assertEquals(4_000L, merged.getFinishedMillis());
	}

	@Test
	public void twoCompletedAssignmentsAreNotMerged()
	{
		PvmTaskHistoryEntry first = new PvmTaskHistoryEntry(
			new PvmTaskSnapshot("Dust devils", "Catacombs", 0, 200, 1_000L, 1L, 1L, 0L, 0L, 0L, 0L, 0L, 0L),
			2_000L);
		PvmTaskHistoryEntry second = new PvmTaskHistoryEntry(
			new PvmTaskSnapshot("Dust devils", "Catacombs", 0, 200, 3_000L, 1L, 1L, 0L, 0L, 0L, 0L, 0L, 0L),
			4_000L);

		assertEquals(false, first.canMergeInterruptedTask(second));
	}
}
