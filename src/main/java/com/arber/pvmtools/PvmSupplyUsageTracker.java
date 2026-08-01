package com.arber.pvmtools;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.IntPredicate;
import net.runelite.api.Client;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.ItemID;
import net.runelite.api.Player;
import net.runelite.api.Projectile;
import net.runelite.api.Skill;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.ProjectileMoved;
import net.runelite.api.events.StatChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.game.ItemManager;
import net.runelite.client.util.Text;

/**
 * Converts client-side inventory, equipment, rune pouch, and weapon changes into
 * supply usage. Blowpipe internals are not exposed by the client, so their
 * consumption uses deterministic expected rates instead of random estimates.
 */
final class PvmSupplyUsageTracker
{
	private static final int EQUIPMENT_WEAPON_SLOT = EquipmentInventorySlot.WEAPON.getSlotIdx();
	private static final int EQUIPMENT_AMMO_SLOT = EquipmentInventorySlot.AMMO.getSlotIdx();
	private static final int EQUIPMENT_CAPE_SLOT = EquipmentInventorySlot.CAPE.getSlotIdx();
	private static final int BLOWPIPE_ATTACK_ANIMATION = 5061;
	private static final int BLOWPIPE_ORNAMENT_ATTACK_ANIMATION = 10656;
	private static final int POWERED_STAFF_ATTACK_ANIMATION = 1167;
	private static final int SHADOW_ATTACK_ANIMATION = 9493;
	private static final int MAX_RUNE_USE_PER_CAST = 35;
	private static final int CAST_ACTION_TICK_WINDOW = 2;

	private static final int[] RUNE_POUCH_TYPE_VARBITS = {
		VarbitID.RUNE_POUCH_TYPE_1,
		VarbitID.RUNE_POUCH_TYPE_2,
		VarbitID.RUNE_POUCH_TYPE_3,
		VarbitID.RUNE_POUCH_TYPE_4,
		VarbitID.RUNE_POUCH_TYPE_5,
		VarbitID.RUNE_POUCH_TYPE_6
	};
	private static final int[] RUNE_POUCH_QUANTITY_VARBITS = {
		VarbitID.RUNE_POUCH_QUANTITY_1,
		VarbitID.RUNE_POUCH_QUANTITY_2,
		VarbitID.RUNE_POUCH_QUANTITY_3,
		VarbitID.RUNE_POUCH_QUANTITY_4,
		VarbitID.RUNE_POUCH_QUANTITY_5,
		VarbitID.RUNE_POUCH_QUANTITY_6
	};

	private static final Map<Integer, Integer> RUNE_POUCH_ITEMS = buildRunePouchItems();

	private final Client client;
	private final ItemManager itemManager;
	private final SupplyUsageConsumer usageConsumer;
	private final Map<Integer, Integer> inventoryRuneCounts = new HashMap<>();
	private final Map<Integer, Integer> pendingRuneUsage = new HashMap<>();
	private final int[] runePouchTypes = new int[RUNE_POUCH_TYPE_VARBITS.length];
	private final int[] previousRunePouchTypes = new int[RUNE_POUCH_TYPE_VARBITS.length];
	private final int[] runePouchQuantities = new int[RUNE_POUCH_QUANTITY_VARBITS.length];
	private final Map<Integer, BlowpipeDart> blowpipeDartsByProjectile = new HashMap<>();

	private boolean initialized;
	private boolean magicXpChangedThisTick;
	private int lastCastActionTick = Integer.MIN_VALUE;
	private int weaponItemId = -1;
	private int weaponQuantity;
	private int ammoItemId = -1;
	private int ammoQuantity;
	private int capeItemId = -1;
	private int quiverAmmoItemId = -1;
	private int quiverAmmoQuantity;
	private BlowpipeDart blowpipeDart;
	private int lastBlowpipeShotTick = Integer.MIN_VALUE;
	private int lastPoweredStaffShotTick = Integer.MIN_VALUE;
	private double blowpipeDartUsageRemainder;
	private int blowpipeScaleUsageRemainder;

