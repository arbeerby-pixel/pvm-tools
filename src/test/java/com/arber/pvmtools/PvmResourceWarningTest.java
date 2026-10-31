package com.arber.pvmtools;

import java.util.HashSet;
import java.util.Set;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PvmResourceWarningTest
{
	@Test
	public void hitpointsThresholdUsesBoostedToRealPercentage()
	{
		assertTrue(PvmToolsPlugin.isHitpointsThresholdReached(25, 100, 25));
		assertTrue(PvmToolsPlugin.isHitpointsThresholdReached(20, 99, 25));
		assertFalse(PvmToolsPlugin.isHitpointsThresholdReached(26, 100, 25));
		assertFalse(PvmToolsPlugin.isHitpointsThresholdReached(0, 100, 25));
		assertFalse(PvmToolsPlugin.isHitpointsThresholdReached(25, 100, 0));
	}

	@Test
	public void prayerThresholdUsesInclusivePointBoundary()
	{
		assertTrue(PvmToolsPlugin.isPrayerThresholdReached(10, 10));
		assertTrue(PvmToolsPlugin.isPrayerThresholdReached(0, 10));
		assertFalse(PvmToolsPlugin.isPrayerThresholdReached(11, 10));
		assertFalse(PvmToolsPlugin.isPrayerThresholdReached(10, 0));
	}

	@Test
	public void resourceAudioCueDoesNotDependOnScreenFlashSetting()
	{
		assertTrue(PvmToolsPlugin.shouldPlayWarningAudio(true, false, true));
		assertFalse(PvmToolsPlugin.shouldPlayWarningAudio(true, true, true));
		assertFalse(PvmToolsPlugin.shouldPlayWarningAudio(true, false, false));
		assertFalse(PvmToolsPlugin.shouldPlayWarningAudio(false, false, true));
	}

	@Test
	public void everyWarningSoundIsAUniqueGameSound()
	{
		Set<Integer> soundEffects = new HashSet<>();
		for (ResourceWarningSound sound : ResourceWarningSound.values())
		{
			if (sound != ResourceWarningSound.OFF)
			{
				assertTrue(sound.getSoundEffectId() > 0);
				assertTrue(soundEffects.add(sound.getSoundEffectId()));
			}
		}
		assertTrue(soundEffects.size() >= 2);
	}

	@Test
	public void prayerExpiryUsesItsOwnAudioSetting()
	{
		assertTrue(PvmToolsPlugin.shouldPlayPrayerExpirySound(true, false));
		assertFalse(PvmToolsPlugin.shouldPlayPrayerExpirySound(false, false));
		assertFalse(PvmToolsPlugin.shouldPlayPrayerExpirySound(true, true));
	}
}
