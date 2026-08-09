package com.arber.pvmtools;

public enum ToolkitMarketPriceSource
{
	GE_GUIDE("OSRS GE guide prices"),
	RUNELITE("RuneLite market prices");

	private final String displayName;

	ToolkitMarketPriceSource(String displayName)
	{
		this.displayName = displayName;
	}

	@Override
	public String toString()
	{
		return displayName;
	}
}