	PvmSupplyUsageTracker(Client client, ItemManager itemManager, SupplyUsageConsumer usageConsumer)
	{
		this.client = client;
		this.itemManager = itemManager;
		this.usageConsumer = usageConsumer;
		registerBlowpipeDart(ItemID.BRONZE_DART, 226);
		registerBlowpipeDart(ItemID.IRON_DART, 227);
		registerBlowpipeDart(ItemID.STEEL_DART, 228);
		registerBlowpipeDart(ItemID.BLACK_DART, 32);
		registerBlowpipeDart(ItemID.MITHRIL_DART, 229);
		registerBlowpipeDart(ItemID.ADAMANT_DART, 230);
		registerBlowpipeDart(ItemID.RUNE_DART, 231);
		registerBlowpipeDart(ItemID.AMETHYST_DART, 1936);
		registerBlowpipeDart(ItemID.DRAGON_DART, 1122);
	}

	void initialize()
	{
		reset();
		syncInventoryBaseline(client.getItemContainer(InventoryID.INVENTORY));
		syncEquipmentBaseline(client.getItemContainer(InventoryID.EQUIPMENT));
		for (int index = 0; index < RUNE_POUCH_TYPE_VARBITS.length; index++)
		{
			runePouchTypes[index] = client.getVarbitValue(RUNE_POUCH_TYPE_VARBITS[index]);
			previousRunePouchTypes[index] = runePouchTypes[index];
			runePouchQuantities[index] = client.getVarbitValue(RUNE_POUCH_QUANTITY_VARBITS[index]);
		}
		quiverAmmoItemId = client.getVarpValue(VarPlayerID.DIZANAS_QUIVER_TEMP_AMMO);
		quiverAmmoQuantity = client.getVarpValue(VarPlayerID.DIZANAS_QUIVER_TEMP_AMMO_AMOUNT);
		initialized = true;
	}

	void reset()
	{
		initialized = false;
		magicXpChangedThisTick = false;
		lastCastActionTick = Integer.MIN_VALUE;
		inventoryRuneCounts.clear();
		pendingRuneUsage.clear();
		weaponItemId = -1;
		weaponQuantity = 0;
		ammoItemId = -1;
		ammoQuantity = 0;
		capeItemId = -1;
		quiverAmmoItemId = -1;
		quiverAmmoQuantity = 0;
		blowpipeDart = null;
		lastBlowpipeShotTick = Integer.MIN_VALUE;
		lastPoweredStaffShotTick = Integer.MIN_VALUE;
		blowpipeDartUsageRemainder = 0d;
		blowpipeScaleUsageRemainder = 0;
	}

	void onMenuOptionClicked(MenuOptionClicked event)
	{
		String option = Text.removeTags(event.getMenuOption());
		if (option != null && option.toLowerCase(Locale.ENGLISH).startsWith("cast"))
		{
			lastCastActionTick = client.getTickCount();
		}
	}

	void onStatChanged(StatChanged event)
	{
		if (event.getSkill() == Skill.MAGIC)
		{
			magicXpChangedThisTick = true;
		}
	}

	void onItemContainerChanged(ItemContainerChanged event)
	{
		if (!initialized)
		{
			initialize();
		}

		if (event.getContainerId() == InventoryID.INVENTORY.getId())
		{
			trackInventoryRunes(event.getItemContainer());
		}
		else if (event.getContainerId() == InventoryID.EQUIPMENT.getId())
		{
			trackEquipmentAmmo(event.getItemContainer());
		}
	}

