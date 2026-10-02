package com.arber.pvmtools;

import java.util.Objects;

final class SlayerTaskPersistenceCheckpoint
{
	private boolean dirty;
	private long lastCheckpointMillis;
	private String lastPersistedValue = "";

	void reset(String persistedValue, long nowMillis)
	{
		dirty = false;
		lastCheckpointMillis = Math.max(0L, nowMillis);
		lastPersistedValue = persistedValue == null ? "" : persistedValue;
	}

	void markDirty()
	{
		dirty = true;
	}

	boolean isDirty()
	{
		return dirty;
	}

	boolean isCheckpointDue(long nowMillis, long intervalMillis)
	{
		return dirty
			&& nowMillis > lastCheckpointMillis
			&& nowMillis - lastCheckpointMillis >= Math.max(0L, intervalMillis);
	}

	boolean valueChanged(String value)
	{
		return !Objects.equals(lastPersistedValue, value == null ? "" : value);
	}

	void persisted(String value, long nowMillis)
	{
		dirty = false;
		lastCheckpointMillis = Math.max(0L, nowMillis);
		lastPersistedValue = value == null ? "" : value;
	}
}
