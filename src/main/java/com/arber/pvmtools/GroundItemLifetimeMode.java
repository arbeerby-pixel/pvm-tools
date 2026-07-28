package com.arber.pvmtools;

public enum GroundItemLifetimeMode
{
	ALL_VISIBLE("All visible"),
	VALUE_THRESHOLD("Value threshold");

	private final String displayName;

	GroundItemLifetimeMode(String displayName)
	{
		this.displayName = displayName;
	}

	@Override
	public String toString()
	{
		return displayName;
	}
}

