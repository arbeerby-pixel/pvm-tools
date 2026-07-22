package com.arber.pvmtools;

import java.time.LocalDate;
import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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
	public void cannonballUsageSurvivesSerialization()
	{
		PvmToolsStats stats = new PvmToolsStats("all");
		stats.addSupplyCost(12_000, 60, PvmToolsPlugin.SupplyCostType.CANNONBALL);

		PvmToolsStats restored = PvmToolsStats.deserialize(stats.serialize(), "all");
		assertEquals(12_000L, restored.getSupplyCostValue());
		assertEquals(12_000L, restored.getCannonballSupplyCostValue());
		assertEquals(60L, restored.getCannonballCount());
	}
}
