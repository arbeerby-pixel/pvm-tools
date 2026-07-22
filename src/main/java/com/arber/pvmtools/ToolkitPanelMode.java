package com.arber.pvmtools;

public enum ToolkitPanelMode
{
	SIMPLE("Simple"),
	ADVANCED("Advanced");

	private final String displayName;

	ToolkitPanelMode(String displayName)
	{
		this.displayName = displayName;
	}

	@Override
	public String toString()
	{
		return displayName;
	}
}