	void onVarbitChanged(VarbitChanged event)
	{
		if (!initialized)
		{
			return;
		}

		for (int index = 0; index < RUNE_POUCH_TYPE_VARBITS.length; index++)
		{
			if (event.getVarbitId() == RUNE_POUCH_TYPE_VARBITS[index])
			{
				previousRunePouchTypes[index] = runePouchTypes[index];
				runePouchTypes[index] = client.getVarbitValue(RUNE_POUCH_TYPE_VARBITS[index]);
				return;
			}

			if (event.getVarbitId() == RUNE_POUCH_QUANTITY_VARBITS[index])
			{
				int newQuantity = client.getVarbitValue(RUNE_POUCH_QUANTITY_VARBITS[index]);
				int used = runePouchQuantities[index] - newQuantity;
				if (used > 0 && used < MAX_RUNE_USE_PER_CAST)
				{
					int runeType = runePouchTypes[index] > 0 ? runePouchTypes[index] : previousRunePouchTypes[index];
					Integer runeItemId = RUNE_POUCH_ITEMS.get(runeType);
					if (runeItemId != null)
					{
						pendingRuneUsage.merge(runeItemId, used, Integer::sum);
					}
				}
				runePouchQuantities[index] = newQuantity;
				return;
			}
		}

		if (event.getVarpId() == VarPlayerID.DIZANAS_QUIVER_TEMP_AMMO
			|| event.getVarpId() == VarPlayerID.DIZANAS_QUIVER_TEMP_AMMO_AMOUNT)
		{
			trackQuiverAmmo();
		}
	}

	void onProjectileMoved(ProjectileMoved event)
	{
		Player localPlayer = client.getLocalPlayer();
		if (localPlayer == null || !isBlowpipe(weaponItemId) || !isBlowpipeAnimation(localPlayer.getAnimation()))
		{
			return;
		}

		Projectile projectile = event.getProjectile();
		BlowpipeDart detectedDart = blowpipeDartsByProjectile.get(projectile.getId());
		if (detectedDart != null)
		{
			blowpipeDart = detectedDart;
		}
	}

	void onGameTick()
	{
		if (!initialized)
		{
			initialize();
		}

		int tick = client.getTickCount();
		boolean castObserved = magicXpChangedThisTick
			|| tick - lastCastActionTick >= 0 && tick - lastCastActionTick <= CAST_ACTION_TICK_WINDOW;
		if (castObserved)
		{
			pendingRuneUsage.forEach((itemId, quantity) -> record(itemId, quantity, PvmToolsPlugin.SupplyCostType.RUNE));
		}
		pendingRuneUsage.clear();
		trackWeaponChargeUsage(tick);
		magicXpChangedThisTick = false;
	}

	private void trackInventoryRunes(ItemContainer itemContainer)
	{
		Map<Integer, Integer> newCounts = collectItems(itemContainer, RUNE_POUCH_ITEMS::containsValue);
		inventoryRuneCounts.forEach((itemId, oldQuantity) ->
		{
			int used = oldQuantity - newCounts.getOrDefault(itemId, 0);
			if (used > 0 && used < MAX_RUNE_USE_PER_CAST)
			{
				pendingRuneUsage.merge(itemId, used, Integer::sum);
			}
		});
		inventoryRuneCounts.clear();
		inventoryRuneCounts.putAll(newCounts);
	}

	private void syncInventoryBaseline(ItemContainer itemContainer)
	{
		inventoryRuneCounts.clear();
		inventoryRuneCounts.putAll(collectItems(itemContainer, RUNE_POUCH_ITEMS::containsValue));
	}

	private Map<Integer, Integer> collectItems(ItemContainer itemContainer, IntPredicate filter)
	{
		Map<Integer, Integer> counts = new HashMap<>();
		if (itemContainer == null)
		{
			return counts;
		}

		for (Item item : itemContainer.getItems())
		{
			if (item.getId() > 0 && filter.test(item.getId()))
			{
				counts.merge(item.getId(), Math.max(0, item.getQuantity()), Integer::sum);
			}
		}
		return counts;
	}

