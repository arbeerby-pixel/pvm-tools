package com.arber.pvmtools;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import net.runelite.api.Skill;

class PvmToolsStats
{
	private String periodId;
	private long lootValue;
	private long supplyCostValue;
	private long potionSupplyCostValue;
	private long foodSupplyCostValue;
	private long cannonballSupplyCostValue;
	private long potionDoseCount;
	private long foodCount;
	private long cannonballCount;
	private long slayerXp;
	private final EnumMap<Skill, Long> combatXpBySkill = new EnumMap<>(Skill.class);
	private final Map<Integer, DropTotals> dropsByItem = new HashMap<>();
	private PvmDropStat bestPickup;

	PvmToolsStats(String periodId)
	{
		this.periodId = periodId;
		for (Skill skill : PvmToolsPlugin.COMBAT_TRACKER_SKILLS)
		{
			combatXpBySkill.put(skill, 0L);
		}
	}

	static PvmToolsStats deserialize(String serialized, String currentPeriodId)
	{
		PvmToolsStats stats = new PvmToolsStats(currentPeriodId);
		if (serialized == null || serialized.isBlank())
		{
			return stats;
		}

		for (String savedValue : serialized.split(";"))
		{
			String[] parts = savedValue.split("=", 2);
			if (parts.length != 2)
			{
				continue;
			}

			switch (parts[0])
			{
				case "period":
					stats.periodId = parts[1];
					break;
				case "loot":
					stats.lootValue = parseLong(parts[1]);
					break;
				case "supply":
					stats.supplyCostValue = parseLong(parts[1]);
					break;
				case "potion":
					stats.potionSupplyCostValue = parseLong(parts[1]);
					break;
				case "food":
					stats.foodSupplyCostValue = parseLong(parts[1]);
					break;
				case "cannon":
					stats.cannonballSupplyCostValue = parseLong(parts[1]);
					break;
				case "potionDoses":
					stats.potionDoseCount = parseLong(parts[1]);
					break;
				case "foodCount":
					stats.foodCount = parseLong(parts[1]);
					break;
				case "cannonCount":
					stats.cannonballCount = parseLong(parts[1]);
					break;
				case "slayer":
					stats.slayerXp = parseLong(parts[1]);
					break;
				case "combat":
					stats.parseCombatXp(parts[1]);
					break;
				case "dropsV2":
					stats.parseDrops(parts[1]);
					break;
				case "bestPickupV2":
					stats.bestPickup = parseDropStat(parts[1]);
					break;
			}
		}

		if (!"all".equals(currentPeriodId) && !currentPeriodId.equals(stats.periodId))
		{
			return new PvmToolsStats(currentPeriodId);
		}

		if ("all".equals(currentPeriodId))
		{
			stats.periodId = currentPeriodId;
		}

		return stats;
	}

	PvmToolsStats copy()
	{
		PvmToolsStats copy = new PvmToolsStats(periodId);
		copy.lootValue = lootValue;
		copy.supplyCostValue = supplyCostValue;
		copy.potionSupplyCostValue = potionSupplyCostValue;
		copy.foodSupplyCostValue = foodSupplyCostValue;
		copy.cannonballSupplyCostValue = cannonballSupplyCostValue;
		copy.potionDoseCount = potionDoseCount;
		copy.foodCount = foodCount;
		copy.cannonballCount = cannonballCount;
		copy.slayerXp = slayerXp;
		copy.combatXpBySkill.putAll(combatXpBySkill);
		for (Map.Entry<Integer, DropTotals> entry : dropsByItem.entrySet())
		{
			copy.dropsByItem.put(entry.getKey(), entry.getValue().copy());
		}
		copy.bestPickup = bestPickup == null ? null : new PvmDropStat(bestPickup.getItemId(), bestPickup.getQuantity(), bestPickup.getValue(), bestPickup.getPickupCount());
		return copy;
	}

