package com.arber.pvmtools;

import net.runelite.api.SoundEffectID;

public enum ResourceWarningSound
{
	OFF("Off", -1),
	BELL("Bell", SoundEffectID.TOWN_CRIER_BELL_DING),
	CHIME("Chime", SoundEffectID.GE_ADD_OFFER_DINGALING),
	ANVIL("Anvil", SoundEffectID.SMITH_ANVIL_TINK);

	private final String displayName;
	private final int soundEffectId;

	ResourceWarningSound(String displayName, int soundEffectId)
	{
		this.displayName = displayName;
		this.soundEffectId = soundEffectId;
	}

	int getSoundEffectId()
	{
		return soundEffectId;
	}

	@Override
	public String toString()
	{
		return displayName;
	}
}