	private void trackEquipmentAmmo(ItemContainer equipment)
	{
		Item newWeapon = getEquipmentItem(equipment, EQUIPMENT_WEAPON_SLOT);
		Item newAmmo = getEquipmentItem(equipment, EQUIPMENT_AMMO_SLOT);
		Item newCape = getEquipmentItem(equipment, EQUIPMENT_CAPE_SLOT);
		trackEquipmentSlotUsage(weaponItemId, weaponQuantity, newWeapon, this::isThrownAmmo);
		trackEquipmentSlotUsage(ammoItemId, ammoQuantity, newAmmo, this::isRangedAmmo);
		weaponItemId = newWeapon.getId();
		weaponQuantity = Math.max(0, newWeapon.getQuantity());
		ammoItemId = newAmmo.getId();
		ammoQuantity = Math.max(0, newAmmo.getQuantity());
		capeItemId = newCape.getId();
	}

	private void syncEquipmentBaseline(ItemContainer equipment)
	{
		Item weapon = getEquipmentItem(equipment, EQUIPMENT_WEAPON_SLOT);
		Item ammo = getEquipmentItem(equipment, EQUIPMENT_AMMO_SLOT);
		Item cape = getEquipmentItem(equipment, EQUIPMENT_CAPE_SLOT);
		weaponItemId = weapon.getId();
		weaponQuantity = Math.max(0, weapon.getQuantity());
		ammoItemId = ammo.getId();
		ammoQuantity = Math.max(0, ammo.getQuantity());
		capeItemId = cape.getId();
	}

	private void trackEquipmentSlotUsage(int oldItemId, int oldQuantity, Item newItem, IntPredicate ammoFilter)
	{
		if (oldItemId <= 0 || oldQuantity <= 0 || !ammoFilter.test(oldItemId))
		{
			return;
		}

		int used = 0;
		if (newItem.getId() == oldItemId && newItem.getQuantity() < oldQuantity)
		{
			used = oldQuantity - Math.max(0, newItem.getQuantity());
		}
		else if (newItem.getId() <= 0 && oldQuantity <= 2 && client.getLocalPlayer() != null
			&& client.getLocalPlayer().getInteracting() != null)
		{
			used = oldQuantity;
		}

		if (used > 0 && used <= 2)
		{
			record(oldItemId, used, PvmToolsPlugin.SupplyCostType.AMMO);
		}
	}

	private void trackQuiverAmmo()
	{
		int newItemId = client.getVarpValue(VarPlayerID.DIZANAS_QUIVER_TEMP_AMMO);
		int newQuantity = client.getVarpValue(VarPlayerID.DIZANAS_QUIVER_TEMP_AMMO_AMOUNT);
		int used = 0;
		if (newItemId == quiverAmmoItemId && newQuantity < quiverAmmoQuantity)
		{
			used = quiverAmmoQuantity - Math.max(0, newQuantity);
		}
		else if (newItemId <= 0 && quiverAmmoItemId > 0 && quiverAmmoQuantity <= 2
			&& client.getLocalPlayer() != null && client.getLocalPlayer().getInteracting() != null)
		{
			used = quiverAmmoQuantity;
		}

		if (used > 0 && used <= 2 && isRangedAmmo(quiverAmmoItemId))
		{
			record(quiverAmmoItemId, used, PvmToolsPlugin.SupplyCostType.AMMO);
		}
		quiverAmmoItemId = newItemId;
		quiverAmmoQuantity = Math.max(0, newQuantity);
	}

	private void trackWeaponChargeUsage(int tick)
	{
		Player localPlayer = client.getLocalPlayer();
		if (localPlayer == null)
		{
			return;
		}

		int animation = localPlayer.getAnimation();
		if (isBlowpipe(weaponItemId) && isBlowpipeAnimation(animation)
			&& localPlayer.getAnimationFrame() == 0 && lastBlowpipeShotTick != tick)
		{
			lastBlowpipeShotTick = tick;
			recordBlowpipeShot();
			return;
		}

		if (localPlayer.getAnimationFrame() != 0 || lastPoweredStaffShotTick == tick)
		{
			return;
		}

		if (animation == POWERED_STAFF_ATTACK_ANIMATION && recordPoweredStaffRunes())
		{
			lastPoweredStaffShotTick = tick;
		}
		else if (animation == SHADOW_ATTACK_ANIMATION && isItem(weaponItemId, ItemID.TUMEKENS_SHADOW))
		{
			record(ItemID.CHAOS_RUNE, 2, PvmToolsPlugin.SupplyCostType.RUNE);
			record(ItemID.SOUL_RUNE, 5, PvmToolsPlugin.SupplyCostType.RUNE);
			lastPoweredStaffShotTick = tick;
		}
	}

