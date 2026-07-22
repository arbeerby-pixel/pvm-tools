package com.arber.pvmtools;

public enum ToolkitPriceSource
{
	GE_GUIDE("GE guide prices"),
	RUNELITE("RuneLite prices");

	private final String displayName;

	ToolkitPriceSource(String displayName)
	{
		this.displayName = displayName;
	}

	@Override
	public String toString()
	{
		return displayName;
	}
}