	String serialize()
	{
		StringBuilder combat = new StringBuilder();
		for (Map.Entry<Skill, Long> entry : combatXpBySkill.entrySet())
		{
			if (combat.length() > 0)
			{
				combat.append(',');
			}

			combat.append(entry.getKey().name()).append(':').append(entry.getValue());
		}

		StringBuilder drops = new StringBuilder();
		for (Map.Entry<Integer, DropTotals> entry : dropsByItem.entrySet())
		{
			if (drops.length() > 0)
			{
				drops.append(',');
			}
			drops.append(entry.getKey())
				.append(':').append(entry.getValue().quantity)
				.append(':').append(entry.getValue().value)
				.append(':').append(entry.getValue().pickupCount);
		}

		return "period=" + periodId
			+ ";loot=" + lootValue
			+ ";supply=" + supplyCostValue
			+ ";potion=" + potionSupplyCostValue
			+ ";food=" + foodSupplyCostValue
			+ ";cannon=" + cannonballSupplyCostValue
			+ ";potionDoses=" + potionDoseCount
			+ ";foodCount=" + foodCount
			+ ";cannonCount=" + cannonballCount
			+ ";slayer=" + slayerXp
			+ ";combat=" + combat
			+ ";dropsV2=" + drops
			+ ";bestPickupV2=" + serializeDropStat(bestPickup);
	}

	String getPeriodId()
	{
		return periodId;
	}

	void addLoot(int itemId, long quantity, long value)
	{
		long safeQuantity = Math.max(0L, quantity);
		long safeValue = Math.max(0L, value);
		lootValue += safeValue;
		if (itemId < 0 || safeQuantity <= 0L)
		{
			return;
		}

		dropsByItem.computeIfAbsent(itemId, ignored -> new DropTotals()).add(safeQuantity, safeValue, 1L);
		if (bestPickup == null || safeValue > bestPickup.getValue())
		{
			bestPickup = new PvmDropStat(itemId, safeQuantity, safeValue, 1L);
		}
	}

	void addSupplyCost(long value, long count, PvmToolsPlugin.SupplyCostType type)
	{
		long safeValue = Math.max(0L, value);
		long safeCount = Math.max(0L, count);
		supplyCostValue += safeValue;
		switch (type)
		{
			case POTION:
				potionSupplyCostValue += safeValue;
				potionDoseCount += safeCount;
				break;
			case FOOD:
				foodSupplyCostValue += safeValue;
				foodCount += safeCount;
				break;
			case CANNONBALL:
				cannonballSupplyCostValue += safeValue;
				cannonballCount += safeCount;
				break;
		}
	}

	void addCombatXp(Skill skill, long xp)
	{
		if (combatXpBySkill.containsKey(skill))
		{
			combatXpBySkill.merge(skill, Math.max(0L, xp), Long::sum);
		}
	}

	void addSlayerXp(long xp)
	{
		slayerXp += Math.max(0L, xp);
	}

	void resetLoot()
	{
		lootValue = 0L;
		dropsByItem.clear();
		bestPickup = null;
	}

	void resetSupplyCost()
	{
		supplyCostValue = 0L;
		potionSupplyCostValue = 0L;
		foodSupplyCostValue = 0L;
		cannonballSupplyCostValue = 0L;
		potionDoseCount = 0L;
		foodCount = 0L;
		cannonballCount = 0L;
	}

	void resetCombatXp()
	{
		for (Skill skill : PvmToolsPlugin.COMBAT_TRACKER_SKILLS)
		{
			combatXpBySkill.put(skill, 0L);
		}
	}

	void resetSlayerXp()
	{
		slayerXp = 0L;
	}

	long getLootValue()
	{
		return lootValue;
	}

	long getSupplyCostValue()
	{
		return supplyCostValue;
	}

	long getNetProfit()
	{
		return lootValue - supplyCostValue;
	}

	long getPotionSupplyCostValue()
	{
		return potionSupplyCostValue;
	}

	long getFoodSupplyCostValue()
	{
		return foodSupplyCostValue;
	}

	long getCannonballSupplyCostValue()
	{
		return cannonballSupplyCostValue;
	}

	long getPotionDoseCount()
	{
		return potionDoseCount;
	}