	private boolean recordPoweredStaffRunes()
	{
		if (isAnyItem(weaponItemId, ItemID.TRIDENT_OF_THE_SWAMP, ItemID.TRIDENT_OF_THE_SWAMP_E,
			ItemID.TRIDENT_OF_THE_SWAMP_O, ItemID.TRIDENT_OF_THE_SWAMP_E_O))
		{
			recordTridentRunes();
			record(ItemID.ZULRAHS_SCALES, 1, PvmToolsPlugin.SupplyCostType.ZULRAH_SCALE);
			return true;
		}
		if (isAnyItem(weaponItemId, ItemID.TRIDENT_OF_THE_SEAS_FULL, ItemID.TRIDENT_OF_THE_SEAS,
			ItemID.TRIDENT_OF_THE_SEAS_E, ItemID.TRIDENT_OF_THE_SEAS_O,
			ItemID.TRIDENT_OF_THE_SEAS_FULL_O, ItemID.TRIDENT_OF_THE_SEAS_E_O))
		{
			recordTridentRunes();
			return true;
		}
		if (isItem(weaponItemId, ItemID.WARPED_SCEPTRE))
		{
			record(ItemID.CHAOS_RUNE, 2, PvmToolsPlugin.SupplyCostType.RUNE);
			record(ItemID.EARTH_RUNE, 5, PvmToolsPlugin.SupplyCostType.RUNE);
			return true;
		}
		if (isAnyItem(weaponItemId, ItemID.SANGUINESTI_STAFF, ItemID.HOLY_SANGUINESTI_STAFF))
		{
			record(ItemID.BLOOD_RUNE, 3, PvmToolsPlugin.SupplyCostType.RUNE);
			return true;
		}
		return false;
	}

	private void recordTridentRunes()
	{
		record(ItemID.CHAOS_RUNE, 1, PvmToolsPlugin.SupplyCostType.RUNE);
		record(ItemID.DEATH_RUNE, 1, PvmToolsPlugin.SupplyCostType.RUNE);
		record(ItemID.FIRE_RUNE, 5, PvmToolsPlugin.SupplyCostType.RUNE);
	}

	private void recordBlowpipeShot()
	{
		blowpipeScaleUsageRemainder += 2;
		int scalesUsed = blowpipeScaleUsageRemainder / 3;
		blowpipeScaleUsageRemainder %= 3;
		if (scalesUsed > 0)
		{
			record(ItemID.ZULRAHS_SCALES, scalesUsed, PvmToolsPlugin.SupplyCostType.ZULRAH_SCALE);
		}

		if (blowpipeDart == null)
		{
			return;
		}
		blowpipeDartUsageRemainder += getAmmoLossRate();
		int dartsUsed = (int) blowpipeDartUsageRemainder;
		blowpipeDartUsageRemainder -= dartsUsed;
		if (dartsUsed > 0)
		{
			record(blowpipeDart.itemId, dartsUsed, PvmToolsPlugin.SupplyCostType.AMMO);
		}
	}

	private double getAmmoLossRate()
	{
		if (capeItemId <= 0)
		{
			return 1d;
		}
		String capeName = itemManager.getItemComposition(capeItemId).getName().toLowerCase(Locale.ENGLISH);
		if (capeName.contains("assembler") || capeName.contains("dizana") || capeName.contains("ranging cape"))
		{
			return 0.20d;
		}
		if (capeName.contains("accumulator") || capeName.contains("max cape"))
		{
			return 0.28d;
		}
		if (capeName.contains("attractor"))
		{
			return 0.40d;
		}
		return 1d;
	}

