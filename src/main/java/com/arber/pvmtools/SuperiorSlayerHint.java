package com.arber.pvmtools;

class SuperiorSlayerHint
{
	private final String prayer;
	private final String tip;

	SuperiorSlayerHint(String prayer, String tip)
	{
		this.prayer = prayer;
		this.tip = tip;
	}

	String getPrayer()
	{
		return prayer;
	}

	String getTip()
	{
		return tip;
	}
}
