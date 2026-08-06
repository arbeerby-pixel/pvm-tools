package com.arber.pvmtools;

import java.time.LocalDate;
import java.util.List;
import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
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
