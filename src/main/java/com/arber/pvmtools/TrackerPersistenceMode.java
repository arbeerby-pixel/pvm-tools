package com.arber.pvmtools;

public enum TrackerPersistenceMode
{
	SESSION("Session"),
	FOREVER("Forever");

	private final String displayName;

	TrackerPersistenceMode(String displayName)
	{
		this.displayName = displayName;
	}

	@Override
	public String toString()
	{
		return displayName;
	}
}
