package com.arber.pvmtools;

import net.runelite.api.ItemID;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PvmPotionDosePricingTest
{
	@Test
	public void mapsEveryPrayerRegenerationDoseToTheFourDosePotion()
	{
		assertEquals(ItemID.PRAYER_REGENERATION_POTION4,
			PvmToolsPlugin.getFourDosePotionId(ItemID.PRAYER_REGENERATION_POTION4));
		assertEquals(ItemID.PRAYER_REGENERATION_POTION4,
			PvmToolsPlugin.getFourDosePotionId(ItemID.PRAYER_REGENERATION_POTION3));
		assertEquals(ItemID.PRAYER_REGENERATION_POTION4,
			PvmToolsPlugin.getFourDosePotionId(ItemID.PRAYER_REGENERATION_POTION2));
		assertEquals(ItemID.PRAYER_REGENERATION_POTION4,
			PvmToolsPlugin.getFourDosePotionId(ItemID.PRAYER_REGENERATION_POTION1));
	}

	@Test
	public void mapsEveryGoadingDoseToTheFourDosePotion()
	{
		assertEquals(ItemID.GOADING_POTION4, PvmToolsPlugin.getFourDosePotionId(ItemID.GOADING_POTION4));
		assertEquals(ItemID.GOADING_POTION4, PvmToolsPlugin.getFourDosePotionId(ItemID.GOADING_POTION3));
		assertEquals(ItemID.GOADING_POTION4, PvmToolsPlugin.getFourDosePotionId(ItemID.GOADING_POTION2));
		assertEquals(ItemID.GOADING_POTION4, PvmToolsPlugin.getFourDosePotionId(ItemID.GOADING_POTION1));
	}
}
