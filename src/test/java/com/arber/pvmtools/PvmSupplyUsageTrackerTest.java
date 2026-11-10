package com.arber.pvmtools;

import java.util.ArrayList;
import java.util.List;
import net.runelite.api.ActorSpotAnim;
import net.runelite.api.Client;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.InventoryID;
import net.runelite.api.IterableHashTable;
import net.runelite.api.MenuAction;
import net.runelite.api.ItemID;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.StatChanged;
import net.runelite.api.gameval.AnimationID;
import net.runelite.api.gameval.SpotanimID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.game.ItemManager;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PvmSupplyUsageTrackerTest
{
	@Test
	public void sangCountsConsecutiveLocalCastsButNotOtherPlayersOrRepeatedGraphics()
	{
		Client client = mock(Client.class);
		Player player = mock(Player.class);
		Player otherPlayer = mock(Player.class);
		when(client.getLocalPlayer()).thenReturn(player);
		when(client.getGameCycle()).thenReturn(100);
		when(player.getAnimationFrame()).thenReturn(2);
		List<String> records = new ArrayList<>();
		PvmSupplyUsageTracker tracker = new PvmSupplyUsageTracker(client, mock(ItemManager.class),
			(itemId, quantity, type) -> records.add(itemId + ":" + quantity + ":" + type));
		tracker.initialize();

		ActorSpotAnim oldCast = spot(SpotanimID.SANGUINESTI_STAFF_CASTING, 99);
		graphics(player, oldCast);
		tracker.onGraphicChanged(player);
		ActorSpotAnim cast = spot(SpotanimID.SANGUINESTI_STAFF_CASTING, 101);
		graphics(otherPlayer, cast);
		tracker.onGraphicChanged(otherPlayer);
		assertEquals(List.of(), records);

		graphics(player, cast);
		tracker.onGraphicChanged(player);
		graphics(player, cast, spot(SpotanimID.SANGUINESTI_STAFF_HEAL, 102));
		tracker.onGraphicChanged(player);
		tracker.onGameTick();
		graphics(player, spot(SpotanimID.SANGUINESTI_STAFF_CASTING, 221));
		tracker.onGraphicChanged(player);
		graphics(player, spot(SpotanimID.SANGUINESTI_STAFF_CASTING_JUSTICIAR, 341));
		tracker.onGraphicChanged(player);
		assertEquals(List.of(ItemID.BLOOD_RUNE + ":2:RUNE", ItemID.BLOOD_RUNE + ":2:RUNE",
			ItemID.BLOOD_RUNE + ":2:RUNE"), records);
	}

	@Test
	public void refillingSangDoesNotCountAsConsumptionEvenWithSameTickMagicXp()
	{
		Client client = mock(Client.class);
		ItemManager itemManager = mock(ItemManager.class);
		List<String> records = new ArrayList<>();
		PvmSupplyUsageTracker tracker = new PvmSupplyUsageTracker(client, itemManager,
			(itemId, quantity, type) -> records.add(itemId + ":" + quantity + ":" + type));
		when(client.getTickCount()).thenReturn(100);
		tracker.initialize();

		ItemContainer baseline = mock(ItemContainer.class);
		when(baseline.getItems()).thenReturn(new Item[]{new Item(ItemID.BLOOD_RUNE, 1_000)});
		tracker.onItemContainerChanged(new ItemContainerChanged(InventoryID.INVENTORY.getId(), baseline));

		MenuOptionClicked recharge = mock(MenuOptionClicked.class);
		when(recharge.getMenuAction()).thenReturn(MenuAction.WIDGET_TARGET_ON_WIDGET);
		when(recharge.getMenuOption()).thenReturn("Use");
		when(recharge.getItemId()).thenReturn(ItemID.HOLY_SANGUINESTI_STAFF_UNCHARGED);
		Widget bloodRunes = mock(Widget.class);
		when(bloodRunes.getItemId()).thenReturn(ItemID.BLOOD_RUNE);
		when(client.getSelectedWidget()).thenReturn(bloodRunes);
		tracker.onMenuOptionClicked(recharge);

		ItemContainer charged = mock(ItemContainer.class);
		when(charged.getItems()).thenReturn(new Item[]{new Item(ItemID.BLOOD_RUNE, 994)});
		tracker.onItemContainerChanged(new ItemContainerChanged(InventoryID.INVENTORY.getId(), charged));
		tracker.onStatChanged(new StatChanged(Skill.MAGIC, 10, 99, 99));
		tracker.onGameTick();

		assertEquals(List.of(), records);

		when(client.getTickCount()).thenReturn(104);
		when(charged.getItems()).thenReturn(new Item[]{new Item(ItemID.BLOOD_RUNE, 993)});
		tracker.onItemContainerChanged(new ItemContainerChanged(InventoryID.INVENTORY.getId(), charged));
		tracker.onStatChanged(new StatChanged(Skill.MAGIC, 20, 99, 99));
		tracker.onGameTick();
		assertEquals(List.of(ItemID.BLOOD_RUNE + ":1:RUNE"), records);
	}

	@Test
	public void shadowAndWarpedSceptreUseTheirOwnCastGraphicsAndRuneCosts()
	{
		Client client = mock(Client.class);
		Player player = mock(Player.class);
		when(client.getLocalPlayer()).thenReturn(player);
		List<String> records = new ArrayList<>();
		PvmSupplyUsageTracker tracker = new PvmSupplyUsageTracker(client, mock(ItemManager.class),
			(itemId, quantity, type) -> records.add(itemId + ":" + quantity + ":" + type));
		tracker.initialize();

		graphics(player, spot(SpotanimID.TUMEKENS_SHADOW_CASTING, 100));
		tracker.onGraphicChanged(player);
		graphics(player, spot(SpotanimID.VFX_WARPED_SCEPTRE_CAST, 220));
		tracker.onGraphicChanged(player);
		assertEquals(List.of(ItemID.CHAOS_RUNE + ":5:RUNE", ItemID.SOUL_RUNE + ":2:RUNE",
			ItemID.CHAOS_RUNE + ":2:RUNE", ItemID.EARTH_RUNE + ":5:RUNE"), records);
	}

	@Test
	public void movingSeasTridentCastIncludesItsCoinCost()
	{
		Client client = mock(Client.class);
		Player player = mock(Player.class);
		when(client.getLocalPlayer()).thenReturn(player);
		when(player.getAnimation()).thenReturn(AnimationID.HUMAN_CASTWAVE_STAFF_WALKMERGE);
		int slot = EquipmentInventorySlot.WEAPON.getSlotIdx();
		ItemContainer equipment = mock(ItemContainer.class);
		when(equipment.getItems()).thenReturn(new Item[slot + 1]);
		when(equipment.getItem(slot)).thenReturn(new Item(ItemID.TRIDENT_OF_THE_SEAS, 1));
		when(client.getItemContainer(InventoryID.EQUIPMENT)).thenReturn(equipment);
		List<String> records = new ArrayList<>();
		PvmSupplyUsageTracker tracker = new PvmSupplyUsageTracker(client, mock(ItemManager.class),
			(itemId, quantity, type) -> records.add(itemId + ":" + quantity + ":" + type));

		tracker.onGameTick();
		tracker.onGameTick();
		assertEquals(List.of(ItemID.CHAOS_RUNE + ":1:RUNE", ItemID.DEATH_RUNE + ":1:RUNE",
			ItemID.FIRE_RUNE + ":5:RUNE", ItemID.COINS_995 + ":10:RUNE"), records);
	}

	@Test
	public void ordinaryBloodRuneInventoryDecreaseDoesNotCountWithoutCastOrRecharge()
	{
		Client client = mock(Client.class);
		ItemManager itemManager = mock(ItemManager.class);
		List<String> records = new ArrayList<>();
		PvmSupplyUsageTracker tracker = new PvmSupplyUsageTracker(client, itemManager,
			(itemId, quantity, type) -> records.add(itemId + ":" + quantity + ":" + type));
		when(client.getTickCount()).thenReturn(100);
		tracker.initialize();

		ItemContainer baseline = mock(ItemContainer.class);
		when(baseline.getItems()).thenReturn(new Item[]{new Item(ItemID.BLOOD_RUNE, 100)});
		tracker.onItemContainerChanged(new ItemContainerChanged(InventoryID.INVENTORY.getId(), baseline));
		ItemContainer changed = mock(ItemContainer.class);
		when(changed.getItems()).thenReturn(new Item[]{new Item(ItemID.BLOOD_RUNE, 97)});
		tracker.onItemContainerChanged(new ItemContainerChanged(InventoryID.INVENTORY.getId(), changed));
		tracker.onGameTick();

		assertFalse(records.iterator().hasNext());
	}

	private static ActorSpotAnim spot(int id, int startCycle)
	{
		ActorSpotAnim graphic = mock(ActorSpotAnim.class);
		when(graphic.getId()).thenReturn(id);
		when(graphic.getStartCycle()).thenReturn(startCycle);
		return graphic;
	}

	private static void graphics(Player player, ActorSpotAnim... active)
	{
		@SuppressWarnings("unchecked")
		IterableHashTable<ActorSpotAnim> table = mock(IterableHashTable.class);
		when(table.iterator()).thenAnswer(invocation -> List.of(active).iterator());
		when(player.getSpotAnims()).thenReturn(table);
	}
}
