package com.arber.pvmtools;

public enum TrackerResetTarget
{
	LOOT("Loot"),
	SUPPLY_COST("Supply cost"),
	COMBAT_XP("Combat XP"),
	SLAYER_XP("Slayer XP"),
	ALL("All trackers");

	private final String displayName;

	TrackerResetTarget(String displayName)
	{
		this.displayName = displayName;
	}

	@Override
	public String toString()
	{
		return displayName;
	}
}