	private boolean isThrownAmmo(int itemId)
	{
		String name = getItemName(itemId);
		return name.contains(" dart") || name.endsWith("dart")
			|| name.contains(" knife") || name.endsWith("knife")
			|| name.contains("thrownaxe") || name.contains("chinchompa");
	}

	private boolean isRangedAmmo(int itemId)
	{
		if (itemId <= 0)
		{
			return false;
		}
		String name = getItemName(itemId);
		return isThrownAmmo(itemId)
			|| name.contains(" arrow") || name.endsWith("arrow")
			|| name.contains(" bolt") || name.endsWith("bolt")
			|| name.contains(" javelin") || name.endsWith("javelin");
	}

	private String getItemName(int itemId)
	{
		return itemManager.getItemComposition(itemId).getName().toLowerCase(Locale.ENGLISH);
	}

	private Item getEquipmentItem(ItemContainer equipment, int slot)
	{
		if (equipment == null || slot < 0 || slot >= equipment.getItems().length)
		{
			return new Item(-1, 0);
		}
		Item item = equipment.getItem(slot);
		return item == null ? new Item(-1, 0) : item;
	}

	private boolean isBlowpipe(int itemId)
	{
		return itemId == ItemID.TOXIC_BLOWPIPE || itemId == ItemID.BLAZING_BLOWPIPE;
	}

	private boolean isBlowpipeAnimation(int animation)
	{
		return animation == BLOWPIPE_ATTACK_ANIMATION || animation == BLOWPIPE_ORNAMENT_ATTACK_ANIMATION;
	}

	private boolean isItem(int itemId, int expected)
	{
		return itemId == expected;
	}

	private boolean isAnyItem(int itemId, int... expectedItems)
	{
		for (int expected : expectedItems)
		{
			if (itemId == expected)
			{
				return true;
			}
		}
		return false;
	}

	private void registerBlowpipeDart(int itemId, int projectileId)
	{
		blowpipeDartsByProjectile.put(projectileId, new BlowpipeDart(itemId));
	}

	private void record(int itemId, int quantity, PvmToolsPlugin.SupplyCostType type)
	{
		if (itemId > 0 && quantity > 0)
		{
			usageConsumer.record(itemId, quantity, type);
		}
	}

	private static Map<Integer, Integer> buildRunePouchItems()
	{
		Map<Integer, Integer> runes = new HashMap<>();
		runes.put(1, ItemID.AIR_RUNE);
		runes.put(2, ItemID.WATER_RUNE);
		runes.put(3, ItemID.EARTH_RUNE);
		runes.put(4, ItemID.FIRE_RUNE);
		runes.put(5, ItemID.MIND_RUNE);
		runes.put(6, ItemID.CHAOS_RUNE);
		runes.put(7, ItemID.DEATH_RUNE);
		runes.put(8, ItemID.BLOOD_RUNE);
		runes.put(9, ItemID.COSMIC_RUNE);
		runes.put(10, ItemID.NATURE_RUNE);
		runes.put(11, ItemID.LAW_RUNE);
		runes.put(12, ItemID.BODY_RUNE);
		runes.put(13, ItemID.SOUL_RUNE);
		runes.put(14, ItemID.ASTRAL_RUNE);
		runes.put(15, ItemID.MIST_RUNE);
		runes.put(16, ItemID.MUD_RUNE);
		runes.put(17, ItemID.DUST_RUNE);
		runes.put(18, ItemID.LAVA_RUNE);
		runes.put(19, ItemID.STEAM_RUNE);
		runes.put(20, ItemID.SMOKE_RUNE);
		runes.put(21, ItemID.WRATH_RUNE);
		runes.put(22, ItemID.SUNFIRE_RUNE);
		runes.put(23, ItemID.AETHER_RUNE);
		return runes;
	}

	@FunctionalInterface
	interface SupplyUsageConsumer
	{
		void record(int itemId, int quantity, PvmToolsPlugin.SupplyCostType type);
	}

	private static final class BlowpipeDart
	{
		private final int itemId;

		private BlowpipeDart(int itemId)
		{
			this.itemId = itemId;
		}
	}
}
