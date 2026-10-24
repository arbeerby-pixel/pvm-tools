package com.arber.pvmtools;

/** The display selected independently for one chat tab. */
public enum ChatTabDisplay
{
	AUTOMATIC("Automatic"),
	VANILLA("Vanilla (normal tab)"),
	LOOT("Loot"),
	SUPPLY_COST("Supply cost"),
	DEATH_FEES("Death fees"),
	NET_PROFIT("Net profit"),
	COMBAT_XP("Combat XP"),
	SLAYER_XP("Slayer XP"),
	TOP_XP_SKILL("Top skill XP"),
	ATTACK_XP("Attack XP"),
	STRENGTH_XP("Strength XP"),
	DEFENCE_XP("Defence XP"),
	RANGED_XP("Ranged XP"),
	MAGIC_XP("Magic XP"),
	HITPOINTS_XP("Hitpoints XP"),
	CLOCK("Clock");

	private final String displayName;

	ChatTabDisplay(String displayName)
	{
		this.displayName = displayName;
	}

	@Override
	public String toString()
	{
		return displayName;
	}
}
