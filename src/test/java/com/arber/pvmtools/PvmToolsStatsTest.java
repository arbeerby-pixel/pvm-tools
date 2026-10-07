package com.arber.pvmtools;

import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class PvmToolsStatsTest
{
	@Test
	public void dayPeriodUsesCurrentDate()
	{
		LocalDate date = LocalDate.of(2026, 6, 22);
		assertEquals("2026-06-22", PvmToolsStatsPeriod.DAY.getCurrentPeriodId(date));
	}

	@Test
	public void dropHighlightsSurviveSerialization()
	{
		PvmToolsStats stats = new PvmToolsStats("2026-06-22");
		stats.addLoot(100, 2, 200);
		stats.addLoot(200, 10, 100);
		stats.addLoot(100, 1, 900);

		PvmToolsStats restored = PvmToolsStats.deserialize(stats.serialize(), "2026-06-22");
		PvmDropStat mostCommon = restored.getMostCommonDrop();
		PvmDropStat mostValuable = restored.getMostValuableDrop();
		PvmDropStat bestPickup = restored.getBestPickup();

		assertNotNull(mostCommon);
		assertNotNull(mostValuable);
		assertNotNull(bestPickup);
		assertEquals(100, mostCommon.getItemId());
		assertEquals(2L, mostCommon.getPickupCount());
		assertEquals(100, mostValuable.getItemId());
		assertEquals(1_100L, mostValuable.getValue());
		assertEquals(100, bestPickup.getItemId());
		assertEquals(1L, bestPickup.getQuantity());
		assertEquals(900L, bestPickup.getValue());
		assertEquals(2, restored.getUniqueDropCount());
		assertEquals(1_200L, restored.getLootValue());
	}

	@Test
	public void trackerCategoryResetsDoNotClearOtherCategories()
	{
		PvmToolsStats stats = new PvmToolsStats("all");
		stats.addLoot(100, 1, 500);
		stats.addSupplyCost(200, 1, PvmToolsPlugin.SupplyCostType.FOOD);
		stats.addCombatXp(Skill.ATTACK, 300);
		stats.addSlayerXp(400);

		stats.resetLoot();
		assertEquals(0L, stats.getLootValue());
		assertEquals(200L, stats.getSupplyCostValue());
		assertEquals(300L, stats.getCombatXp());
		assertEquals(400L, stats.getSlayerXp());

		stats.resetSupplyCost();
		stats.resetCombatXp();
		stats.resetSlayerXp();
		assertEquals(0L, stats.getSupplyCostValue());
		assertEquals(0L, stats.getCombatXp());
		assertEquals(0L, stats.getSlayerXp());
	}

	@Test
	public void ignoredSupplyCostStillTracksUsageCount()
	{
		PvmToolsStats stats = new PvmToolsStats("all");
		stats.addSupplyCost(0L, 3L, PvmToolsPlugin.SupplyCostType.POTION);

		assertEquals(0L, stats.getSupplyCostValue());
		assertEquals(0L, stats.getPotionSupplyCostValue());
		assertEquals(3L, stats.getPotionDoseCount());
	}

	@Test
	public void trackedLootIsSortedByTotalValue()
	{
		PvmToolsStats stats = new PvmToolsStats("all");
		stats.addLoot(100, 2, 500);
		stats.addLoot(200, 1, 1_500);
		stats.addLoot(100, 1, 900);

		List<PvmDropStat> drops = stats.getTrackedDrops();
		assertEquals(2, drops.size());
		assertEquals(200, drops.get(0).getItemId());
		assertEquals(1_500L, drops.get(0).getValue());
		assertEquals(100, drops.get(1).getItemId());
		assertEquals(3L, drops.get(1).getQuantity());
		assertEquals(2L, drops.get(1).getPickupCount());
	}

	@Test
	public void combatLootGroupsKillsByMonsterAndSurvivesSerialization()
	{
		PvmToolsStats stats = new PvmToolsStats("all");
		stats.addCombatLoot("Gargoyle", 111, List.of(
			new PvmDropStat(995, 2_000, 2_000, 1),
			new PvmDropStat(100, 1, 10_000, 1)), 1_000L);
		stats.addCombatLoot("Gargoyle", 111, List.of(
			new PvmDropStat(995, 3_000, 3_000, 1)), 2_000L);
		stats.addCombatSupplyCost("Gargoyle", 111, 4_500L);
		stats.addCombatLoot("Aquanite", 114, List.of(
			new PvmDropStat(200, 2, 30_000, 1)), 3_000L);

		PvmToolsStats restored = PvmToolsStats.deserialize(stats.serialize(), "all");
		List<PvmLootSourceStat> sources = restored.getCombatLootSources();

		assertEquals(2, sources.size());
		assertEquals("Aquanite", sources.get(0).getName());
		assertEquals(1L, sources.get(0).getKills());
		assertEquals(30_000L, sources.get(0).getTotalValue());
		assertEquals("Gargoyle", sources.get(1).getName());
		assertEquals(2L, sources.get(1).getKills());
		assertEquals(15_000L, sources.get(1).getTotalValue());
		assertEquals(4_500L, sources.get(1).getSupplyCostValue());
		assertEquals(2, sources.get(1).getDrops().size());
		assertEquals(45_000L, restored.getCombatLootValue());
	}

	@Test
	public void severalPickedItemsFromOneKillOnlyCountOneKill()
	{
		PvmToolsStats stats = new PvmToolsStats("all");
		stats.addCombatLoot("Gargoyle", 111, List.of(
			new PvmDropStat(995, 2_000, 2_000, 1)), 1_000L, true);
		stats.addCombatLoot("Gargoyle", 111, List.of(
			new PvmDropStat(100, 1, 10_000, 1)), 1_100L, false);

		PvmLootSourceStat source = stats.getCombatLootSources().get(0);
		assertEquals(1L, source.getKills());
		assertEquals(12_000L, source.getTotalValue());
		assertEquals(2, source.getDrops().size());
	}

	@Test
	public void supplyCostDoesNotCreateLootSourceBeforeConfirmedPickup()
	{
		PvmToolsStats stats = new PvmToolsStats("all");
		stats.addCombatSupplyCost("Kraken", 291, 3_287L);

		assertTrue(stats.getCombatLootSources().isEmpty());
		assertTrue(PvmToolsStats.deserialize(stats.serialize(), "all").getCombatLootSources().isEmpty());

		stats.addCombatLoot("Kraken", 291, List.of(
			new PvmDropStat(995, 100, 100, 1)), 1_000L);

		PvmLootSourceStat source = stats.getCombatLootSources().get(0);
		assertEquals("Kraken", source.getName());
		assertEquals(3_287L, source.getSupplyCostValue());
	}

	@Test
	public void pendingMonsterSupplyCostSurvivesRestartBeforeFirstPickup()
	{
		PvmToolsStats stats = new PvmToolsStats("all");
		stats.addSupplyCost(3_287L, 3L, PvmToolsPlugin.SupplyCostType.RUNE);
		stats.addCombatSupplyCost("Kraken", 291, 3_287L);

		PvmToolsStats restored = PvmToolsStats.deserialize(stats.serialize(), "all");
		assertTrue(restored.getCombatLootSources().isEmpty());
		assertEquals(3_287L, restored.getSupplyCostValue());
		restored.addCombatLoot("Kraken", 291, List.of(
			new PvmDropStat(995, 100, 100, 1)), 1_000L);
		restored.addCombatLoot("Kraken", 291, List.of(
			new PvmDropStat(995, 100, 100, 1)), 2_000L);

		assertEquals(3_287L, restored.getCombatLootSources().get(0).getSupplyCostValue());
	}

	@Test
	public void copiedStatsRetainPendingMonsterSupplyCost()
	{
		PvmToolsStats stats = new PvmToolsStats("all");
		stats.addCombatSupplyCost("Monster; with ! separators ~ and =", 100, 250L);

		PvmToolsStats restored = PvmToolsStats.deserialize(stats.copy().serialize(), "all");
		restored.addCombatLoot("Monster; with ! separators ~ and =", 100, List.of(
			new PvmDropStat(995, 100, 100, 1)), 1_000L);

		assertEquals(250L, restored.getCombatLootSources().get(0).getSupplyCostValue());
		assertTrue(stats.getCombatLootSources().isEmpty());
	}

	@Test
	public void resettingSupplyCostClearsPendingCostsAcrossRestart()
	{
		PvmToolsStats stats = new PvmToolsStats("all");
		stats.addCombatSupplyCost("Kraken", 291, 3_287L);
		stats.resetSupplyCost();

		PvmToolsStats restored = PvmToolsStats.deserialize(stats.serialize(), "all");
		restored.addCombatLoot("Kraken", 291, List.of(
			new PvmDropStat(995, 100, 100, 1)), 1_000L);

		assertEquals(0L, restored.getCombatLootSources().get(0).getSupplyCostValue());
	}

	@Test
	public void malformedPendingCostsDoNotDiscardOtherValidEntries()
	{
		PvmToolsStats restored = PvmToolsStats.deserialize(
			"period=all;pendingCombatSupplyV1=invalid!%~300!S3Jha2Vu~-5!S3Jha2Vu~3287", "all");
		restored.addCombatLoot("Kraken", 291, List.of(
			new PvmDropStat(995, 100, 100, 1)), 1_000L);

		assertEquals(3_287L, restored.getCombatLootSources().get(0).getSupplyCostValue());
	}

	@Test
	public void oldStatsFormatWithoutPendingCostsRestoresExistingTotals()
	{
		PvmToolsStats restored = PvmToolsStats.deserialize(
			"period=all;loot=123;supply=45;potion=45;potionDoses=2;slayer=89;combat=MAGIC:67;"
				+ "combatLootV1=S3Jha2Vu~291~1~1000~1000~45~995:123:123:1", "all");

		assertEquals(123L, restored.getLootValue());
		assertEquals(45L, restored.getSupplyCostValue());
		assertEquals(2L, restored.getPotionDoseCount());
		assertEquals(89L, restored.getSlayerXp());
		assertEquals(67L, restored.getCombatXp(Skill.MAGIC));
		assertEquals(45L, restored.getCombatLootSources().get(0).getSupplyCostValue());
	}

	@Test
	public void negativeAndInvalidPendingCostsCannotCreateSupplyCost()
	{
		for (String cost : List.of("-5", "0", "not-a-number"))
		{
			PvmToolsStats restored = PvmToolsStats.deserialize(
				"period=all;pendingCombatSupplyV1=S3Jha2Vu~" + cost, "all");
			restored.addCombatLoot("Kraken", 291, List.of(
				new PvmDropStat(995, 100, 100, 1)), 1_000L);

			assertEquals(0L, restored.getCombatLootSources().get(0).getSupplyCostValue());
		}
	}

	@Test
	public void lifetimeRecoveryPreservesHigherTotalsWithoutInventingDetails()
	{
		PvmToolsStats stats = new PvmToolsStats("all");
		stats.addLoot(995, 100, 100);
		stats.addCombatLoot("Kraken", 291, List.of(
			new PvmDropStat(995, 100, 100, 1)), 1_000L);
		stats.addSupplyCost(5_460_000L, 151L, PvmToolsPlugin.SupplyCostType.POTION);
		stats.addCombatSupplyCost("Kraken", 291, 5_460_000L);
		stats.addSlayerXp(594_000L);
		stats.addCombatXp(Skill.MAGIC, 722_000L);
		stats.addCombatXp(Skill.RANGED, 500_000L);
		Map<Skill, Long> chatCombat = new EnumMap<>(Skill.class);
		chatCombat.put(Skill.MAGIC, 535_000L);
		chatCombat.put(Skill.RANGED, 523_000L);

		assertTrue(stats.recoverLifetimeTrackerTotals(150L, 6_050_000L, chatCombat, 458_000L));
		assertEquals(150L, stats.getLootValue());
		assertEquals(6_050_000L, stats.getSupplyCostValue());
		assertEquals(590_000L, stats.getOtherHistoricalSupplyCostValue());
		assertEquals(594_000L, stats.getSlayerXp());
		assertEquals(722_000L, stats.getCombatXp(Skill.MAGIC));
		assertEquals(523_000L, stats.getCombatXp(Skill.RANGED));
		assertEquals(5_460_000L, stats.getPotionSupplyCostValue());
		assertEquals(151L, stats.getPotionDoseCount());
		assertEquals(1, stats.getUniqueDropCount());
		assertEquals(100L, stats.getTrackedDrops().get(0).getQuantity());
		assertEquals(100L, stats.getCombatLootSources().get(0).getTotalValue());
		assertEquals(5_460_000L, stats.getCombatLootSources().get(0).getSupplyCostValue());
		assertFalse(stats.recoverLifetimeTrackerTotals(150L, 6_050_000L, chatCombat, 458_000L));

		PvmToolsStats restored = PvmToolsStats.deserialize(stats.serialize(), "all");
		assertEquals(6_050_000L, restored.getSupplyCostValue());
		assertEquals(590_000L, restored.getOtherHistoricalSupplyCostValue());
		assertEquals(594_000L, restored.getSlayerXp());
	}

	@Test
	public void lifetimeRecoveryDoesNotRewriteDatedPeriodsOrAcceptNegativeTotals()
	{
		PvmToolsStats daily = new PvmToolsStats("2026-10-07");
		assertFalse(daily.recoverLifetimeTrackerTotals(100L, 200L, Map.of(Skill.MAGIC, 300L), 400L));
		assertEquals(0L, daily.getLootValue());
		assertEquals(0L, daily.getSupplyCostValue());
		assertEquals(0L, daily.getCombatXp(Skill.MAGIC));
		assertEquals(0L, daily.getSlayerXp());

		PvmToolsStats lifetime = new PvmToolsStats("all");
		assertFalse(lifetime.recoverLifetimeTrackerTotals(-1L, -1L, Map.of(Skill.MAGIC, -1L), -1L));
		assertEquals(0L, lifetime.getOtherHistoricalSupplyCostValue());
	}

	@Test
	public void resettingSupplyCostClearsCombatLootSourceCost()
	{
		PvmToolsStats stats = new PvmToolsStats("all");
		stats.addCombatLoot("Kraken", 291, List.of(
			new PvmDropStat(995, 100, 100, 1)), 1_000L);
		stats.addCombatSupplyCost("Kraken", 291, 3_287L);

		stats.resetSupplyCost();

		assertEquals(0L, stats.getCombatLootSources().get(0).getSupplyCostValue());
	}

	@Test
	public void legacyPreviouslyTrackedLootIsDiscarded()
	{
		PvmToolsStats stats = new PvmToolsStats("all");
		stats.addCombatLoot("Previously tracked loot", 0, List.of(
			new PvmDropStat(995, 5_000, 5_000, 1)), 1_000L);

		PvmToolsStats restored = PvmToolsStats.deserialize(stats.serialize(), "all");
		assertEquals(0, restored.getCombatLootSources().size());
	}

	@Test
	public void cannonballUsageSurvivesSerialization()
	{
		PvmToolsStats stats = new PvmToolsStats("all");
		stats.addSupplyCost(12_000, 60, PvmToolsPlugin.SupplyCostType.CANNONBALL);

		PvmToolsStats restored = PvmToolsStats.deserialize(stats.serialize(), "all");
		assertEquals(12_000L, restored.getSupplyCostValue());
		assertEquals(12_000L, restored.getCannonballSupplyCostValue());
		assertEquals(60L, restored.getCannonballCount());
	}

	@Test
	public void combatSupplyUsageSurvivesSerializationAndContributesToTotalCost()
	{
		PvmToolsStats stats = new PvmToolsStats("all");
		stats.addSupplyCost(4_500L, 75L, PvmToolsPlugin.SupplyCostType.RUNE);
		stats.addSupplyCost(8_000L, 40L, PvmToolsPlugin.SupplyCostType.AMMO);
		stats.addSupplyCost(2_500L, 100L, PvmToolsPlugin.SupplyCostType.ZULRAH_SCALE);

		PvmToolsStats restored = PvmToolsStats.deserialize(stats.serialize(), "all");
		assertEquals(15_000L, restored.getSupplyCostValue());
		assertEquals(4_500L, restored.getRuneSupplyCostValue());
		assertEquals(8_000L, restored.getAmmoSupplyCostValue());
		assertEquals(2_500L, restored.getZulrahScaleSupplyCostValue());
		assertEquals(75L, restored.getRuneCount());
		assertEquals(40L, restored.getAmmoCount());
		assertEquals(100L, restored.getZulrahScaleCount());
	}
}
