package com.arber.pvmtools;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SlayerTaskPersistenceCheckpointTest
{
	@Test
	public void dirtyStateIsCheckpointedAtFortyFiveSeconds()
	{
		SlayerTaskPersistenceCheckpoint checkpoint = new SlayerTaskPersistenceCheckpoint();
		checkpoint.reset("saved", 1_000L);
		checkpoint.markDirty();

		assertFalse(checkpoint.isCheckpointDue(45_999L, 45_000L));
		assertTrue(checkpoint.isCheckpointDue(46_000L, 45_000L));
		assertTrue(checkpoint.valueChanged("updated"));
		checkpoint.persisted("updated", 46_000L);
		assertFalse(checkpoint.isDirty());
	}

	@Test
	public void identicalSerializedStateDoesNotNeedAnotherWrite()
	{
		SlayerTaskPersistenceCheckpoint checkpoint = new SlayerTaskPersistenceCheckpoint();
		checkpoint.reset("same", 0L);

		assertFalse(checkpoint.valueChanged("same"));
		assertTrue(checkpoint.valueChanged("different"));
	}
}