	long getFoodCount()
	{
		return foodCount;
	}

	long getCannonballCount()
	{
		return cannonballCount;
	}

	long getCombatXp()
	{
		long total = 0L;
		for (long xp : combatXpBySkill.values())
		{
			total += xp;
		}
		return total;
	}

	long getCombatXp(Skill skill)
	{
		return combatXpBySkill.getOrDefault(skill, 0L);
	}

	long getSlayerXp()
	{
		return slayerXp;
	}

	PvmDropStat getMostCommonDrop()
	{
		return findTopDrop(true);
	}

	PvmDropStat getMostValuableDrop()
	{
		return findTopDrop(false);
	}

	PvmDropStat getBestPickup()
	{
		return bestPickup;
	}

	int getUniqueDropCount()
	{
		return dropsByItem.size();
	}

	private PvmDropStat findTopDrop(boolean byQuantity)
	{
		PvmDropStat best = null;
		for (Map.Entry<Integer, DropTotals> entry : dropsByItem.entrySet())
		{
			DropTotals totals = entry.getValue();
			PvmDropStat candidate = new PvmDropStat(entry.getKey(), totals.quantity, totals.value, totals.pickupCount);
			if (best == null
				|| byQuantity && candidate.getPickupCount() > best.getPickupCount()
				|| !byQuantity && candidate.getValue() > best.getValue())
			{
				best = candidate;
			}
		}
		return best;
	}

	private void parseDrops(String value)
	{
		if (value == null || value.isBlank())
		{
			return;
		}

		for (String serializedDrop : value.split(","))
		{
			PvmDropStat drop = parseDropStat(serializedDrop);
			if (drop != null && drop.getItemId() >= 0 && drop.getQuantity() > 0L)
			{
				dropsByItem.put(drop.getItemId(), new DropTotals(drop.getQuantity(), drop.getValue(), drop.getPickupCount()));
			}
		}
	}

	private static PvmDropStat parseDropStat(String value)
	{
		if (value == null || value.isBlank())
		{
			return null;
		}

		String[] parts = value.split(":", 4);
		if (parts.length != 4)
		{
			return null;
		}

		try
		{
			int itemId = Integer.parseInt(parts[0]);
			return new PvmDropStat(itemId, parseLong(parts[1]), parseLong(parts[2]), parseLong(parts[3]));
		}
		catch (NumberFormatException ex)
		{
			return null;
		}
	}

	private static String serializeDropStat(PvmDropStat drop)
	{
		return drop == null ? "" : drop.getItemId() + ":" + drop.getQuantity() + ":" + drop.getValue() + ":" + drop.getPickupCount();
	}

	private void parseCombatXp(String value)
	{
		if (value == null || value.isBlank())
		{
			return;
		}

		for (String skillValue : value.split(","))
		{
			String[] parts = skillValue.split(":", 2);
			if (parts.length != 2)
			{
				continue;
			}

			try
			{
				Skill skill = Skill.valueOf(parts[0]);
				if (combatXpBySkill.containsKey(skill))
				{
					combatXpBySkill.put(skill, parseLong(parts[1]));
				}
			}
			catch (IllegalArgumentException ignored)
			{
				// Ignore stale or manually edited values.
			}
		}
	}

	private static long parseLong(String value)
	{
		try
		{
			return Math.max(0L, Long.parseLong(value.trim()));
		}
		catch (NumberFormatException ex)
		{
			return 0L;
		}
	}

	private static final class DropTotals
	{
		private long quantity;
		private long value;
		private long pickupCount;

		private DropTotals()
		{
		}

		private DropTotals(long quantity, long value, long pickupCount)
		{
			this.quantity = Math.max(0L, quantity);
			this.value = Math.max(0L, value);
			this.pickupCount = Math.max(0L, pickupCount);
		}

		private void add(long quantity, long value, long pickupCount)
		{
			this.quantity += quantity;
			this.value += value;
			this.pickupCount += pickupCount;
		}

		private DropTotals copy()
		{
			return new DropTotals(quantity, value, pickupCount);
		}
	}
}
