package com.arber.pvmtools;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PvmStatsPersistenceCheckpointTest
{
	@Test
	public void accountSwitchClearsDirtyAndThrottleState()
	{
		PvmStatsPersistenceCheckpoint checkpoint = new PvmStatsPersistenceCheckpoint();
		checkpoint.markDirty(PvmToolsStatsPeriod.ALL_TIME);
		checkpoint.persisted(PvmToolsStatsPeriod.ALL_TIME, 10_000L);
		checkpoint.markDirty(PvmToolsStatsPeriod.DAY);
		checkpoint.reset();
		assertFalse(checkpoint.isDirty(PvmToolsStatsPeriod.DAY));
		checkpoint.markDirty(PvmToolsStatsPeriod.ALL_TIME);
		assertTrue(checkpoint.isDue(PvmToolsStatsPeriod.ALL_TIME, 2_000L, 2_000L, false));
	}

	@Test
	public void dirtyPeriodIsWrittenImmediatelyThenThrottled()
	{
		PvmStatsPersistenceCheckpoint checkpoint = new PvmStatsPersistenceCheckpoint();
		checkpoint.markDirty(PvmToolsStatsPeriod.DAY);

		assertTrue(checkpoint.isDue(PvmToolsStatsPeriod.DAY, 1_000L, 1_000L, false));
		checkpoint.persisted(PvmToolsStatsPeriod.DAY, 1_000L);
		checkpoint.markDirty(PvmToolsStatsPeriod.DAY);
		assertFalse(checkpoint.isDue(PvmToolsStatsPeriod.DAY, 1_999L, 1_000L, false));
		assertTrue(checkpoint.isDue(PvmToolsStatsPeriod.DAY, 2_000L, 1_000L, false));
	}

	@Test
	public void forceFlushWritesDirtyPeriodBeforeInterval()
	{
		PvmStatsPersistenceCheckpoint checkpoint = new PvmStatsPersistenceCheckpoint();
		checkpoint.markDirty(PvmToolsStatsPeriod.ALL_TIME);
		checkpoint.persisted(PvmToolsStatsPeriod.ALL_TIME, 10_000L);
		checkpoint.markDirty(PvmToolsStatsPeriod.ALL_TIME);

		assertFalse(checkpoint.isDue(PvmToolsStatsPeriod.ALL_TIME, 10_001L, 60_000L, false));
		assertTrue(checkpoint.isDue(PvmToolsStatsPeriod.ALL_TIME, 10_001L, 60_000L, true));
	}
}
