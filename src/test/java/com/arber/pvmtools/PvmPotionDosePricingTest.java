package com.arber.pvmtools;

import net.runelite.api.ItemID;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PvmPotionDosePricingTest
{
	@Test
	public void mapsOtherCommonPotionFamiliesToFourDoseItems()
	{
		assertPotionFamily(ItemID.STRENGTH_POTION4, ItemID.STRENGTH_POTION3,
			ItemID.STRENGTH_POTION2, ItemID.STRENGTH_POTION1);
		assertPotionFamily(ItemID.SUPER_ATTACK4, ItemID.SUPER_ATTACK3,
			ItemID.SUPER_ATTACK2, ItemID.SUPER_ATTACK1);
		assertPotionFamily(ItemID.ZAMORAK_BREW4, ItemID.ZAMORAK_BREW3,
			ItemID.ZAMORAK_BREW2, ItemID.ZAMORAK_BREW1);
		assertPotionFamily(ItemID.SANFEW_SERUM4, ItemID.SANFEW_SERUM3,
			ItemID.SANFEW_SERUM2, ItemID.SANFEW_SERUM1);
		assertPotionFamily(ItemID.DIVINE_SUPER_ATTACK_POTION4, ItemID.DIVINE_SUPER_ATTACK_POTION3,
			ItemID.DIVINE_SUPER_ATTACK_POTION2, ItemID.DIVINE_SUPER_ATTACK_POTION1);
		assertPotionFamily(ItemID.ANCIENT_BREW4, ItemID.ANCIENT_BREW3,
			ItemID.ANCIENT_BREW2, ItemID.ANCIENT_BREW1);
		assertPotionFamily(ItemID.MOONLIGHT_POTION4, ItemID.MOONLIGHT_POTION3,
			ItemID.MOONLIGHT_POTION2, ItemID.MOONLIGHT_POTION1);
	}

	@Test
	public void derivesDoseCountFromPotionNameForFutureFamilies()
	{
		assertEquals(4, PvmToolsPlugin.getDoseCountFromItemName("Future potion(4)"));
		assertEquals(2, PvmToolsPlugin.getDoseCountFromItemName("Future potion (2)"));
		assertEquals(1, PvmToolsPlugin.getDoseCountFromItemName("Future potion"));
		assertEquals(1, PvmToolsPlugin.getDoseCountFromItemName(null));
	}

	@Test
	public void calculatesPerDoseValueWithoutInventingCostForUnpricedItems()
	{
		assertEquals(3_000L, PvmToolsPlugin.getPerDoseValue(12_000L, 4));
		assertEquals(5_000L, PvmToolsPlugin.getPerDoseValue(10_000L, 2));
		assertEquals(0L, PvmToolsPlugin.getPerDoseValue(0L, 4));
	}

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

	private static void assertPotionFamily(int fourDose, int threeDose, int twoDose, int oneDose)
	{
		assertEquals(fourDose, PvmToolsPlugin.getFourDosePotionId(fourDose));
		assertEquals(fourDose, PvmToolsPlugin.getFourDosePotionId(threeDose));
		assertEquals(fourDose, PvmToolsPlugin.getFourDosePotionId(twoDose));
		assertEquals(fourDose, PvmToolsPlugin.getFourDosePotionId(oneDose));
	}
}
