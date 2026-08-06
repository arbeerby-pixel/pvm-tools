package com.arber.pvmtools;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
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
	private long runeSupplyCostValue;
	private long ammoSupplyCostValue;
	private long zulrahScaleSupplyCostValue;
	private long potionDoseCount;
	private long foodCount;
	private long cannonballCount;
	private long runeCount;
	private long ammoCount;
	private long zulrahScaleCount;
	private long slayerXp;
	private final EnumMap<Skill, Long> combatXpBySkill = new EnumMap<>(Skill.class);
	private final Map<Integer, DropTotals> dropsByItem = new HashMap<>();
	private final Map<String, LootSourceTotals> combatLootBySource = new LinkedHashMap<>();
	private final Map<String, Long> pendingCombatSupplyCostBySource = new HashMap<>();
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
				case "runes":
					stats.runeSupplyCostValue = parseLong(parts[1]);
					break;
				case "ammo":
					stats.ammoSupplyCostValue = parseLong(parts[1]);
					break;
				case "scales":
					stats.zulrahScaleSupplyCostValue = parseLong(parts[1]);
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
				case "runeCount":
					stats.runeCount = parseLong(parts[1]);
					break;
				case "ammoCount":
					stats.ammoCount = parseLong(parts[1]);
					break;
				case "scaleCount":
					stats.zulrahScaleCount = parseLong(parts[1]);
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
				case "combatLootV1":
					stats.parseCombatLoot(parts[1]);
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
		copy.runeSupplyCostValue = runeSupplyCostValue;
		copy.ammoSupplyCostValue = ammoSupplyCostValue;
		copy.zulrahScaleSupplyCostValue = zulrahScaleSupplyCostValue;
		copy.potionDoseCount = potionDoseCount;
		copy.foodCount = foodCount;
		copy.cannonballCount = cannonballCount;
		copy.runeCount = runeCount;
		copy.ammoCount = ammoCount;
		copy.zulrahScaleCount = zulrahScaleCount;
		copy.slayerXp = slayerXp;
		copy.combatXpBySkill.putAll(combatXpBySkill);
		for (Map.Entry<Integer, DropTotals> entry : dropsByItem.entrySet())
		{
			copy.dropsByItem.put(entry.getKey(), entry.getValue().copy());
		}
		for (Map.Entry<String, LootSourceTotals> entry : combatLootBySource.entrySet())
		{
			copy.combatLootBySource.put(entry.getKey(), entry.getValue().copy());
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

		StringBuilder combatLoot = new StringBuilder();
		for (LootSourceTotals source : combatLootBySource.values())
		{
			if (source.dropsByItem.isEmpty())
			{
				continue;
			}
			if (combatLoot.length() > 0)
			{
				combatLoot.append('!');
			}
			combatLoot.append(source.serialize());
		}

		return "period=" + periodId
			+ ";loot=" + lootValue
			+ ";supply=" + supplyCostValue
			+ ";potion=" + potionSupplyCostValue
			+ ";food=" + foodSupplyCostValue
			+ ";cannon=" + cannonballSupplyCostValue
			+ ";runes=" + runeSupplyCostValue
			+ ";ammo=" + ammoSupplyCostValue
			+ ";scales=" + zulrahScaleSupplyCostValue
			+ ";potionDoses=" + potionDoseCount
			+ ";foodCount=" + foodCount
			+ ";cannonCount=" + cannonballCount
			+ ";runeCount=" + runeCount
			+ ";ammoCount=" + ammoCount
			+ ";scaleCount=" + zulrahScaleCount
			+ ";slayer=" + slayerXp
			+ ";combat=" + combat
			+ ";dropsV2=" + drops
			+ ";bestPickupV2=" + serializeDropStat(bestPickup)
			+ ";combatLootV1=" + combatLoot;
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
			case RUNE:
				runeSupplyCostValue += safeValue;
				runeCount += safeCount;
				break;
			case AMMO:
				ammoSupplyCostValue += safeValue;
				ammoCount += safeCount;
				break;
			case ZULRAH_SCALE:
				zulrahScaleSupplyCostValue += safeValue;
				zulrahScaleCount += safeCount;
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
		combatLootBySource.clear();
		pendingCombatSupplyCostBySource.clear();
		bestPickup = null;
	}

	void resetSupplyCost()
	{
		supplyCostValue = 0L;
		potionSupplyCostValue = 0L;
		foodSupplyCostValue = 0L;
		cannonballSupplyCostValue = 0L;
		runeSupplyCostValue = 0L;
		ammoSupplyCostValue = 0L;
		zulrahScaleSupplyCostValue = 0L;
		potionDoseCount = 0L;
		foodCount = 0L;
		cannonballCount = 0L;
		runeCount = 0L;
		ammoCount = 0L;
		zulrahScaleCount = 0L;
		pendingCombatSupplyCostBySource.clear();
		for (LootSourceTotals source : combatLootBySource.values())
		{
			source.supplyCostValue = 0L;
		}
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

	void addCombatLoot(String sourceName, int combatLevel, List<PvmDropStat> drops, long timestampMillis)
	{
		addCombatLoot(sourceName, combatLevel, drops, timestampMillis, true);
	}

	void addCombatLoot(String sourceName, int combatLevel, List<PvmDropStat> drops, long timestampMillis, boolean countKill)
	{
		if (sourceName == null || sourceName.isBlank() || drops == null || drops.isEmpty())
		{
			return;
		}

		String cleanName = sourceName.trim();
		String normalizedName = cleanName.toLowerCase(java.util.Locale.ENGLISH);
		LootSourceTotals source = combatLootBySource.computeIfAbsent(
			normalizedName,
			ignored -> new LootSourceTotals(cleanName, combatLevel));
		source.supplyCostValue += pendingCombatSupplyCostBySource.getOrDefault(normalizedName, 0L);
		pendingCombatSupplyCostBySource.remove(normalizedName);
		source.addLoot(combatLevel, drops, timestampMillis, countKill);
	}

	void addCombatSupplyCost(String sourceName, int combatLevel, long value)
	{
		if (sourceName == null || sourceName.isBlank() || value <= 0L)
		{
			return;
		}

		String normalizedName = sourceName.trim().toLowerCase(java.util.Locale.ENGLISH);
		LootSourceTotals source = combatLootBySource.get(normalizedName);
		if (source == null || source.dropsByItem.isEmpty())
		{
			pendingCombatSupplyCostBySource.merge(normalizedName, value, Long::sum);
			return;
		}

		source.combatLevel = Math.max(source.combatLevel, combatLevel);
		source.supplyCostValue += value;
	}

	long getRuneSupplyCostValue()
	{
		return runeSupplyCostValue;
	}

	long getAmmoSupplyCostValue()
	{
		return ammoSupplyCostValue;
	}

	long getZulrahScaleSupplyCostValue()
	{
		return zulrahScaleSupplyCostValue;
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

	long getRuneCount()
	{
		return runeCount;
	}

	long getAmmoCount()
	{
		return ammoCount;
	}

	long getZulrahScaleCount()
	{
		return zulrahScaleCount;
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

	List<PvmDropStat> getTrackedDrops()
	{
		List<PvmDropStat> drops = new ArrayList<>();
		for (Map.Entry<Integer, DropTotals> entry : dropsByItem.entrySet())
		{
			DropTotals totals = entry.getValue();
			drops.add(new PvmDropStat(entry.getKey(), totals.quantity, totals.value, totals.pickupCount));
		}
		drops.sort(Comparator
			.comparingLong(PvmDropStat::getValue)
			.reversed()
			.thenComparing(Comparator.comparingLong(PvmDropStat::getPickupCount).reversed())
			.thenComparingInt(PvmDropStat::getItemId));
		return drops;
	}

	List<PvmLootSourceStat> getCombatLootSources()
	{
		List<PvmLootSourceStat> sources = new ArrayList<>();
		for (LootSourceTotals source : combatLootBySource.values())
		{
			if (!source.dropsByItem.isEmpty())
			{
				sources.add(source.toStat());
			}
		}
		sources.sort(Comparator
			.comparingLong(PvmLootSourceStat::getLastLootMillis)
			.reversed()
			.thenComparing(Comparator.comparingLong(PvmLootSourceStat::getTotalValue).reversed())
			.thenComparing(PvmLootSourceStat::getName));
		return sources;
	}

	long getCombatLootValue()
	{
		long total = 0L;
		for (LootSourceTotals source : combatLootBySource.values())
		{
			if (!source.dropsByItem.isEmpty())
			{
				total += source.getTotalValue();
			}
		}
		return total;
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

	private void parseCombatLoot(String value)
	{
		if (value == null || value.isBlank())
		{
			return;
		}

		for (String serializedSource : value.split("!"))
		{
			LootSourceTotals source = LootSourceTotals.deserialize(serializedSource);
			if (source != null
				&& !"Previously tracked loot".equalsIgnoreCase(source.name)
				&& !source.dropsByItem.isEmpty())
			{
				combatLootBySource.put(source.name.toLowerCase(java.util.Locale.ENGLISH), source);
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

	private static final class LootSourceTotals
	{
		private final String name;
		private int combatLevel;
		private long kills;
		private long firstLootMillis;
		private long lastLootMillis;
		private long supplyCostValue;
		private final Map<Integer, DropTotals> dropsByItem = new LinkedHashMap<>();

		private LootSourceTotals(String name, int combatLevel)
		{
			this.name = name;
			this.combatLevel = Math.max(0, combatLevel);
		}

		private void addLoot(int level, List<PvmDropStat> drops, long timestampMillis, boolean countKill)
		{
			combatLevel = Math.max(combatLevel, level);
			if (countKill)
			{
				kills++;
			}
			long safeTimestamp = Math.max(0L, timestampMillis);
			if (firstLootMillis == 0L || safeTimestamp > 0L && safeTimestamp < firstLootMillis)
			{
				firstLootMillis = safeTimestamp;
			}
			lastLootMillis = Math.max(lastLootMillis, safeTimestamp);
			for (PvmDropStat drop : drops)
			{
				if (drop != null && drop.getItemId() >= 0 && drop.getQuantity() > 0L)
				{
					dropsByItem.computeIfAbsent(drop.getItemId(), ignored -> new DropTotals())
						.add(drop.getQuantity(), drop.getValue(), 1L);
				}
			}
		}

		private long getTotalValue()
		{
			long total = 0L;
			for (DropTotals drop : dropsByItem.values())
			{
				total += drop.value;
			}
			return total;
		}

		private PvmLootSourceStat toStat()
		{
			List<PvmDropStat> drops = new ArrayList<>();
			for (Map.Entry<Integer, DropTotals> entry : dropsByItem.entrySet())
			{
				DropTotals totals = entry.getValue();
				drops.add(new PvmDropStat(entry.getKey(), totals.quantity, totals.value, totals.pickupCount));
			}
			drops.sort(Comparator
				.comparingLong(PvmDropStat::getValue)
				.reversed()
				.thenComparingInt(PvmDropStat::getItemId));
			return new PvmLootSourceStat(name, combatLevel, kills, firstLootMillis, lastLootMillis, supplyCostValue, drops);
		}

		private LootSourceTotals copy()
		{
			LootSourceTotals copy = new LootSourceTotals(name, combatLevel);
			copy.kills = kills;
			copy.firstLootMillis = firstLootMillis;
			copy.lastLootMillis = lastLootMillis;
			copy.supplyCostValue = supplyCostValue;
			for (Map.Entry<Integer, DropTotals> entry : dropsByItem.entrySet())
			{
				copy.dropsByItem.put(entry.getKey(), entry.getValue().copy());
			}
			return copy;
		}

		private String serialize()
		{
			StringBuilder items = new StringBuilder();
			for (Map.Entry<Integer, DropTotals> entry : dropsByItem.entrySet())
			{
				if (items.length() > 0)
				{
					items.append(',');
				}
				DropTotals totals = entry.getValue();
				items.append(entry.getKey())
					.append(':').append(totals.quantity)
					.append(':').append(totals.value)
					.append(':').append(totals.pickupCount);
			}

			String encodedName = Base64.getUrlEncoder().withoutPadding()
				.encodeToString(name.getBytes(StandardCharsets.UTF_8));
			return encodedName + '~' + combatLevel + '~' + kills + '~' + firstLootMillis + '~' + lastLootMillis
				+ '~' + supplyCostValue + '~' + items;
		}

		private static LootSourceTotals deserialize(String value)
		{
			try
			{
				String[] parts = value.split("~", 7);
				if (parts.length != 6 && parts.length != 7)
				{
					return null;
				}
				String name = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
				LootSourceTotals source = new LootSourceTotals(name, (int) parseLong(parts[1]));
				source.kills = parseLong(parts[2]);
				source.firstLootMillis = parseLong(parts[3]);
				source.lastLootMillis = parseLong(parts[4]);
				int itemsIndex = parts.length == 7 ? 6 : 5;
				if (parts.length == 7)
				{
					source.supplyCostValue = parseLong(parts[5]);
				}
				if (!parts[itemsIndex].isBlank())
				{
					for (String serializedDrop : parts[itemsIndex].split(","))
					{
						PvmDropStat drop = parseDropStat(serializedDrop);
						if (drop != null && drop.getItemId() >= 0 && drop.getQuantity() > 0L)
						{
							source.dropsByItem.put(drop.getItemId(), new DropTotals(
								drop.getQuantity(), drop.getValue(), drop.getPickupCount()));
						}
					}
				}
				return source;
			}
			catch (IllegalArgumentException ex)
			{
				return null;
			}
		}
	}
}
