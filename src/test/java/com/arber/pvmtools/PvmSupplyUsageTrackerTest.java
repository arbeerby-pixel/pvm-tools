package com.arber.pvmtools;

import java.util.ArrayList;
import java.util.List;
import net.runelite.api.Client;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.InventoryID;
import net.runelite.api.MenuAction;
import net.runelite.api.ItemID;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.client.game.ItemManager;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PvmSupplyUsageTrackerTest
{
	@Test
	public void identifiesSanguinestiStaffRechargeUse()
	{
		MenuOptionClicked event = mock(MenuOptionClicked.class);
		when(event.getMenuAction()).thenReturn(MenuAction.ITEM_USE_ON_ITEM);
		when(event.getItemId()).thenReturn(ItemID.BLOOD_RUNE);
		when(event.getId()).thenReturn(ItemID.SANGUINESTI_STAFF);

		assertTrue(PvmSupplyUsageTracker.isSanguinestiRechargeAction(event));
		assertTrue(PvmSupplyUsageTracker.isSanguinestiStaffItem(ItemID.HOLY_SANGUINESTI_STAFF));
	}

	@Test
	public void bloodRunesUsedToRechargeStaffAreRecordedWithoutMagicXp()
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
		when(recharge.getMenuAction()).thenReturn(MenuAction.ITEM_USE_ON_ITEM);
		when(recharge.getMenuOption()).thenReturn("Use");
		when(recharge.getItemId()).thenReturn(ItemID.BLOOD_RUNE);
		when(recharge.getId()).thenReturn(ItemID.SANGUINESTI_STAFF);
		tracker.onMenuOptionClicked(recharge);

		ItemContainer charged = mock(ItemContainer.class);
		when(charged.getItems()).thenReturn(new Item[]{new Item(ItemID.BLOOD_RUNE, 900)});
		tracker.onItemContainerChanged(new ItemContainerChanged(InventoryID.INVENTORY.getId(), charged));
		tracker.onGameTick();

		assertEquals(List.of(ItemID.BLOOD_RUNE + ":100:RUNE"), records);
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
}
